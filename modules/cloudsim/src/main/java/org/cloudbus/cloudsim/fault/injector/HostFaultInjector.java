package org.cloudbus.cloudsim.fault.injector;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPe;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.vmplus.util.CustomLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;

import static org.cloudbus.cloudsim.fault.injector.HostFaultCloudSimTags.HOST_FAILURE;

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

    @Getter
    @Setter
    private double maxTimeToFail;

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
        final Predicate<SimEvent> otherEventsPredicate = evt -> evt.getTag() != HOST_FAILURE;
        if (CloudSim.clock() < getMaxTimeToFail() || CloudSim.isFutureEventQueued(otherEventsPredicate)) {
            schedule(getId(), getNextFaultDelay(), HOST_FAILURE);
        }
    }

    private double getNextFaultDelay() {
        return Math.random() * 3600;
    }


    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == HOST_FAILURE) {
            generateHostFaultAndScheduleNext();
        }
    }

    private void generateHostFaultAndScheduleNext() {
        try {
            final ContainerHost host = getRandomHostContainingVMs();
            injectHostFault(host);
        } finally {
            scheduleHostFault();
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

        this.lastFailedHost = host;

        totalHostFaults++;
        hostFailureTimes.computeIfAbsent(lastFailedHost, h -> new ArrayList<>()).add(CloudSim.clock());

        this.lastFailedHost.getPeList().stream().map(ContainerVmPe::getId).forEach(id ->
                this.lastFailedHost.setPeStatus(id, Pe.FAILED)
        );

        failAllHostVms();
    }

    private void failAllHostVms() {
        final int vms = lastFailedHost.getVmList().size();
        final String msg = vms > 0 ? "affecting all its %d VMs".formatted(vms) : "but there was no running VM";
        CustomLog.logError(Level.SEVERE,
                String.format("%f: %s: All %d PEs from host #%s failed at %f, %s.",
                        CloudSim.clock(), getClass().getSimpleName(),
                        lastFailedHost.getNumberOfPes(), lastFailedHost.getId(), CloudSim.clock(), msg), null);


        lastFailedHost.getVmList().forEach(vm -> {
            Log.printConcatLine(CloudSim.clock(), ": ", getClass().getSimpleName(),
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
