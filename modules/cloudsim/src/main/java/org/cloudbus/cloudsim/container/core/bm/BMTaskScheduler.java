package org.cloudbus.cloudsim.container.core.bm;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.vmplus.util.Id;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.cloudbus.cloudsim.container.core.bm.BMCloudSimTags.SCHEDULE_USER_REQUEST_TASKS;
import static org.cloudbus.cloudsim.container.core.bm.BMCloudSimTags.TASK_COMPLETE;

public class BMTaskScheduler extends SimEntity {

    public static final int MAX_USER_REQUESTS = 3;
    private int brokerId;
    Queue<DAGCloudlet> tasksQueue;
    List<DAGCloudlet> finishedTasks;
    List<DAGCloudlet> allTasks;
    Map<DAGCloudlet, DAGCloudlet> waitingList;

    public BMTaskScheduler(String name, int brokerId) {
        super(name);
        this.brokerId = brokerId;
        tasksQueue = new LinkedList<>();
        waitingList = new HashMap<>();
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
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != SCHEDULE_USER_REQUEST_TASKS;
        if (CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), getNextUserRequestDelay(), SCHEDULE_USER_REQUEST_TASKS);
        }

    }

    private double getNextUserRequestDelay() {
        return Math.random() * 1000;
    }

    @Override
    public void processEvent(SimEvent ev) {

        if (ev.getTag() == SCHEDULE_USER_REQUEST_TASKS) {
            enqueueUserRequestTasksAndScheduleNext();
        } else if (ev.getTag() == TASK_COMPLETE) {

            processTaskComplete(ev);
        }
    }

    private void processTaskComplete(SimEvent ev) {
        DAGCloudlet cloudlet = (DAGCloudlet) ev.getData();
        finishedTasks.add(cloudlet);
        Log.printConcatLine(CloudSim.clock(), ": ", getName(), " finished cloudlets=", String.join(",", finishedTasks.stream().map(c -> String.valueOf(c.getCloudletId())).collect(Collectors.toList())));
    }


    private void enqueueUserRequestTasksAndScheduleNext() {
        if (allTasks.size() < MAX_USER_REQUESTS * 3) {
            List<DAGCloudlet> cloudlets = getUserRequestCloudlets();
            tasksQueue.addAll(cloudlets);
            allTasks.addAll(cloudlets);
        }
        if (!tasksQueue.isEmpty()) {
            DAGCloudlet cloudlet = tasksQueue.poll();
            if (cloudlet.getParent() == null) {
                sendNow(brokerId, BMCloudSimTags.SUBMIT_TASK, cloudlet);

            } else {
                List<Integer> finishedCloudletIds = finishedTasks.stream().map(c -> c.getCloudletId()).collect(Collectors.toList());
                if (finishedCloudletIds.contains(cloudlet.getParent().getCloudletId())) {
                    sendNow(brokerId, BMCloudSimTags.SUBMIT_TASK, cloudlet);
                } else {
                    tasksQueue.add(cloudlet);
                }
            }

            scheduleNextUserRequest();
        }


    }

    private List<DAGCloudlet> getCloudlets() {
        List<DAGCloudlet> cloudlets = new ArrayList<>();

        DAGCloudlet cloudlet = new DAGCloudlet(Id.pollId(Cloudlet.class), 10000, 10, 0, 0, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        DAGCloudlet cloudlet2 = new DAGCloudlet(Id.pollId(Cloudlet.class), 10000, 10, 0, 0, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        cloudlet2.setParent(cloudlet);
        cloudlet2.setUserId(brokerId);
        cloudlet.setUserId(brokerId);
        cloudlet.addChild(cloudlet2);
        cloudlets.add(cloudlet);
        cloudlets.add(cloudlet2);
        return cloudlets;
    }

    private List<DAGCloudlet> getUserRequestCloudlets() { //represent all ms calls for a single user request
        List<DAGCloudlet> userRequestCloudlets = new ArrayList<>();
            DAGCloudlet cloudlet = new DAGCloudlet(Id.pollId(Cloudlet.class), 10000, 10, 0, 0, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            DAGCloudlet cloudlet2 = new DAGCloudlet(Id.pollId(Cloudlet.class), 10000, 10, 0, 0, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            DAGCloudlet cloudlet3 = new DAGCloudlet(Id.pollId(Cloudlet.class), 10000, 10, 0, 0, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet2.setParent(cloudlet);
            cloudlet2.addChild(cloudlet3);
            cloudlet.addChild(cloudlet2);
            cloudlet3.setParent(cloudlet2);
            userRequestCloudlets.add(cloudlet);
            userRequestCloudlets.add(cloudlet3);
            userRequestCloudlets.add(cloudlet2);

        userRequestCloudlets.forEach(dagCloudlet -> dagCloudlet.setUserId(brokerId));
        return userRequestCloudlets;
    }

    public boolean hasMoreTasks() {

        return !tasksQueue.isEmpty() || (allTasks.size() < finishedTasks.size()) || isScheduleTasksEventQueued();
    }

    boolean isScheduleTasksEventQueued() {
        final Predicate<SimEvent> scheduleTasksEventsPredicate = evt -> evt.getTag() == SCHEDULE_USER_REQUEST_TASKS;
        return CloudSim.isFutureEventQueued(scheduleTasksEventsPredicate);
    }

    public void printCloudletList() {

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent +  "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (DAGCloudlet cloudlet : finishedTasks) {
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