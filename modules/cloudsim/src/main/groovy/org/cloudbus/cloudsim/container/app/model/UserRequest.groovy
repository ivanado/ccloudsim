package org.cloudbus.cloudsim.container.app.model

import org.cloudbus.cloudsim.container.utils.IDs

class UserRequest {
    int id
    List<Task> tasks
    UserRequestType type
    int brokerId

    UserRequest(int brokerId) {
        this.id = IDs.pollId(UserRequest.class)
        this.type = UserRequestType.getUserRequestTypeOne()
        this.tasks = new ArrayList<>()
        this.brokerId = brokerId
        initializeTasks()

    }

    private void initializeTasks() {
        this.tasks = this.type.msCallGraph.collect { ms -> new Task(ms, this.brokerId, this) }
    }


    List<Task> getTasks(boolean noProvider) {

        return this.tasks.findAll { task ->

            return noProvider
                    ? task.microservice.getProvider() == null
                    : task.microservice.getProvider() != null
        }
    }
}
