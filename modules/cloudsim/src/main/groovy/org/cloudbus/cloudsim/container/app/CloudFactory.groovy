package org.cloudbus.cloudsim.container.app


import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisionerSimple
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPeProvisionerSimple
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisionerSimple
import org.cloudbus.cloudsim.container.core.ContainerDatacenter
import org.cloudbus.cloudsim.container.core.ContainerDatacenterCharacteristics
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerHostAllocationPolicy
import org.cloudbus.cloudsim.container.schedulers.ContainerSchedulerTimeSharedOverSubscription
import org.cloudbus.cloudsim.vmplus.util.Id

class CloudFactory {
        public static int HOST_MIPS_PER_CPU = 100000
        public static int HOST_CPUS = 32
        public static int HOST_BW = 10000
        public static int HOST_RAM = 10000

    static List<ContainerHost> createHosts(int hostsCount) {
            List<ContainerHost> hosts = new ArrayList<>()

        for (int i = 0; i < hostsCount; i++) {
                List<ContainerPe> hostPes = createHostPes()
            ContainerHost host = new ContainerHost(
                        Id.pollId(ContainerHost.class),

                        new ContainerRamProvisionerSimple(HOST_RAM),
                        new ContainerBwProvisionerSimple(HOST_BW),
                        0,
                        hostPes,
                        new ContainerSchedulerTimeSharedOverSubscription(hostPes))
            hosts.add(host)
        }
            return hosts
    }

    static List<ContainerPe> createHostPes() {
            List<ContainerPe> pes = new ArrayList<>()
        for (int i = 0; i < HOST_CPUS; i++) {
                pes.add(new ContainerPe(i, new ContainerPeProvisionerSimple(HOST_MIPS_PER_CPU)))
        }
            return pes
    }

    static ContainerDatacenter createDatacenter(String name, List<ContainerHost> hosts) {
            ContainerDatacenterCharacteristics characteristics = new ContainerDatacenterCharacteristics("x86", "Linux", "Xen",
                    hosts, 0.0, 0, 0,
                    0, 0)
        return new ContainerDatacenter(name, characteristics, new ContainerHostAllocationPolicy())

    }

}
