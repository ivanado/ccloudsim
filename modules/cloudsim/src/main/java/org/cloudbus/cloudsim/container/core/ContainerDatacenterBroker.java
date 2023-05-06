package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.Task;
import org.cloudbus.cloudsim.container.schedulers.UserRequestScheduler;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerDatacenterBroker extends SimEntity {


    public Map<Integer, Integer> containerToHostMap;
    public Integer datacenterId;
    public ContainerDatacenterCharacteristics datacenterCharacteristics;

    public List<Task> submittedTasks;

    public UserRequestScheduler taskScheduler;

    public ContainerDatacenterBroker(String name) {
        super(name);
        this.containerToHostMap = new HashMap<>();
        this.taskScheduler = new UserRequestScheduler("BM-TaskScheduler", getId());
        this.submittedTasks = new ArrayList<>();
    }

    @Override
    public void startEntity() {
        Log.printLine(getName(), " is starting...");
        schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST -> processResourceCharacteristicsRequest(ev);
            case CloudSimTags.RESOURCE_CHARACTERISTICS -> processResourceCharacteristics(ev);
            case ContainerCloudSimTags.TASK_SUBMIT -> processTaskSubmit(ev);
            case ContainerCloudSimTags.CONTAINER_CREATE_ACK -> processContainerAllocated(ev);
            case CloudSimTags.CLOUDLET_RETURN -> processCloudletReturn(ev);
            default -> processOtherEvent(ev);
        }
    }

    private void processContainerAllocated(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int hostId = data[0];
        int containerId = data[1];
        if (data[2] == CloudSimTags.TRUE) {
            //container allocated
            if (hostId == -1) {
                Log.printLine("Error : Where is the HOST");
            } else {
                containerToHostMap.put(containerId, hostId);
                Log.printLine(getName(), ": The Container #", containerId,
                        ", is created on host #", hostId);

                sendNow(datacenterId, ContainerCloudSimTags.CONTAINER_DC_LOG);
//                ---->process the cloudlet on created container
                Task processCloudletTask = submittedTasks.stream().filter(t -> t.container.getId() == containerId).findFirst().orElse(null);
                if (processCloudletTask != null) {
                    processCloudletTask.cloudlet.setContainerId(containerId);
                    sendNow(datacenterId, CloudSimTags.CLOUDLET_SUBMIT, processCloudletTask);
                }

            }
        }
    }

    private void processTaskSubmit(SimEvent ev) {
        Task task = (Task) ev.getData();
        submittedTasks.add(task);
        //create container
        sendNow(datacenterId, ContainerCloudSimTags.CONTAINER_SUBMIT, task);
    }

    private void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine(getName(), ".processOtherEvent(): ", "Error - an event is null.");
            return;
        }
        Log.printLine(getName(), ".processOtherEvent(): Error - event unknown by this DatacenterBroker.");
    }


    private void processCloudletReturn(SimEvent ev) {
        Task task = null;
        if(ev.getData() instanceof Task){
             task = (Task) ev.getData();
        }else{
            ContainerCloudlet cloudlet=(ContainerCloudlet) ev.getData();
             task =   taskScheduler.getTaskForCloudlet(cloudlet);
        }


        Log.printLine(getName(), ": Cloudlet ", task.cloudlet.getCloudletId(),
                " returned. ", taskScheduler.getProcessedTasksCount(), " finished Cloudlets = ", String.join(", ", taskScheduler.finishedTasks.stream().map(t -> t.cloudlet.toString()).collect(Collectors.toList())));
        //deallocate the container used for cloudlet processing
        sendNow(datacenterId, ContainerCloudSimTags.CONTAINER_DESTROY, task);
        sendNow(taskScheduler.getId(), ContainerCloudSimTags.TASK_RETURN, task);


        if (this.taskScheduler.allTasksProcessed()) {
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": All Cloudlets executed. Finishing...");
            clearDatacenters();
            finishExecution();
        } else { // some cloudlets haven't finished yet
            if (taskScheduler.allTasks.size() > taskScheduler.finishedTasks.size()) {
                // all the cloudlets sent finished. It means that some bount
                // cloudlet is waiting its container to be created
                //should never happen since the contains are allocated for specific cloudlet
                clearDatacenters();
            }

        }
    }

    protected void finishExecution() {
        sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
    }


    protected void clearDatacenters() {
//TODO clear containers?hosts?

    }


    private void processResourceCharacteristics(SimEvent ev) {
        ContainerDatacenterCharacteristics characteristics = (ContainerDatacenterCharacteristics) ev.getData();
        this.datacenterCharacteristics = characteristics;
    }

    private void processResourceCharacteristicsRequest(SimEvent ev) {
        this.datacenterId = CloudSim.getCloudResourceList().get(0);
        sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
    }

    @Override
    public void shutdownEntity() {
        Log.printLine(getName(), " is shutting down...");
    }

    public void printCloudletReport() {
        taskScheduler.printTasksReport();
    }
}
