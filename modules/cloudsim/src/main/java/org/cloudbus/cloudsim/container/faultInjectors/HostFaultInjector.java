package org.cloudbus.cloudsim.container.faultInjectors;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudSimTags;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class HostFaultInjector extends SimEntity {
    @Getter
    @Setter
    private ContainerDatacenter datacenter;
    private final Map<ContainerHost, List<Double>> hostFailureTimes;
    private DatacenterMetrics dcResources = DatacenterMetrics.get();
    private RandomDataGenerator  randomData;
    public HostFaultInjector(final ContainerDatacenter datacenter) {
        super(datacenter.getName() + "-ContainerHostFaultInjector");
        this.setDatacenter(datacenter);
        this.hostFailureTimes = new HashMap<>();
        this.randomData = new RandomDataGenerator();
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

    private void scheduleHostRecovery(ContainerHost host) {
        schedule(getId(), getNextFaultDelay(), ContainerCloudSimTags.HOST_RECOVER, host);
    }

    private double getNextFaultDelay() {

   long x =   randomData.nextPoisson(0.0003);
   return x;
//        return Math.random() * 3600;
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == ContainerCloudSimTags.HOST_FAIL) {
            generateHostFaultAndScheduleNext();
        } else if (ev.getTag() == ContainerCloudSimTags.HOST_RECOVER) {
            hostRecovery(ev);
        }
    }

    private void generateHostFaultAndScheduleNext() {
        try {
            final ContainerHost host = DatacenterMetrics.get().getRandomHost();
            if (host != null) {
                injectHostFault(host);
                scheduleHostRecovery(host);
            }


        } finally {
            if (dcResources.hasRunningHosts()) {
                scheduleHostFault();
            }
        }
    }

    private void hostRecovery(SimEvent event) {
        ContainerHost host = (ContainerHost) event.getData();
        Log.printLine(getClass().getSimpleName(), ": recovering host #", host.getId());
        host.setFailed(false);

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
        host.setFailed(true);
        dcResources.getHostFailureTimes().computeIfAbsent(host, h -> new ArrayList<>()).add(CloudSim.clock());

        dcResources.getRunningHosts().remove(host);
        dcResources.getFailedHosts().add(host);
    }


    private void failHostContainers(ContainerHost host) {
        List<Container> hostContainers = host.getContainerList();
        hostContainers.forEach(this::failHostContainer);
    }

    private void failHostContainer(Container container) {
        Log.printLine(getClass().getSimpleName(),
                ": Sending CONTAINER_DESTROY for container #", container.getId(),
                " with uid=", container.getUid(), " from host #", container.getHost().getId(), " to datacenter ", this.datacenter.getName());
        sendNow(datacenter.getId(), ContainerCloudSimTags.CLOUDLET_FAIL, container);
    }


    @Override
    public void shutdownEntity() {

    }

    public static void main(String[] args) {
        RandomDataGenerator randomData = new RandomDataGenerator();
        for(int i =0; i <300;i++){
            long x =   randomData.nextPoisson(200);
            System.out.println("->"+x);
        }

    }

}
