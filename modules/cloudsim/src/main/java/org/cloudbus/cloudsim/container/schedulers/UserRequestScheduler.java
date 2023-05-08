package org.cloudbus.cloudsim.container.schedulers;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.Task;
import org.cloudbus.cloudsim.container.app.UserRequest;
import org.cloudbus.cloudsim.container.core.ContainerCloudSimTags;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;


public class UserRequestScheduler extends SimEntity {

    public static final int MAX_USER_REQUESTS = 3;
    public int brokerId;

    public List<Task> finishedTasks;
    public List<Task> allTasks;
    public Queue<Task> taskQueue;
    public List<Task> waitingTasks;

    public UserRequestScheduler(String name, int brokerId) {
        super(name);
        this.brokerId = brokerId;
        this.allTasks = new ArrayList<>();
        this.finishedTasks = new ArrayList<>();
        this.waitingTasks = new ArrayList<>();
        this.taskQueue = new LinkedList<>();
    }

    @Override
    public void startEntity() {
        Log.printLine(getName(), " is starting...");
        scheduleNextUserRequest();
    }

    @Override
    public void shutdownEntity() {
        Log.printLine(getName(), " is shutting down...");
    }

    private void scheduleNextUserRequest() {
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != ContainerCloudSimTags.USER_REQUEST_SUBMIT;
        if (CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), getNextUserRequestDelay(), ContainerCloudSimTags.USER_REQUEST_SUBMIT);
        }

    }

    private double getNextUserRequestDelay() {
        return Math.random() * 100;
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case ContainerCloudSimTags.USER_REQUEST_SUBMIT -> enqueueUserRequestTasksAndScheduleNext();
            case ContainerCloudSimTags.USER_REQUEST_RETURN -> processUserRequestFinished();
            case ContainerCloudSimTags.TASK_RETURN -> processTaskFinished(ev);
        }
    }

    private void processUserRequestFinished() {
        Log.printLine(getName(), ": User request finished processing. ", finishedTasks.size(), " finished tasks");
    }


    private void enqueueUserRequestTasksAndScheduleNext() {

        UserRequest testRequest = new UserRequest(brokerId);
        List<Task> userRequestTasks = testRequest.getTasks();
        allTasks.addAll(userRequestTasks);
        taskQueue.addAll(testRequest.getTasks(true));
        waitingTasks.addAll(testRequest.getTasks(false));

        submitTasks();
        if(allTasks.size()<10){
            scheduleNextUserRequest();
        }


    }

    private void submitTasks() {
        while (!taskQueue.isEmpty()) {
            sendNow(brokerId, ContainerCloudSimTags.TASK_SUBMIT, taskQueue.poll());
        }
    }

    private void processTaskFinished(SimEvent ev) {
        Task finishedTask = (Task) ev.getData();
        finishedTasks.add(finishedTask);

        //if consumer tasks exist in waitingList move to taskQueue
        List<Task> tasksReady = waitingTasks.stream().filter(task -> task.getProvider() == finishedTask).toList();
        if (!tasksReady.isEmpty()) {
            taskQueue.addAll(tasksReady);
            waitingTasks.removeAll(tasksReady);

            submitTasks();
        }
        if (allTasksProcessed()) {
            sendNow(getId(), ContainerCloudSimTags.USER_REQUEST_RETURN);
        }
    }


    public boolean hasMoreTasks() {
        return !taskQueue.isEmpty() || (allTasks.size() < finishedTasks.size()) || isScheduleTasksEventQueued();
    }

    public boolean allTasksProcessed() {
        return allTasks.size() == finishedTasks.size();
    }

    boolean isScheduleTasksEventQueued() {
        final Predicate<SimEvent> scheduleTasksEventsPredicate = evt -> evt.getTag() == ContainerCloudSimTags.USER_REQUEST_SUBMIT;
        return CloudSim.isFutureEventQueued(scheduleTasksEventsPredicate);
    }

    public void printTasksReport() {
        StringBuilder sb = new StringBuilder();
        String indent = "    ";
        sb.append("\n");
        sb.append("========== OUTPUT ==========").append("\n");
        sb.append("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time\n");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (Task task : finishedTasks) {
            sb.append(indent + task.cloudlet.getCloudletId() + indent + indent);

            if (task.cloudlet.getCloudletStatusString() == "Success") {
                sb.append("SUCCESS");
                sb.append(indent + indent + task.cloudlet.getResourceId()
                        + indent + indent
                        + indent + indent
                        + dft.format(task.cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(task.cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(task.cloudlet.getFinishTime())).append("\n");
            }


            Log.printLine(sb.toString());
        }

        for (Task task : allTasks.stream().filter(t-> !finishedTasks.contains(t)).toList()) {
            sb.append(indent + task.cloudlet.getCloudletId() + indent + indent);

            if (task.cloudlet.getCloudletStatusString() != "Success") {
                sb.append(task.cloudlet.getCloudletStatusString());
                sb.append(indent + indent + task.cloudlet.getResourceId()
                        + indent + indent
                        + indent + indent
                        + dft.format(task.cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(task.cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(task.cloudlet.getFinishTime())).append("\n");
            }


            Log.printLine(sb.toString());
        }
    }

    public int getProcessedTasksCount() {
        return finishedTasks.size();
    }

    public Task getTaskForCloudlet(ContainerCloudlet cloudlet) {
        return allTasks.stream().filter(t -> t.cloudlet.getCloudletId() == cloudlet.getCloudletId()).findFirst().orElse(null);
    }
}