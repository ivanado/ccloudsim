package org.cloudbus.cloudsim.container.app.model

import org.cloudbus.cloudsim.container.utils.IDs

class UserRequest {
    int id
    List<Task> tasks
    UserRequestType type
    int brokerId

    UserRequest(int brokerId) {
        this.id = IDs.pollId(UserRequest.class)
        this.type = UserRequestType.getUserRequestType()
        this.tasks = new ArrayList<>()
        this.brokerId = brokerId
        initializeTasks()

    }

    private void initializeTasks() {
        this.tasks = this.type.msCallGraph.collect { ms -> new Task(ms, this.brokerId, this) }
        for(Task task: this.tasks){
            for( Microservice p : task.microservice.providers){
                task.setProvider(this.tasks.find {it.microservice.getId() == p.getId()})
            }
            for( Microservice c : task.microservice.consumers){
                task.setConsumer(this.tasks.find {it.microservice.getId() == c.getId()})
            }
        }

    }

    List<Task> getInitialReadyTasks(){
        this.tasks.findAll {it.providers.isEmpty()}
    }
    List<Task> getInitialWaitingTasks(){
        this.tasks.findAll {!it.providers.isEmpty()}
    }
}
