package org.cloudbus.cloudsim.container.schedulers;

import org.apache.commons.math3.random.RandomDataGenerator;
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

import static org.cloudbus.cloudsim.vmplus.util.TextUtil.NEW_LINE;


public class UserRequestScheduler extends SimEntity {

    private static int MAX_CONCURRENT_USER_REQUESTS = 2;
    public int brokerId;

    public List<Task> finishedTasks;
    public List<Task> allTasks;
    public Queue<Task> taskQueue;
    public List<Task> waitingTasks;
    public final Queue<UserRequest> userRequestQueue;

    private int runningUserRequestCount = 0;
    private final DatacenterMetrics dcMetrics = DatacenterMetrics.get();
    final RandomDataGenerator randomDataGenerator = new RandomDataGenerator();


    public UserRequestScheduler(String name, int brokerId, List<UserRequest> allUserRequests) {
        super(name);
        this.brokerId = brokerId;
        this.allTasks = new ArrayList<>();
        this.finishedTasks = new ArrayList<>();
        this.waitingTasks = new ArrayList<>();
        this.taskQueue = new LinkedList<>();
        this.userRequestQueue = new LinkedList<>(allUserRequests);
        MAX_CONCURRENT_USER_REQUESTS = allUserRequests.size();
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
        return 1;//randomDataGenerator.nextPoisson(100);
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case ContainerCloudSimTags.USER_REQUEST_SUBMIT -> enqueueUserRequestTasksAndScheduleNext();
            case ContainerCloudSimTags.USER_REQUEST_RETURN -> processUserRequestFinished(ev);
            case ContainerCloudSimTags.TASK_RETURN -> processTaskFinished(ev);
            case ContainerCloudSimTags.TASK_WAIT -> processTaskWait(ev);
        }
    }

    private void processTaskWait(SimEvent ev) {
        Task task = (Task) ev.getData();
        waitingTasks.add(task);
    }

    private void processUserRequestFinished(SimEvent ev) {
        UserRequest userRequest = (UserRequest) ev.getData();
        runningUserRequestCount--;
        Log.printLine(getName(), ": User request #", userRequest, " finished processing. ", dcMetrics.getFinishedUserRequestTasks(userRequest), " finished tasks");
    }


    private void enqueueUserRequestTasksAndScheduleNext() {
        while (runningUserRequestCount < MAX_CONCURRENT_USER_REQUESTS && !userRequestQueue.isEmpty()) {


            UserRequest ur = userRequestQueue.poll();
        Log.printLine(getName(), ": Scheduling next user request #", ur.toString());
            dcMetrics.addUserRequest(ur);
            List<Task> userRequestTasks = ur.getTasks();
            allTasks.addAll(userRequestTasks);
            taskQueue.addAll(ur.getInitialReadyTasks());
            waitingTasks.addAll(ur.getInitialWaitingTasks());


            runningUserRequestCount++;
        }
        submitTasks();
        if (!userRequestQueue.isEmpty()) {
            scheduleNextUserRequest();
        }
    }

    private void submitTasks() {
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            Log.printLine(getName(), ": Submitting task with container #", task.getContainer().getId(), " and cloudlet #", task.getCloudlet().getCloudletId());

            if (!dcMetrics.hasRunningHostsWithResourcesAvailable(task.getContainer().getNumberOfPes())) {
                //no available resources to execute cloudlet. return the task to waiting
                if(!waitingTasks.contains(task)){
                    waitingTasks.add(task);
                }

            } else {
                sendNow(brokerId, ContainerCloudSimTags.TASK_SUBMIT, task);
            }
        }
        printQueues();
    }

    private void printQueues() {
        StringBuilder sb = new StringBuilder();
        sb.append(NEW_LINE).append("=======SCHEDULER QUEUES=======").append(NEW_LINE);
        sb.append(String.format("%20s", "All tasks list: ")).append(allTasks.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append(NEW_LINE);
        sb.append(String.format("%20s", "Waiting tasks queue: ")).append(waitingTasks.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append(NEW_LINE);
        sb.append(String.format("%20s", "Finished tasks list: ")).append(finishedTasks.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append(NEW_LINE);
        sb.append(String.format("%20s", "Tasks queue: ")).append(taskQueue.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append(NEW_LINE);
        sb.append("=============================").append(NEW_LINE);
        Log.print(sb.toString());
    }

    private void processTaskFinished(SimEvent ev) {
        Task finishedTask = (Task) ev.getData();
        finishedTasks.add(finishedTask);


        dcMetrics.finishTask(finishedTask);

        List<Task> finishedTaskConsumers = finishedTask.getConsumers();
        //if consumer tasks exist in waitingList move to taskQueue
        if (!finishedTaskConsumers.isEmpty()) {
            enqueueReadyTasks(finishedTaskConsumers);
        }
        enqueueReadyTasks(waitingTasks);

        submitTasks();

        if (dcMetrics.areUserRequestTasksProcessed(finishedTask.getUserRequest())) {
            runningUserRequestCount--;
            sendNow(getId(), ContainerCloudSimTags.USER_REQUEST_RETURN, finishedTask.getUserRequest());
        }
    }


    private void enqueueReadyTasks(List<Task> tasks) {
        List<Task> readyToProcessTasks = getReadyTasks(tasks);
        tasks.removeAll(readyToProcessTasks);
        taskQueue.addAll(readyToProcessTasks);
    }

    private List<Task> getReadyTasks(List<Task> waitingTasks) {
        return waitingTasks.stream().filter(t ->
                finishedTasks.containsAll(t.getProviders())).toList();
//                        && dcMetrics.hasRunningHostsWithResourcesAvailable(t.getContainer().getNumberOfPes())).toList();
    }


    public boolean hasMoreTasks() {
        return !taskQueue.isEmpty() && !waitingTasks.isEmpty() || (allTasks.size() < finishedTasks.size()) || isScheduleTasksEventQueued();
    }

    boolean isScheduleTasksEventQueued() {
        final Predicate<SimEvent> scheduleTasksEventsPredicate = evt -> evt.getTag() == ContainerCloudSimTags.USER_REQUEST_SUBMIT;
        return CloudSim.isFutureEventQueued(scheduleTasksEventsPredicate);
    }

    public void printTasksReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(NEW_LINE);
        sb.append("============================================================= OUTPUT ======================================================================================").append(NEW_LINE);
        sb.append(String.format("%20s %20s %20s %20s %20s %20s %20s %20s ", "UserRequestId", "Cloudlet ID", "STATUS", "Data center ID", "ContainerId", "Time", "Start Time", "Finish Time")).append(NEW_LINE);

        DecimalFormat dft = new DecimalFormat("###.##");


        for (Task task : allTasks) {
            sb.append(String.format("%20s %20s %20s %20s %20s %20s %20s %20s ",
                    task.getUserRequest().getId(),
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