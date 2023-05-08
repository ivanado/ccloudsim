package org.cloudbus.cloudsim.container.faultInjectors;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudSimTags;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.List;
import java.util.function.Predicate;

public class ContainerHostFaultInjector extends SimEntity {
    @Getter
    @Setter
    private ContainerDatacenter datacenter;

    public ContainerHostFaultInjector(final ContainerDatacenter datacenter) {
        super(datacenter.getName() + "-ContainerHostFaultInjector");
        this.setDatacenter(datacenter);
    }

    @Override
    public void startEntity() {
        scheduleHostFault();
    }

    private void scheduleHostFault() {
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != ContainerCloudSimTags.HOST_FAIL;
        if (CloudSim.clock() < Double.MAX_VALUE || CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), getNextFaultDelay(), ContainerCloudSimTags.HOST_FAIL);
        }
    }

    private double getNextFaultDelay() {
        PoissonDistribution poissonDistribution = new PoissonDistribution(100);
        return poissonDistribution.sample();
//        return Math.random() * 3600;
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == ContainerCloudSimTags.HOST_FAIL) {
            generateHostFaultAndScheduleNext();
        }
    }

    private void generateHostFaultAndScheduleNext() {
        try {
            final ContainerHost host = getRandomHost();
            if (host != null) {
                injectHostFault(host);
                scheduleHostRecovery(host);
            }


        } finally {
            if (datacenter.hasRunningHosts()) {
                scheduleHostFault();
            }
        }
    }

    private void scheduleHostRecovery(ContainerHost host) {
    }

    private void injectHostFault(ContainerHost host) {
        if (host == null) {
            return;
        }
        Log.printLine(getClass().getSimpleName(), ": Failing host ", host.getId(), "...");


        failHost(host);
    }

    private void failHost(ContainerHost host) {

        final List<String> containerIds = host.getContainerList().stream().map(c -> String.valueOf(c.getId())).toList();

        final String msg = containerIds.size() > 0
                ? "affecting all its %d containers [%s]".formatted(containerIds.size(), String.join(",", containerIds))
                : "but there was no running containers";
        Log.printLine(getClass().getSimpleName(),
                String.format(" : All %d PEs from host #%s failed at %f, %s.",

                        host.getNumberOfPes(), host.getId(),
                        CloudSim.clock(), msg));
        failHostContainers(host);
//        failedHosts.add(lastFailedHost);
        host.setFailed(true);
    }


    private void failHostContainers(ContainerHost host) {
        List<Container> hostContainers = host.getContainerList();
        hostContainers.forEach(this::failHostContainer);
    }

    private void failHostContainer(Container container) {
        Log.printLine(getClass().getSimpleName(),
                ": Sending CONTAINER_DESTROY for container #", container.getId(),
                " with uid=", container.getUid(), " from host #", container.getHost().getId(), " to datacenter ", this.datacenter.getName());
        sendNow(datacenter.getId(), CloudSimTags.CLOUDLET_CANCEL, container);

//        sendNow(datacenter.getId(), ContainerCloudSimTags.CONTAINER_DESTROY, container);
    }

    private ContainerHost getRandomHost() {
        if (datacenter.getHostList().isEmpty()) {
            return null;
        }
        List<ContainerHost> runningHosts = datacenter.getHostList().stream().filter(host -> !host.isFailed()).toList();
        if (runningHosts.isEmpty()) {
            return null;
        }
        final int idx = (int) (Math.random() * runningHosts.size());
        return runningHosts.get(idx);
    }


    @Override
    public void shutdownEntity() {

    }
}
