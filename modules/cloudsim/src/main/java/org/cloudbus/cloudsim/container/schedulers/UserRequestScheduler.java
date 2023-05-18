package org.cloudbus.cloudsim.container.schedulers;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.DatacenterResources;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.app.model.UserRequest;
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
import java.util.stream.Collectors;


public class UserRequestScheduler extends SimEntity {

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
        DatacenterResources.get().addUserRequest(testRequest);
        List<Task> userRequestTasks = testRequest.getTasks();
        allTasks.addAll(userRequestTasks);
        taskQueue.addAll(testRequest.getTasks(true));
        waitingTasks.addAll(testRequest.getTasks(false));

        submitTasks();
        if (allTasks.size() < 10) {
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
        sb.append("\n=======SCHEDULER QUEUES=======").append("\n");
        sb.append("All tasks list: ").append(allTasks.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append("\n");
        sb.append("Waiting tasks queue: ").append(waitingTasks.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append("\n");
        sb.append("Finished tasks list: ").append(finishedTasks.stream().map(t  -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append("\n");
        sb.append("Tasks queue: ").append(taskQueue.stream().map(t  -> String.valueOf(t.getId())).collect(Collectors.joining(", "))).append("\n");
        sb.append("=============================").append("\n");
        Log.print(sb.toString());
    }

    private void processTaskFinished(SimEvent ev) {
        Task finishedTask = (Task) ev.getData();
        finishedTasks.add(finishedTask);

        int finishedTaskMicroserviceId=finishedTask.getMicroservice().getId();
        //if consumer tasks exist in waitingList move to taskQueue
        List<Task> tasksReady = waitingTasks.stream().filter(task -> task.getMicroservice().getProvider().getId() == finishedTaskMicroserviceId).toList();
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
                + "Data center ID" + indent + "ContainerId" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time\n");

        DecimalFormat dft = new DecimalFormat("###.##");


        for (Task task : allTasks) {
            sb.append(indent + task.getCloudlet().getCloudletId() + indent + indent);


            sb.append(task.getCloudlet().getCloudletStatusString());
            sb.append(indent + indent + task.getCloudlet().getResourceId()
                    + indent + indent + task.getContainer().getId()
                    + indent + indent
                    + dft.format(task.getCloudlet().getActualCPUTime()) + indent
                    + indent + dft.format(task.getCloudlet().getExecStartTime())
                    + indent + indent
                    + dft.format(task.getCloudlet().getFinishTime())).append("\n");


        }
        Log.printLine(sb.toString());
    }


    public Task getTaskForCloudlet(ContainerCloudlet cloudlet) {
        return allTasks.stream().filter(t -> t.getCloudlet().getCloudletId() == cloudlet.getCloudletId()).findFirst().orElse(null);
    }
}