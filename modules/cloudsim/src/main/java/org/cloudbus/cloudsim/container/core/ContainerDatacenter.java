package org.cloudbus.cloudsim.container.core;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.InfoPacket;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.List;

public class ContainerDatacenter extends SimEntity {
    private final ContainerAllocationPolicy containerAllocationPolicy;

    @Getter
    @Setter
    private ContainerDatacenterCharacteristics characteristics;

    private double lastProcessTime = 0;

    private final DatacenterMetrics dcResources = DatacenterMetrics.get();

    public ContainerDatacenter(String name, ContainerDatacenterCharacteristics characteristics, ContainerAllocationPolicy containerAllocationPolicy) {
        super(name);
        this.characteristics = characteristics;
        this.containerAllocationPolicy = containerAllocationPolicy;


        dcResources.getRunningHosts().addAll(this.characteristics.getHostList());
    }

    @Override
    public void startEntity() {
        Log.printLine(getName(), " is starting...");

        int gisID = CloudSim.getCloudInfoServiceEntityId();

        sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());
    }

    @Override
    public void processEvent(SimEvent ev) {
        int srcId;
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

            case ContainerCloudSimTags.CONTAINER_SUBMIT -> processContainerSubmit(ev);
            case ContainerCloudSimTags.CONTAINER_DESTROY -> processContainerDestroy(ev);
            case ContainerCloudSimTags.CONTAINER_FAIL_DESTROY -> processContainerFailDestroy(ev);
            case CloudSimTags.CLOUDLET_SUBMIT -> processCloudletSubmit(ev);
            case ContainerCloudSimTags.CONTAINER_DC_EVENT -> updateAndCheckProcessing();
            case ContainerCloudSimTags.CLOUDLET_FAIL -> failCloudletRunningOnContainer(ev);
            default -> processOtherEvent(ev);
        }
    }

    private void processContainerFailDestroy(SimEvent ev) {
        Container container = (Container) ev.getData();

        containerAllocationPolicy.deallocateContainerFromHost(container);

        dcResources.failContainer(container);
        printResourcesStatus();

    }

    private void failCloudletRunningOnContainer(SimEvent ev) {
        Container container = (Container) ev.getData();
        ContainerCloudlet cloudletToFail = dcResources.getRunningCloudlets().stream().filter(cl -> cl.getContainerId() == container.getId()).findAny().orElse(null);
        if (cloudletToFail != null) {
            try {
                cloudletToFail.setCloudletStatus(Cloudlet.FAILED);
                dcResources.failCloudlet(cloudletToFail);
            } catch (Exception e) {
                throw new RuntimeException("setting cloudlet status failed");
            }
        }

        sendNow(getId(), ContainerCloudSimTags.CONTAINER_FAIL_DESTROY, container);
    }


    private void updateAndCheckProcessing() {
        updateCloudletProcessing();
        checkCloudletCompletion();
    }


    private void processContainerDestroy(SimEvent ev) {
        Container container = (Container) ev.getData();

        containerAllocationPolicy.deallocateContainerFromHost(container);

        dcResources.finishContainer(container);
        printResourcesStatus();
    }

    private void processContainerSubmit(SimEvent ev) {

        Task task = (Task) ev.getData();

        boolean result = containerAllocationPolicy.allocateHostForContainer(task.getContainer(), dcResources.getRunningHosts());

        int[] data = new int[3];
        data[1] = task.getContainer().getId();
        data[2] = result ? CloudSimTags.TRUE : CloudSimTags.FALSE;

        if (result) {
            ContainerHost containerHost = containerAllocationPolicy.getContainerHost(task.getContainer());
            data[0] = containerHost.getId();
            if (containerHost.getId() == -1) {
                Log.printLine(getName(), ": The ContainerHOST ID is not known (-1) !");
            }
            dcResources.startContainer(task.getContainer());
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
                dcResources.finishCloudlet(cl);
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

                return;
            }
            dcResources.startCloudlet(cl);
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
            for (ContainerHost host : dcResources.getRunningHosts()) {
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
        for (ContainerHost host : dcResources.getRunningHosts()) {
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

//    public List<ContainerHost> getHostList() {
//        return getCharacteristics().getHostList();
//    }

//    public List<Container> getContainerList() {
//        return activeContainers;
//    }

    private void printResourcesStatus() {
        Log.printLine();
        Log.printLine("========== DATACENTER " + getName() + " ==========");
        dcResources.getRunningHosts().forEach(containerHost -> {
            List<String> containers = containerHost.getContainerList().stream().map(container -> String.valueOf(container.getId())).toList();
            List<String> ms = containerHost.getContainerList().stream().map(c -> String.valueOf(c.getMicroserviceId())).toList();
            String msg = "Host #" + containerHost.getId() +
                    " AllPes=" + containerHost.getNumberOfPes() +
                    " Host-FreePes=" + containerHost.getNumberOfFreePes() +
                    " Containers=" + containers +
                    " Microservices= " +ms;
            Log.printLine(msg);
        });

        Log.printLine("========== ============== ==========\n");
    }

}
