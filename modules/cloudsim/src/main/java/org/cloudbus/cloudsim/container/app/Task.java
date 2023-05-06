package org.cloudbus.cloudsim.container.app;

import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.UtilizationModelNull;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.container.schedulers.ContainerCloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.container.utils.IDs;

public class Task {

    public Microservice microservice;


    public ContainerCloudlet cloudlet;


    public Container container;

    public Task provider;

    public Task consumer;

    public Task(Microservice microservice, int brokerId) {
        this.microservice = microservice;
        this.cloudlet = new ContainerCloudlet(IDs.pollId(ContainerCloudlet.class), 1000, 10, 1, 1, new UtilizationModelFull(), new UtilizationModelNull(), new UtilizationModelNull());
        this.cloudlet.setUserId(brokerId);
        this.container = new Container(IDs.pollId(ContainerCloudlet.class), brokerId, 10, 10, 0, 0, 0, "", new ContainerCloudletSchedulerTimeShared(), 300);
        this.provider = null;
        this.consumer = null;

    }

    public Task getProvider() {
        return provider;
    }


    public void set(Task provider, Task consumer) {
        this.provider = provider;
        this.consumer = consumer;
    }
}
