package org.cloudbus.cloudsim.container.schedulers;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.app.model.UserRequest;
import org.cloudbus.cloudsim.container.core.ContainerCloudSimTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class UserRequestScheduler extends SimEntity {

    public static final String NEW_LINE = "\n";
    int MAX_CONCURRENT_USER_REQUESTS = 1;
    private final List<UserRequest> allUserRequests;
    public int brokerId;

    public List<Task> finishedTasks;
    public List<Task> allTasks;
    public Queue<Task> taskQueue;
    public List<Task> waitingTasks;
    public final Queue<UserRequest> userRequestQueue;

    private int runningUserRequestCount = 0;

    public UserRequestScheduler(String name, int brokerId, List<UserRequest> allUserRequests) {
        super(name);
        this.brokerId = brokerId;
        this.allTasks = new ArrayList<>();
        this.finishedTasks = new ArrayList<>();
        this.waitingTasks = new ArrayList<>();
        this.taskQueue = new LinkedList<>();
        this.allUserRequests = allUserRequests;
        this.userRequestQueue = new LinkedList<>(allUserRequests);
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
        while (runningUserRequestCount < MAX_CONCURRENT_USER_REQUESTS && !userRequestQueue.isEmpty()) {

            UserRequest ur = userRequestQueue.poll();
            DatacenterMetrics.get().addUserRequest(ur);
            List<Task> userRequestTasks = ur.getTasks();
            allTasks.addAll(userRequestTasks);
            taskQueue.addAll(ur.getInitialReadyTasks());
            waitingTasks.addAll(ur.getInitialWaitingTasks());

            submitTasks();
            runningUserRequestCount++;
        }


        if (!userRequestQueue.isEmpty()) {
            scheduleNextUserRequest();
        }
    }

    private void submitTasks() {
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            Log.printLine(getName(), ": Submitting task with container #", task.getContainer().getId(), " and cloudlet #", task.getCloudlet().getCloudletId());
            sendNow(brokerId, ContainerCloudSimTags.TASK_SUBMIT, task);
        }
        printQueues();
    }

    private void printQueues() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=======SCHEDULER QUEUES=======").append(NEW_LINE);
        sb.append("All tasks list: ").append(allTasks.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append(NEW_LINE);
        sb.append("Waiting tasks queue: ").append(waitingTasks.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append(NEW_LINE);
        sb.append("Finished tasks list: ").append(finishedTasks.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append(NEW_LINE);
        sb.append("Tasks queue: ").append(taskQueue.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append(NEW_LINE);
        sb.append("=============================").append(NEW_LINE);
        Log.print(sb.toString());
    }

    private void processTaskFinished(SimEvent ev) {
        Task finishedTask = (Task) ev.getData();
        finishedTasks.add(finishedTask);

        int finishedTaskMicroserviceId = finishedTask.getMicroservice().getId();
        List<Task> tasksReady =   finishedTask.getConsumers();
        //if consumer tasks exist in waitingList move to taskQueue
        if (!tasksReady.isEmpty()) {
            taskQueue.addAll(tasksReady);
            waitingTasks.removeAll(tasksReady);

            submitTasks();
        }
        if (allTasksProcessed()) {
            runningUserRequestCount--;
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
        sb.append(NEW_LINE);
        sb.append("============================================================= OUTPUT ======================================================================================").append(NEW_LINE);
        sb.append(String.format("%20s %20s %20s %20s %20s %20s %20s ", "Cloudlet ID", "STATUS", "Data center ID", "ContainerId", "Time", "Start Time", "Finish Time")).append(NEW_LINE);

        DecimalFormat dft = new DecimalFormat("###.##");


        for (Task task : allTasks) {
            sb.append(String.format("%20s %20s %20s %20s %20s %20s %20s ",
                    task.getCloudlet().getCloudletId(),
                    task.getCloudlet().getCloudletStatusString(),
                    task.getCloudlet().getResourceId(),
                    task.getContainer().getId(),
                    dft.format(task.getCloudlet().getActualCPUTime()),
                    dft.format(task.getCloudlet().getExecStartTime()),
                    dft.format(task.getCloudlet().getFinishTime()))).append(NEW_LINE);


        }
        Log.printLine(sb.toString());
    }
}