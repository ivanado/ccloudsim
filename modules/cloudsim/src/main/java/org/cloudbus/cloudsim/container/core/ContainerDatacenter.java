package org.cloudbus.cloudsim.container.core;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.InfoPacket;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.List;

public class ContainerDatacenter extends SimEntity {
    private final ContainerAllocationPolicy containerAllocationPolicy;
    private final double schedulingInterval;

    @Getter
    @Setter
    private ContainerDatacenterCharacteristics characteristics;

    private List<ContainerHost> allHosts;

    private List<Container> activeContainers;
    private double lastProcessTime = 0;

    public ContainerDatacenter(String name, ContainerDatacenterCharacteristics characteristics, ContainerAllocationPolicy containerAllocationPolicy, double schedulingInterval) {
        super(name);
        this.characteristics = characteristics;
        this.containerAllocationPolicy = containerAllocationPolicy;
        this.schedulingInterval = schedulingInterval;
        this.allHosts = new ArrayList<>();
        this.activeContainers = new ArrayList<>();


        this.allHosts.addAll(this.characteristics.getHostList());
    }

    @Override
    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");

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

            // New Cloudlet arrives
            case CloudSimTags.CLOUDLET_SUBMIT -> processCloudletSubmit(ev, false);
//

            // New Cloudlet arrives, but the sender asks for an ack
            case CloudSimTags.CLOUDLET_SUBMIT_ACK -> processCloudletSubmit(ev, true);
//            case ContainerCloudSimTags.HOST_DATACENTER_EVENT -> {
//                updateCloudletProcessing();
//                checkCloudletCompletion();
//            }
//
//            // Cancels a previously submitted Cloudlet
//            case CloudSimTags.CLOUDLET_CANCEL -> processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
//
//
//            // Pauses a previously submitted Cloudlet
//            case CloudSimTags.CLOUDLET_PAUSE -> processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
//
//
//            // Pauses a previously submitted Cloudlet, but the sender
//            // asks for an acknowledgement
//            case CloudSimTags.CLOUDLET_PAUSE_ACK -> processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
//
//
//            // Resumes a previously submitted Cloudlet
//            case CloudSimTags.CLOUDLET_RESUME -> processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
//
//
//            // Resumes a previously submitted Cloudlet, but the sender
//            // asks for an acknowledgement
//            case CloudSimTags.CLOUDLET_RESUME_ACK -> processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
//
//
//            // Moves a previously submitted Cloudlet to a different resource
//            case CloudSimTags.CLOUDLET_MOVE -> processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
//
//
//            // Moves a previously submitted Cloudlet to a different resource
//            case CloudSimTags.CLOUDLET_MOVE_ACK -> processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
//
//
//            // Checks the status of a Cloudlet
//            case CloudSimTags.CLOUDLET_STATUS -> processCloudletStatus(ev);


            case CloudSimTags.INFOPKT_SUBMIT -> processPingRequest(ev);

