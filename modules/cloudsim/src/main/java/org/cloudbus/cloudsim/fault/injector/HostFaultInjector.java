package org.cloudbus.cloudsim.fault.injector;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPe;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;

import static org.cloudbus.cloudsim.fault.injector.FaultInjectionCloudSimTags.HOST_FAIL;
import static org.cloudbus.cloudsim.fault.injector.FaultInjectionCloudSimTags.HOST_RECOVER;

public class HostFaultInjector extends SimEntity {


    @Getter
    private ContainerDatacenter datacenter;

    @Getter
    @Setter
    private int totalHostFaults = 0;

    protected final void setDatacenter(@NonNull final ContainerDatacenter datacenter) {
        this.datacenter = datacenter;
    }

    @Getter
    private ContainerHost lastFailedHost;

    private final Map<ContainerHost, List<Double>> hostFailureTimes;

    private final List<ContainerHost> failedHosts = new ArrayList<>();

    @Getter
    private double maxTimeToFail = Double.MAX_VALUE;

    public HostFaultInjector(final ContainerDatacenter datacenter) {
        super(datacenter.getName() + "-HostFaultInjector");
        this.setDatacenter(datacenter);
        this.lastFailedHost = null;
        this.hostFailureTimes = new HashMap<>();

    }

    @Override
    public void startEntity() {
        scheduleHostFault();
    }

    private void scheduleHostFault() {
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != HOST_FAIL;
        if (CloudSim.clock() < getMaxTimeToFail() || CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), getNextFaultDelay(), HOST_FAIL);
        }
    }

    private double getNextFaultDelay() {
        PoissonDistribution poissonDistribution = new PoissonDistribution(40000);
        return poissonDistribution.sample();
//        return Math.random() * 3600;
    }


    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == HOST_FAIL) {
            generateHostFaultAndScheduleNext();
        } else if (ev.getTag() == HOST_RECOVER) {
            recoverHost();
        }
    }

    private void recoverHost() {
        if (failedHosts.size() > 0) {
            ContainerHost host = failedHosts.remove(0);
            Log.printConcatLine(getClass().getSimpleName(), ": Recovering host ", host.getId());
            host.getPeList().stream().map(ContainerVmPe::getId).forEach(id ->
                    host.setPeStatus(id, Pe.FREE)
            );
        }


    }

    private void generateHostFaultAndScheduleNext() {
        try {
            final ContainerHost host = getRandomHostContainingVMs();
            injectHostFault(host);
            scheduleHostRecovery(host);
        } finally {
            scheduleHostFault();
        }
    }

    private void scheduleHostRecovery(ContainerHost host) {
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != HOST_FAIL;
        if (CloudSim.clock() < getMaxTimeToFail() || CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), 3600, HOST_RECOVER);
        }

    }


    private ContainerHost getRandomHost() {
        if (datacenter.getHostList().isEmpty()) {
            return null;
        }

        final int idx = (int) (Math.random() * datacenter.getHostList().size());
        return datacenter.getHostList().get(idx);
    }

    private ContainerHost getRandomHostContainingVMs() {
        if (datacenter.getHostList().isEmpty()) {
            return null;
        }

        List<ContainerHost> hostsWithVM = datacenter.getHostList().stream().filter(host -> host.getVmList().size() > 0).toList();
        if (hostsWithVM.isEmpty()) {
            return null;
        }

        final int idx = (int) (Math.random() * hostsWithVM.size());
        return hostsWithVM.get(idx);
    }

    private void injectHostFault(ContainerHost host) {
        if (host == null) {
            return;
        }
        Log.printConcatLine(getClass().getSimpleName(), ": Failing host ", host.getId(), "...");
        this.lastFailedHost = host;

        totalHostFaults++;
        hostFailureTimes.computeIfAbsent(lastFailedHost, h -> new ArrayList<>()).add(CloudSim.clock());

        this.lastFailedHost.getPeList().stream().map(ContainerVmPe::getId).forEach(id ->
                this.lastFailedHost.setPeStatus(id, Pe.FAILED)
        );

        failHost();
    }

    private String formatVmsToHosts() {
        return String.join(",", lastFailedHost.getVmList().stream().map(vm -> "vm#" + vm.getId() + "=[" + getVmContainerIdsAsString(vm) + "]").toList());
    }

    private String getVmContainerIdsAsString(ContainerVm vm) {
        return String.join(",", vm.getContainerList().stream().map(container -> String.valueOf(container.getId())).toList());
    }

    private void failHost() {

        final List<String> vmIds = lastFailedHost.getVmList().stream().map(vm -> String.valueOf(vm.getId())).toList();

        final String msg = vmIds.size() > 0
                ? "affecting all its %d vms [%s] containers".formatted(vmIds.size(), formatVmsToHosts())
                : "but there was no running VM";
        Log.printConcatLine(Level.SEVERE,
                String.format(" %s: All %d PEs from host #%s failed at %f, %s.",
                        getClass().getSimpleName(),
                        lastFailedHost.getNumberOfPes(), lastFailedHost.getId(),
                        CloudSim.clock(), msg), null);
        failHostContainers();
        failHostVms();
        failedHosts.add(lastFailedHost);
    }


    private void failHostContainers() {
        List<Container> hostContainers = lastFailedHost.getVmList().stream().map(ContainerVm::getContainerList).flatMap(Collection::stream).toList();
        hostContainers.forEach(container -> failHostContainer(container));
    }

    private void failHostContainer(Container container) {
        Log.printConcatLine(getClass().getSimpleName(),
                ": Sending CONTAINER_DESTROY for container #", container.getId(),
                " with uid=", container.getUid(), " from vm #", container.getVm().getId(), " to datacenter ", this.datacenter.getName());

        sendNow(datacenter.getId(), FaultInjectionCloudSimTags.CONTAINER_DESTROY, container);
    }

    private void failHostVms() {
        lastFailedHost.getVmList().forEach(vm -> {
            Log.printConcatLine(getClass().getSimpleName(),
                    ": Sending VM_DESTROY for vm #", vm.getId(),
                    " with uid=", vm.getUid(), " to datacenter ", this.datacenter.getName());
            sendNow(datacenter.getId(), CloudSimTags.VM_DESTROY, vm);
        });
    }

    @Override
    public void shutdownEntity() {
        Log.printConcatLine(getName(), " is shutting down...");
    }

    public int getHostFaultsNumber() {
        return totalHostFaults;
    }

    public long meanTimeBetweenHostFaultsInMinutes() {
        return (long) (meanTimeBetweenHostFaultsInSeconds() / 60.0);
    }

    public long meanTimeBetweenHostFaultsInSeconds() {
        final double[] faultTimes = hostFailureTimes
                .values()
                .stream()
                .flatMap(Collection::stream)
                .mapToDouble(time -> time)
                .sorted()
                .toArray();

        if (faultTimes.length == 0) {
            return 0;
        }

        //Computes the differences between failure times t2 - t1
        double sum = 0;
        double previous = faultTimes[0];
        for (final double time : faultTimes) {
            sum += time - previous;
            previous = time;
        }

        return (long) (sum / faultTimes.length);
    }

    public String getHostFailureTimesReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Host failure times\n");
        this.hostFailureTimes.forEach((host, failureTimes) -> {
            sb.append("  Host #").append(host.getId()).append(": ");
            failureTimes.forEach(failureTime -> sb.append(failureTime).append("\t"));
            sb.append("\n");
        });
        return sb.toString();
    }
}
