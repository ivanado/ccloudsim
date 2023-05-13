package org.cloudbus.cloudsim.container.app;


import org.cloudbus.cloudsim.container.app.algo.UserRequestType;
import org.cloudbus.cloudsim.container.utils.IDs;

import java.util.ArrayList;
import java.util.List;

public class UserRequest {
    public int id;
    public List<Task> tasks;
    public UserRequestType type;

    public int brokerId;

    public UserRequest(int brokerId) {
        this.id = IDs.pollId(UserRequest.class);
        this.type = UserRequestType.getUserRequestTypeOne();
        this.tasks = new ArrayList<>();
        this.brokerId = brokerId;
        initializeTasks();

    }

    private void initializeTasks() {
        this.tasks = this.type.msCallGraph.stream().map(ms-> new Task(ms, this.brokerId, this)).toList();
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Task> getTasks(boolean noProvider) {
        return tasks.stream().filter(task -> {
            if (noProvider) {
                return task.microservice.getProvider() == null;
            } else {
                return task.microservice.getProvider() != null;
            }
        }).toList();

    }

}
