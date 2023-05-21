package org.cloudbus.cloudsim.container.faultInjectors;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudSimTags;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ContainerFaultInjector extends SimEntity {
    private final ContainerDatacenter datacenter;
    private Map<Integer, List<Double>> microserviceContainerFailureTimes;
    private DatacenterMetrics dcResources;

    private RandomDataGenerator randomData;

    public ContainerFaultInjector(final ContainerDatacenter datacenter) {
        super(datacenter.getName() + "-ContainerFaultInjector");
        this.datacenter = datacenter;
        this.microserviceContainerFailureTimes = new HashMap<>();
        this.dcResources = DatacenterMetrics.get();
        this.randomData =new RandomDataGenerator();
    }

    @Override
    public void startEntity() {
        scheduleContainerFault();

    }

    private void scheduleContainerFault() {
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != ContainerCloudSimTags.CONTAINER_FAIL;
        if (CloudSim.clock() < Double.MAX_VALUE || CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), getNextFaultDelay(), ContainerCloudSimTags.CONTAINER_FAIL);
        }
    }

    private double getNextFaultDelay() {
        return randomData.nextPoisson(1000);
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == ContainerCloudSimTags.CONTAINER_FAIL) {
            failRandomContainerAndScheduleNext();
        }

    }

    private void failRandomContainerAndScheduleNext() {
        Container containerToFail = dcResources.getRandomContainerToFail();
        sendNow(datacenter.getId(), ContainerCloudSimTags.CLOUDLET_FAIL, containerToFail);
        dcResources.getMicroserviceContainerFailureTimes().computeIfAbsent(containerToFail.getMicroserviceId(), c -> new ArrayList<>()).add(CloudSim.clock());
    }



    @Override
    public void shutdownEntity() {
        Log.printLine(getName(), ": is shutting down...");
    }


}
