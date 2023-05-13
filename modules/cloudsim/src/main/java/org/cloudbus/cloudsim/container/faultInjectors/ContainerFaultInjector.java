package org.cloudbus.cloudsim.container.faultInjectors;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudSimTags;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.core.DatacenterResources;
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
    private DatacenterResources dcResources;

    public ContainerFaultInjector(final ContainerDatacenter datacenter) {
        super(datacenter.getName() + "-ContainerFaultInjector");
        this.datacenter = datacenter;
        this.microserviceContainerFailureTimes = new HashMap<>();
        this.dcResources = DatacenterResources.get();
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
        PoissonDistribution poissonDistribution = new PoissonDistribution(100);
        return poissonDistribution.sample();
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == ContainerCloudSimTags.CONTAINER_FAIL) {
            failRandomContainerAndScheduleNext();
        }

    }

    private void failRandomContainerAndScheduleNext() {
        Container containerToFail = getRandomContainer();
        datacenter.removeRunningContainer(containerToFail);
        sendNow(datacenter.getId(), CloudSimTags.CLOUDLET_CANCEL, containerToFail);
        dcResources.microserviceContainerFailureTimes.computeIfAbsent(containerToFail.getMicroserviceId(), c -> new ArrayList<>()).add(CloudSim.clock());
    }

    private Container getRandomContainer() {
        if (datacenter.getContainerList().isEmpty()) {
            return null;
        }
        List<Container> runningContainers = datacenter.getContainerList();
        if (runningContainers.isEmpty()) {
            return null;
        }
        final int idx = (int) (Math.random() * runningContainers.size());
        return runningContainers.get(idx);

    }

    @Override
    public void shutdownEntity() {
        Log.printLine(getName(), ": is shutting down...");
    }


}
