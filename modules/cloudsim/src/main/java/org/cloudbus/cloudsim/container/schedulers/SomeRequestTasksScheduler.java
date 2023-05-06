package org.cloudbus.cloudsim.container.schedulers;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.container.core.ContainerCloudSimTags;
import org.cloudbus.cloudsim.container.core.MicroserviceCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.vmplus.util.Id;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class SomeRequestTasksScheduler extends SimEntity {

    public static final int MAX_USER_REQUESTS = 3;
    private int brokerId;
    Queue<MicroserviceCloudlet> tasksQueue;
    List<MicroserviceCloudlet> waitingList;
    List<MicroserviceCloudlet> finishedTasks;
    List<MicroserviceCloudlet> allTasks;

    public SomeRequestTasksScheduler(String name, int brokerId) {
        super(name);
        this.brokerId = brokerId;
        tasksQueue = new LinkedList<>();
        waitingList = new ArrayList<>();
        finishedTasks = new ArrayList<>();
        allTasks = new ArrayList<>();
    }

    @Override
    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");
        scheduleNextUserRequest();
    }

    @Override
    public void shutdownEntity() {
        Log.printConcatLine(getName(), " is shutting down...");
    }

    private void scheduleNextUserRequest() {
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != ContainerCloudSimTags.USER_REQUEST_SUBMIT;
        if (CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), getNextUserRequestDelay(), ContainerCloudSimTags.USER_REQUEST_SUBMIT);
        }

    }

    private double getNextUserRequestDelay() {
        return Math.random() * 10;
    }

    @Override
    public void processEvent(SimEvent ev) {

        if (ev.getTag() == ContainerCloudSimTags.USER_REQUEST_SUBMIT) {
            enqueueUserRequestTasksAndScheduleNext();
        } else if (ev.getTag() == ContainerCloudSimTags.TASK_RETURN) {
            processTaskComplete(ev);
        }
    }

    private void processTaskComplete(SimEvent ev) {
        MicroserviceCloudlet cloudlet = (MicroserviceCloudlet) ev.getData();
        finishedTasks.add(cloudlet);
        Log.printConcatLine(CloudSim.clock(), ": ", getName(), " finished cloudlets=", String.join(",", finishedTasks.stream().map(c -> String.valueOf(c.getCloudletId())).collect(Collectors.toList())));

        //move from waiting to tasksQueue

        tasksQueue.addAll(getTasksReady());

    }

    private List<MicroserviceCloudlet> getTasksReady() {
        List ready = waitingList.stream().filter(c -> finishedTasks.contains(c.getProducerMs())).collect(Collectors.toList());
        waitingList.removeAll(ready);
        return ready;
    }


    private boolean any(MicroserviceCloudlet cloudlet) {
        return cloudlet.getProducerMs() == null || finishedTasks.stream().anyMatch(finished -> finished.getCloudletId() == cloudlet.getProducerMs().getCloudletId());
    }

    private void enqueueUserRequestTasksAndScheduleNext() {
        if (allTasks.size() < MAX_USER_REQUESTS * 3) {
            List<MicroserviceCloudlet> cloudlets = getUserRequestCloudlets();

//            final Predicate<MicroserviceCloudlet> producerMsProcessed = cloudlet -> finishedTasks.contains(cloudlet.getProducerMs());
//
//            cloudlets.stream().filter(cloudlet -> cloudlet.getProducerMs() == null || finishedTasks.stream().anyMatch(finished -> finished.getCloudletId() == cloudlet.getProducerMs().getCloudletId()));


            List<List<MicroserviceCloudlet>> groupedTasks = cloudlets.stream().collect(Collectors.teeing(
                    Collectors.filtering(cloudlet -> any(cloudlet), Collectors.toList()),
                    Collectors.filtering(cloudlet -> !any(cloudlet), Collectors.toList()),
                    List::of));
            tasksQueue.addAll(groupedTasks.get(0));
            waitingList.addAll(groupedTasks.get(1));
            allTasks.addAll(cloudlets);
        }

        while (!tasksQueue.isEmpty()) {
            MicroserviceCloudlet cloudlet = tasksQueue.poll();
            sendNow(brokerId, ContainerCloudSimTags.TASK_SUBMIT, cloudlet);
        }

        scheduleNextUserRequest();
    }


    private List<MicroserviceCloudlet> getUserRequestCloudlets() { //represent all ms calls for a single user request
        List<MicroserviceCloudlet> userRequestCloudlets = new ArrayList<>();
        MicroserviceCloudlet cloudlet = new MicroserviceCloudlet(Id.pollId(Cloudlet.class), 100000, 10, 0, 0, new UtilizationModelFull());
        MicroserviceCloudlet cloudlet2 = new MicroserviceCloudlet(Id.pollId(Cloudlet.class), 100000, 10, 0, 0, new UtilizationModelFull());
        MicroserviceCloudlet cloudlet3 = new MicroserviceCloudlet(Id.pollId(Cloudlet.class), 100000, 10, 0, 0, new UtilizationModelFull());
        cloudlet2.setProducerMs(cloudlet);
        cloudlet2.addConsumerMs(cloudlet3);
        cloudlet.addConsumerMs(cloudlet2);
        cloudlet3.setProducerMs(cloudlet2);
        userRequestCloudlets.add(cloudlet);
        userRequestCloudlets.add(cloudlet3);
        userRequestCloudlets.add(cloudlet2);

        userRequestCloudlets.forEach(microserviceCloudlet -> microserviceCloudlet.setUserId(brokerId));
        return userRequestCloudlets;
    }

    public boolean hasMoreTasks() {
        return !tasksQueue.isEmpty() || (allTasks.size() < finishedTasks.size()) || isScheduleTasksEventQueued();
    }

    public boolean allTasksProcessed() {
        return allTasks.size() == finishedTasks.size();
    }

    boolean isScheduleTasksEventQueued() {
        final Predicate<SimEvent> scheduleTasksEventsPredicate = evt -> evt.getTag() == ContainerCloudSimTags.USER_REQUEST_SUBMIT;
        return CloudSim.isFutureEventQueued(scheduleTasksEventsPredicate);
    }

    public void printCloudletList() {

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (MicroserviceCloudlet cloudlet : finishedTasks) {
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatusString() == "Success") {
                Log.print("SUCCESS");
                Log.printLine(indent + indent + cloudlet.getResourceId()
                        + indent + indent
                        + indent + indent
                        + dft.format(cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime()));
            }
        }
    }
}