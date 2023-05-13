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
    Task provider
    Task consumer
    UserRequest userRequest
    int id

    Task(Microservice microservice, int brokerId, UserRequest userRequest) {
        this.id = IDs.pollId(Task.class)
        this.microservice = microservice
        this.cloudlet = new ContainerCloudlet(IDs.pollId(ContainerCloudlet.class), 1000, 10, 1, 1, new UtilizationModelFull(), new UtilizationModelNull(), new UtilizationModelNull())
        this.cloudlet.setUserId(brokerId)
        this.container = new Container(IDs.pollId(Container.class), microservice.getId(), brokerId, 10, 10, 0, 0, 0, "", new ContainerCloudletSchedulerSpaceShared(), 300)
        this.provider = null
        this.consumer = null
        this.userRequest = userRequest
    }


    @Override
    boolean equals(Object obj) {
        return this.id == ((Task) obj).id
    }


    void set(Task provider, Task consumer) {
        this.provider = provider
        this.consumer = consumer
    }
}
