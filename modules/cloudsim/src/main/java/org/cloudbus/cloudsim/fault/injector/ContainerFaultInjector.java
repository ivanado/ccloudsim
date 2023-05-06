package org.cloudbus.cloudsim.fault.injector;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudSimTags;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.vmplus.util.CustomLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;



public class ContainerFaultInjector extends SimEntity {
    private Map<String, List<Double>> containerFailureTimes;
    @Getter
    private double maxTimeToFail =Double.MAX_VALUE;

    @Getter @Setter
    private ContainerDatacenter datacenter;
    public ContainerFaultInjector(final ContainerDatacenter datacenter) {
        super(datacenter.getName() + "-ContainerFaultInjector");
        this.setDatacenter(datacenter);
        this.containerFailureTimes = new HashMap<>();

    }
    @Override
    public void startEntity() {
        scheduleContainerFault();
    }

    private void scheduleContainerFault() {
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != ContainerCloudSimTags.HOST_FAIL;
        if (CloudSim.clock() < getMaxTimeToFail() || CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), getNextFaultDelay(), ContainerCloudSimTags.CONTAINER_FAIL);
        }
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == ContainerCloudSimTags.CONTAINER_FAIL) {

            generateContainerFaultAndScheduleNext();
        }
    }

    private void generateContainerFaultAndScheduleNext() {
        try {
            final Container container = getRandomContainer();
            injectContainerFault(container);
        } finally {
            scheduleContainerFault();
        }
    }

    private void injectContainerFault(Container container) {
        CustomLog.printConcatLine(getClass().getSimpleName(), ": Failing container ", container.getId(), "...");
        containerFailureTimes.computeIfAbsent(container.getUid(),  h -> new ArrayList<>()).add(CloudSim.clock());
        CustomLog.printConcatLine(getClass().getSimpleName(),
                ": Sending CONTAINER_DESTROY for container #", container.getId(),
                " with uid=", container.getUid(), " from host #", container.getHost().getId(), " to datacenter ", this.datacenter.getName());

        sendNow(datacenter.getId(), ContainerCloudSimTags.CONTAINER_DESTROY, container.getId());
    }

    private Container getRandomContainer() {
        final int idx = (int) (Math.random() * datacenter.getContainerList().size());
        return datacenter.getContainerList().get(idx);
    }

    @Override
    public void shutdownEntity() {
        CustomLog.printConcatLine(getName(), " is shutting down...");

    }

    public double getNextFaultDelay() {
        PoissonDistribution poissonDistribution=new PoissonDistribution(3000);
        return poissonDistribution.sample();
    }


}
