package org.cloudbus.cloudsim.container.core;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.InfoPacket;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.Microservice;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContainerDatacenter extends SimEntity {
    private final ContainerAllocationPolicy containerAllocationPolicy;
    private final double schedulingInterval;

    @Getter
    @Setter
    private ContainerDatacenterCharacteristics characteristics;

    private List<ContainerHost> allHosts;

    private List<Container> activeContainers;
    private double lastProcessTime = 0;

    List<ContainerCloudlet> runningCloudlets;
    private List<Container> failedContainers;

    public ContainerDatacenter(String name, ContainerDatacenterCharacteristics characteristics, ContainerAllocationPolicy containerAllocationPolicy, double schedulingInterval) {
        super(name);
        this.characteristics = characteristics;
        this.containerAllocationPolicy = containerAllocationPolicy;
        this.schedulingInterval = schedulingInterval;
        this.allHosts = new ArrayList<>();
        this.activeContainers = new ArrayList<>();
        this.runningCloudlets = new ArrayList<>();
        this.failedContainers = new ArrayList<>();


        this.allHosts.addAll(this.characteristics.getHostList());
    }

    @Override
    public void startEntity() {
        Log.printLine(getName(), " is starting...");

        int gisID = CloudSim.getCloudInfoServiceEntityId();

        sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());
    }

    @Override
    public void processEvent(SimEvent ev) {
        int srcId = -1;
        switch (ev.getTag()) {
            // Resource characteristics inquiry
            case CloudSimTags.RESOURCE_CHARACTERISTICS -> {
                srcId = (Integer) ev.getData();
                sendNow(srcId, ev.getTag(), getCharacteristics());
            }

            // Resource dynamic info inquiry
            case CloudSimTags.RESOURCE_DYNAMICS -> {
                srcId = (Integer) ev.getData();
                sendNow(srcId, ev.getTag(), 0);
            }
            case CloudSimTags.RESOURCE_NUM_PE -> {
                srcId = (Integer) ev.getData();
                int numPE = getCharacteristics().getNumberOfPes();
                sendNow(srcId, ev.getTag(), numPE);
            }
            case CloudSimTags.RESOURCE_NUM_FREE_PE -> {
                srcId = (Integer) ev.getData();
                int freePesNumber = getCharacteristics().getNumberOfFreePes();
                sendNow(srcId, ev.getTag(), freePesNumber);
            }
            case CloudSimTags.INFOPKT_SUBMIT -> processPingRequest(ev);
            case ContainerCloudSimTags.CONTAINER_DC_LOG -> printResourcesStatus();

            case ContainerCloudSimTags.CONTAINER_SUBMIT -> processContainerSubmit(ev, true);
            case ContainerCloudSimTags.CONTAINER_DESTROY -> processContainerDestroy(ev);
            case CloudSimTags.CLOUDLET_SUBMIT -> processCloudletSubmit(ev);
            case ContainerCloudSimTags.CONTAINER_DC_EVENT -> updateAndCheckProcessing();
            case CloudSimTags.CLOUDLET_CANCEL -> cancelCloudletRunningOnContainer(ev);
            default -> processOtherEvent(ev);
        }
    }

    private void cancelCloudletRunningOnContainer(SimEvent ev) {
        Container container = (Container) ev.getData();
        ContainerCloudlet cloudletToFail = runningCloudlets.stream().filter(cl -> cl.getContainerId() == container.getId()).findAny().orElse(null);
        try {
            cloudletToFail.setCloudletStatus(Cloudlet.FAILED);
            failedContainers.add(container);
//            container.getContainerCloudletScheduler().cloudletCancel(cloudletToFail.getCloudletId());
//            cloudletToFail.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sendNow(getId(), ContainerCloudSimTags.CONTAINER_DESTROY, container);
    }


    private void updateAndCheckProcessing() {
        updateCloudletProcessing();
        checkCloudletCompletion();
    }


    private void processContainerDestroy(SimEvent ev) {
        Container container;
        if (ev.getData() instanceof Task) {
            Task task = (Task) ev.getData();
            container = task.getContainer();
        } else {
            container = (Container) ev.getData();
        }
        containerAllocationPolicy.deallocateContainerFromHost(container);

        activeContainers.remove(container);
        printResourcesStatus();
    }

    private void processContainerSubmit(SimEvent ev, boolean ack) {

        Task task = (Task) ev.getData();

//calculate values

        boolean result = containerAllocationPolicy.allocateHostForContainer(task.getContainer(), this.allHosts);
        if (ack) {
            int[] data = new int[3];
            data[1] = task.getContainer().getId();

            data[2] = result ? CloudSimTags.TRUE : CloudSimTags.FALSE;

            if (result) {
                ContainerHost containerHost = containerAllocationPolicy.getContainerHost(task.getContainer());
                data[0] = containerHost.getId();
                if (containerHost.getId() == -1) {

                    Log.printLine(getName(), ": The ContainerHOST ID is not known (-1) !");
                }
                activeContainers.add(task.getContainer());
                if (task.getContainer().isBeingInstantiated()) {
                    task.getContainer().setBeingInstantiated(false);
                }
                task.getContainer().updateContainerProcessing(CloudSim.clock(), containerAllocationPolicy.getContainerHost(task.getContainer()).getContainerScheduler().getAllocatedMipsForContainer(task.getContainer()));
            } else {
                data[0] = -1;
                Log.printLine(String.format("Couldn't find a host for the container #%s", task.getContainer().getUid()));

            }
            send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), ContainerCloudSimTags.CONTAINER_CREATE_ACK, data);
        }
    }

    private void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine(getName(), ".processOtherEvent(): Error - an event is null.");
        }
    }

    @Override
    public void shutdownEntity() {
        Log.printLine(getName(), " is shutting down...");
    }

    protected void processPingRequest(SimEvent ev) {
        InfoPacket pkt = (InfoPacket) ev.getData();
        pkt.setTag(CloudSimTags.INFOPKT_RETURN);
        pkt.setDestId(pkt.getSrcId());

        // sends back to the sender
        sendNow(pkt.getSrcId(), CloudSimTags.INFOPKT_RETURN, pkt);
    }

    protected void processCloudletSubmit(SimEvent ev) {
        updateCloudletProcessing();

        try {
            Task processCloudletTask = (Task) ev.getData();
            ContainerCloudlet cl = processCloudletTask.getCloudlet();
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printLine(getName(), ": Warning - Cloudlet #", cl.getCloudletId(), " owned by ", name, " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();
                runningCloudlets.remove(cl);
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, processCloudletTask);

                return;
            }
            runningCloudlets.add(cl);
            cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics().getCostPerBw());

            int userId = cl.getUserId();
            int containerId = cl.getContainerId();

            // time to transfer the files
            double fileTransferTime = 0;//predictFileTransferTime(cl.getRequiredFiles());

            ContainerHost host = containerAllocationPolicy.getContainerHost(containerId, userId);
            Container container = host.getContainer(containerId, userId);
            double estimatedFinishTime = container.getContainerCloudletScheduler().cloudletSubmit(cl, fileTransferTime);

            // if this cloudlet is in the exec queue
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                estimatedFinishTime += fileTransferTime;
                send(getId(), estimatedFinishTime, ContainerCloudSimTags.CONTAINER_DC_EVENT);
            }

        } catch (ClassCastException c) {
            Log.printLine(getName(), ".processCloudletSubmit(): ClassCastException error.");
            c.printStackTrace();
        } catch (Exception e) {
            Log.printLine(getName(), ".processCloudletSubmit(): Exception error.");
            e.printStackTrace();
        }

        checkCloudletCompletion();
    }

    protected void updateCloudletProcessing() {

        if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + CloudSim.getMinTimeBetweenEvents()) {
            double smallerTime = Double.MAX_VALUE;
            for (ContainerHost host : allHosts) {
                double time = host.updateContainerProcessing(CloudSim.clock());
                // what time do we expect that the next cloudlet will finish?
                if (time > 0.0 && time < smallerTime) {
                    smallerTime = time;
                }
            }
            if (smallerTime < CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01) {
                smallerTime = CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01;
            }
            if (smallerTime != Double.MAX_VALUE) {
                schedule(getId(), (smallerTime - CloudSim.clock()), ContainerCloudSimTags.CONTAINER_DC_EVENT);
            }
            setLastProcessTime(CloudSim.clock());
        }
    }


    private void setLastProcessTime(double clock) {
        this.lastProcessTime = clock;
    }

    public double getLastProcessTime() {
        return lastProcessTime;
    }

    protected void checkCloudletCompletion() {
        for (ContainerHost host : allHosts) {
            for (Container container : host.getContainerList()) {
                while (container.getContainerCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = container.getContainerCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {

                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }

        }
    }

    public List<ContainerHost> getHostList() {
        return getCharacteristics().getHostList();
    }

    public List<Container> getContainerList() {
        return activeContainers;
    }

    private void printResourcesStatus() {
        Log.printLine();
        Log.printLine("========== DATACENTER " + getName() + " ==========");
        getHostList().forEach(containerHost -> {
            List<String> containers = containerHost.getContainerList().stream().map(container -> String.valueOf(container.getId())).toList();

            String msg = "Host #" + containerHost.getId() + "\t AllPes=" + containerHost.getNumberOfPes() + " Host-FreePes=" + containerHost.getNumberOfFreePes() + " Scheduler-FreePes=" + containerAllocationPolicy.getFreePesForHost(containerHost.getId()) + " Containers=" + containers;
            Log.printLine(msg);
        });

        Log.printLine("========== ============== ==========\n");
    }

    public boolean hasRunningHosts() {
        return this.allHosts.stream().anyMatch(h -> !h.isFailed());
    }

    public List<ContainerHost> getRunningHosts() {
        return this.allHosts.stream().filter(h -> !h.isFailed()).toList();
    }

    public void removeRunningContainer(Container containerToFail) {
        getContainerList().remove(containerToFail);
    }

    public int getContainerReplicaCount(Microservice ms) {
        return (int) this.activeContainers.stream().filter(c -> {
            return c.getMicroserviceId() == ms.getId();
        }).count();
    }
    public int getContainerReplicaCount(int msId) {
        return (int) this.activeContainers.stream().filter(c -> {
            return c.getMicroserviceId() == msId;
        }).count();
    }
    public List<ContainerHost> getHostsWithFreePes(int requiredPes){
        return getRunningHosts().stream().filter(host->{return host.getNumberOfFreePes() >= requiredPes;}).collect(Collectors.toList());
    }

    public List<ContainerHost> getHostsRunningMicroservice(int msId) {
        return getRunningHosts().stream().filter(host->{return host.hasContainer(msId);}).collect(Collectors.toList());
    }

    public List<Container> getContainersRunningMicroservice(int msId) {
        return getContainerList().stream().filter(container -> {return container.getMicroserviceId() == msId;}).toList();
    }
}