            case ContainerCloudSimTags.CONTAINER_SUBMIT -> processContainerSubmit(ev, true);
//            case ContainerCloudSimTags.CONTAINER_MIGRATE -> processContainerMigrate(ev, false);
//            case FaultInjectionCloudSimTags.CONTAINER_FAIL -> processContainerFail(ev, false);
            case ContainerCloudSimTags.CONTAINER_DESTROY -> processContainerDestroy(ev);
            case ContainerCloudSimTags.CONTAINER_DC_LOG -> printResourcesStatus();
            default -> processOtherEvent(ev);
        }
    }

    private void processContainerDestroy(SimEvent ev) {
        int containerId = (int) ev.getData();
        Container container = activeContainers.stream().filter(c -> c.getId() == containerId).findFirst().orElseThrow();
        containerAllocationPolicy.deallocateContainerFromHost(container);

        activeContainers.remove(container);

    }

    private void processContainerSubmit(SimEvent ev, boolean ack) {

        List<Container> containerList = (List<Container>) ev.getData();

        for (Container container : containerList) {
            boolean result = containerAllocationPolicy.allocateHostForContainer(container, this.allHosts);
            if (ack) {
                int[] data = new int[3];
                data[1] = container.getId();
                if (result) {
                    data[2] = CloudSimTags.TRUE;
                } else {
                    data[2] = CloudSimTags.FALSE;
                }
                if (result) {
                    ContainerHost containerHost = containerAllocationPolicy.getContainerHost(container);
                    data[0] = containerHost.getId();
                    if (containerHost.getId() == -1) {

                        Log.printConcatLine("The ContainerHOST ID is not known (-1) !");
                    }
                    activeContainers.add(container);
                    if (container.isBeingInstantiated()) {
                        container.setBeingInstantiated(false);
                    }
                    container.updateContainerProcessing(CloudSim.clock(), containerAllocationPolicy.getContainerHost(container).getContainerScheduler().getAllocatedMipsForContainer(container));
                } else {
                    data[0] = -1;
                    //notAssigned.add(container);
                    Log.printLine(String.format("Couldn't find a host for the container #%s", container.getUid()));

                }
                send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), ContainerCloudSimTags.CONTAINER_CREATE_ACK, data);

            }
        }

    }

    private void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ".processOtherEvent(): Error - an event is null.");
        }
    }

    @Override
    public void shutdownEntity() {
        Log.printConcatLine(CloudSim.clock(), ":", getName(), " is shutting down...");
    }

    /**
     * Processes a ping request.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processPingRequest(SimEvent ev) {
        InfoPacket pkt = (InfoPacket) ev.getData();
        pkt.setTag(CloudSimTags.INFOPKT_RETURN);
        pkt.setDestId(pkt.getSrcId());

        // sends back to the sender
        sendNow(pkt.getSrcId(), CloudSimTags.INFOPKT_RETURN, pkt);
    }

    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
        updateCloudletProcessing();

        try {
            ContainerCloudlet cl = (ContainerCloudlet) ev.getData();

            // checks whether this Cloudlet has finished or not
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printConcatLine(getName(), ": Warning - Cloudlet #", cl.getCloudletId(), " owned by ", name, " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();

                // NOTE: If a Cloudlet has finished, then it won't be processed.
                // So, if ack is required, this method sends back a result.
                // If ack is not required, this method don't send back a result.
                // Hence, this might cause CloudSim to be hanged since waiting
                // for this Cloudlet back.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = cl.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                    sendNow(cl.getUserId(), tag, data);
                }

                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

                return;
            }

            // process this Cloudlet to this CloudResource
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

            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cl.getCloudletId();
                data[2] = CloudSimTags.TRUE;

                // unique tag = operation tag
                int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                sendNow(cl.getUserId(), tag, data);
            }
        } catch (ClassCastException c) {
            Log.printLine(String.format("%s.processCloudletSubmit(): ClassCastException error.", getName()));
            c.printStackTrace();
        } catch (Exception e) {
            Log.printLine(String.format("%s.processCloudletSubmit(): Exception error.", getName()));
            e.printStackTrace();
        }

        checkCloudletCompletion();
    }

    protected void updateCloudletProcessing() {
        // if some time passed since last processing
        // R: for term is to allow loop at simulation start. Otherwise, one initial
        // simulation step is skipped and schedulers are not properly initialized
        if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + CloudSim.getMinTimeBetweenEvents()) {
            double smallerTime = Double.MAX_VALUE;
            for (ContainerHost host : allHosts) {
                // inform containers to update processing
                double time = host.updateContainerProcessing(CloudSim.clock());
                // what time do we expect that the next cloudlet will finish?
                if (time > 0.0 && time < smallerTime) {
                    smallerTime = time;
                }
            }
            // guarantees a minimal interval before scheduling the event
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
        Log.printLine("========== DATCENTER " + getName() + " ==========");
        getHostList().forEach(containerHost -> {
            List<String> containers = containerHost.getContainerList().stream().map(container -> String.valueOf(container.getId())).toList();

            String msg = "Host #" + containerHost.getId() + "\t AllPes=" + containerHost.getNumberOfPes() + " FreePes=" + containerHost.getNumberOfFreePes() + " Containers=" + containers;
            Log.printLine(msg);
        });

        Log.printLine("========== ============== ==========");
    }

    public boolean hasRunningHosts() {
        return this.allHosts.stream().anyMatch(h->!h.isFailed());
    }

    public List<ContainerHost> getRunningHosts(){
        return this.allHosts.stream().filter(h->!h.isFailed()).toList();
    }
}
