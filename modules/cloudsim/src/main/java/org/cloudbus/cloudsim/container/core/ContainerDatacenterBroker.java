package org.cloudbus.cloudsim.container.core;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.schedulers.UserRequestTasksScheduler;
import org.cloudbus.cloudsim.container.schedulers.ContainerCloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.fault.injector.FaultInjectionCloudSimTags;
import org.cloudbus.cloudsim.vmplus.util.Id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerDatacenterBroker extends SimEntity {
    @Getter
    @Setter
    private List<Integer> datacenterIdsList;
    @Getter
    @Setter
    protected Map<Integer, ContainerDatacenterCharacteristics> datacenterCharacteristicsList;


    private Map<Integer, Integer> containerToHostMap;

    private List<Integer> createdContainers;
    private Map<Integer, Cloudlet> createdContainerToCloudletMap;

    private int totalContainersCreated = 0;
    private int totalProcessedCloudlets = 0;
    private int totalScheduledCloudlets = 0;

    private List<Integer> runningContainers;
    private Map<Integer, Cloudlet> scheduledCloudlets;
    private Map<Integer, Cloudlet> processedCloudlets;


    private UserRequestTasksScheduler taskScheduler;

    public ContainerDatacenterBroker(String name) {
        super(name);
        containerToHostMap = new HashMap<>();
        createdContainerToCloudletMap = new HashMap<>();
        processedCloudlets = new HashMap<>();
        scheduledCloudlets = new HashMap<>();
        createdContainers = new ArrayList<>();
        runningContainers = new ArrayList<>();
        taskScheduler = new UserRequestTasksScheduler("BM-TaskScheduler", getId());
    }

    @Override
    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");
        schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // Resource characteristics request
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST -> processResourceCharacteristicsRequest(ev);

            // Resource characteristics answer
            case CloudSimTags.RESOURCE_CHARACTERISTICS -> processResourceCharacteristics(ev);
            case ContainerCloudSimTags.SUBMIT_TASK -> processTaskSubmit(ev);

            // A finished cloudlet returned
            case CloudSimTags.CLOUDLET_RETURN -> processCloudletReturn(ev);

            // if the simulation finishes
            case CloudSimTags.END_OF_SIMULATION -> shutdownEntity();
            case ContainerCloudSimTags.CONTAINER_CREATE_ACK -> processContainerCreate(ev);
            case CloudSimTags.CLOUDLET_SUBMIT_ACK ->
                    processCloudletSubmit(ev);//just add the cloudlet on the running list

            default -> processOtherEvent(ev);
        }
    }

    private void processCloudletSubmit(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int cloudletId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            //cloudlet submitted container is processing
            Cloudlet cloudlet = scheduledCloudlets.get(cloudletId);
            runningContainers.add(createdContainerToCloudletMap.entrySet().stream().filter(e -> e.getValue().getCloudletId() == cloudletId).map(e -> e.getKey()).findFirst().orElseThrow());
        } else {
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Cloudlet #", cloudletId, " execution failed");
        }
    }

    /**
     * Allocates the container for the task and runs the task
     *
     * @param ev
     */
    private void processTaskSubmit(SimEvent ev) {
        MicroserviceCloudlet cloudlet = (MicroserviceCloudlet) ev.getData();
        Container container = new Container(Id.pollId(Container.class), getId(), 100, 15, 0, 0, 0, "xen", new ContainerCloudletSchedulerTimeShared(), 300);
        sendNow(datacenterIdsList.get(0), ContainerCloudSimTags.CONTAINER_SUBMIT, List.of(container));
        cloudlet.setContainerId(container.getId());
        createdContainerToCloudletMap.put(container.getId(), cloudlet);
        totalScheduledCloudlets++;
    }

    private void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printConcatLine(getName(), ".processOtherEvent(): ", "Error - an event is null.");
            return;
        }

        Log.printConcatLine(getName(), ".processOtherEvent(): Error - event unknown by this DatacenterBroker.");
    }

    private void processContainerCreate(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int hostId = data[0];
        int containerId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            if (hostId == -1) {
                Log.printConcatLine("Error : Where is the HOST");
            } else {
                containerToHostMap.put(containerId, hostId);
                createdContainers.add(containerId);
                Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": The Container #", containerId,
                        ", is created on host #", hostId);
                totalContainersCreated++;

                sendNow(datacenterIdsList.get(0), ContainerCloudSimTags.DATACENTER_PRINT);

//                ---->process the cloudlet on created container
                Cloudlet cloudletToProcess = createdContainerToCloudletMap.get(containerId);
                scheduledCloudlets.put(cloudletToProcess.getCloudletId(), cloudletToProcess);
                runningContainers.add(containerId);
                sendNow(datacenterIdsList.get(0), CloudSimTags.CLOUDLET_SUBMIT, cloudletToProcess);
            }
        } else {
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Failed Creation of Container #", containerId);
        }
    }

    private void processCloudletReturn(SimEvent ev) {
        ContainerCloudlet cloudlet = (ContainerCloudlet) ev.getData();
        processedCloudlets.put(cloudlet.getCloudletId(), cloudlet);
        totalProcessedCloudlets++;

        Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Cloudlet ", cloudlet.getCloudletId(),
                " returned. ", totalProcessedCloudlets, " finished Cloudlets = ", String.join(", ", processedCloudlets.keySet().stream().map(c -> c.toString()).collect(Collectors.toList())));
        //deallocate the container used for cloudlet processing
        sendNow(datacenterIdsList.get(0), FaultInjectionCloudSimTags.CONTAINER_DESTROY, cloudlet.getContainerId());
        sendNow(taskScheduler.getId(), ContainerCloudSimTags.TASK_COMPLETE, cloudlet);


        if (this.taskScheduler.allTasksProcessed()) {
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": All Cloudlets executed. Finishing...");
            clearDatacenters();
            finishExecution();
        } else { // some cloudlets haven't finished yet
            if (totalScheduledCloudlets > totalProcessedCloudlets) {
                // all the cloudlets sent finished. It means that some bount
                // cloudlet is waiting its container be created
                //should never happen since the containes are allocated for specific cloudlet
                clearDatacenters();
            }

        }
    }

    protected void finishExecution() {
        sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
    }


    protected void clearDatacenters() {
//        for (int contId : createdContainers) {
////            Log.printConcatLine(CloudSim.clock(), ": " + getName(), ": Destroying VM #", vm.getId());
//            sendNow(datacenterId, CONTAINER_DESTROY, contId);
//        }
//
//        createdContainers.clear();

    }


    private void processResourceCharacteristics(SimEvent ev) {
        ContainerDatacenterCharacteristics characteristics = (ContainerDatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            getDatacenterCharacteristicsList().clear();
//            setDatacenterRequestedIdsList(new ArrayList<>());
//            createVmsInDatacenter(getDatacenterIdsList().get(0));
        }
    }

    private void processResourceCharacteristicsRequest(SimEvent ev) {
        setDatacenterIdsList(CloudSim.getCloudResourceList());
        setDatacenterCharacteristicsList(new HashMap<>());

        //Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Cloud Resource List received with ",
//                getDatacenterIdsList().size(), " resource(s)");

        for (Integer datacenterId : getDatacenterIdsList()) {
            sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
        }
    }

    @Override
    public void shutdownEntity() {
        Log.printConcatLine(getName(), " is shutting down...");
    }

    public void printCloudletReport() {
        taskScheduler.printCloudletList();
    }
}
