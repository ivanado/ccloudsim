package org.cloudbus.cloudsim.container.app;


import java.util.ArrayList;
import java.util.List;

public class UserRequest {

    public List<Task> tasks;
    public List<Microservice> msCallGraph;

    public int brokerId;

    public UserRequest(int brokerId) {
        this.tasks = new ArrayList<>();
        this.msCallGraph = MicroserviceCallGraph.get();
        this.brokerId = brokerId;
        initializeTasks();

    }

    private void initializeTasks() {
        tasks = msCallGraph.stream().map(ms -> new Task(ms, this.brokerId)).toList();
        msCallGraph.forEach(ms -> {
            Task task = tasks.stream().filter(t -> t.microservice == ms).findFirst().orElse(null);
            Task provider = null;
            Task consumer = null;
            if (ms.hasProvider()) {
                provider = tasks.stream().filter(t -> t.microservice.getId() == ms.getProvider().getId()).findFirst().orElse(null);
            }
            if (ms.hasConsumer()) {
                consumer = tasks.stream().filter(t -> t.microservice.getId() == ms.getConsumer().getId()).findFirst().orElse(null);

            }
            if (task != null) task.set(provider, consumer);
        });
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Task> getTasks(boolean independent) {
        return tasks.stream().filter(task -> {
            if (independent) {
                return task.microservice.getProvider() == null;
            } else {
                return task.microservice.getProvider() != null;
            }
        }).toList();

    }
}
