package org.cloudbus.cloudsim.container.app.model

import org.cloudbus.cloudsim.UtilizationModelFull
import org.cloudbus.cloudsim.UtilizationModelNull
import org.cloudbus.cloudsim.container.core.Container
import org.cloudbus.cloudsim.container.core.ContainerCloudlet
import org.cloudbus.cloudsim.container.schedulers.ContainerCloudletSchedulerSpaceShared
import org.cloudbus.cloudsim.container.utils.IDs

class Task {

    Microservice microservice
    ContainerCloudlet cloudlet
    Container container
//    Task provider
//    Task consumer
    List<Task> consumers = []
    List<Task> providers = []
    UserRequest userRequest
    int id

    Task(Microservice microservice, int brokerId, UserRequest userRequest) {
        this.id = IDs.pollId(Task.class)
        this.microservice = microservice
        this.cloudlet = new ContainerCloudlet(IDs.pollId(ContainerCloudlet.class), 1000, 10, 1, 1, new UtilizationModelFull(), new UtilizationModelNull(), new UtilizationModelNull())
        this.cloudlet.setUserId(brokerId)
        this.container = new Container(IDs.pollId(Container.class), microservice.getId(), brokerId, 10, 10, 0, 0, 0, "", new ContainerCloudletSchedulerSpaceShared(), 300)
        this.userRequest = userRequest
    }

    void setProvider(Task provider) {
        providers.add(provider)
    }


    void setConsumer(Task consumer) {
        consumers.add(consumer)
    }

    @Override
    boolean equals(Object obj) {
        return this.id == ((Task) obj).id
    }

    @Override
    String toString() {
        return "Task-$id"
    }
//
//    void set(Task provider, Task consumer) {
//        this.providers.add(provider)
//        this.consumers.add(consumer)
//    }
}
