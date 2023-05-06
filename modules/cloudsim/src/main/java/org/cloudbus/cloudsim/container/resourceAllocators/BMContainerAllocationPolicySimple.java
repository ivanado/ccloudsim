package org.cloudbus.cloudsim.container.resourceAllocators;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BMContainerAllocationPolicySimple extends ContainerAllocationPolicy {
   public List<Integer> freePes;
   public Map<String, Integer> containerUsedPes;
    public Map<Integer, Integer> hostFreePes;
    private Map<String, ContainerHost> containerHostTable;


    public BMContainerAllocationPolicySimple(List<ContainerHost> hosts) {
        this.freePes = new ArrayList<>();
        this.hostFreePes = new HashMap<>();
        this.containerUsedPes = new HashMap<>();
        this.containerHostTable = new HashMap<>();
        for (ContainerHost containerHost : hosts) {
            hostFreePes.put(containerHost.getId(), containerHost.getNumberOfFreePes());

        }
    }

    @Override
    public boolean allocateHostForContainer(Container container, List<ContainerHost> containerHostList) {
        setContainerHostList(containerHostList);

        int requiredPes = container.getNumberOfPes();
        boolean result = false;

        if (!this.containerHostTable.containsKey(container.getUid())) {
            ContainerHost containerHost = getHost(requiredPes, containerHostList);
            result = containerHost != null && containerHost.containerCreate(container);

            if (result) {
                containerHostTable.put(container.getUid(), containerHost);
                containerUsedPes.put(container.getUid(), requiredPes);
                hostFreePes.put(containerHost.getId(), hostFreePes.get(containerHost.getId()) - requiredPes);
                result = true;
            }
        }

        return result;
    }

    private ContainerHost chooseHostForContainer(int requiredPes, List<ContainerHost> containerHostList) {
        //----------->TODO plug in FWGWO here to choose the optimal host

//       TODO host free pes never gets updated on container pe allocation (numberOfFreePes always max)
        return containerHostList
                .stream()
                .filter(host -> host.getNumberOfFreePes() >= requiredPes)
                .findFirst().orElse(null);
    }

    private ContainerHost getHost(int requiredPes, List<ContainerHost> containerHostList){
        return containerHostList.stream().filter(containerHost -> hostFreePes.get(containerHost.getId()) >= requiredPes).findFirst().orElse(null);
    }

    @Override
    public boolean allocateHostForContainer(Container container, ContainerHost containerHost) {
        boolean result = containerHost != null && containerHost.containerCreate(container);
        int requiredPes = container.getNumberOfPes();
        if (result) {
            containerHostTable.put(container.getUid(), containerHost);
            containerUsedPes.put(container.getUid(), requiredPes);
            hostFreePes.put(containerHost.getId(), hostFreePes.get(containerHost.getId()) - requiredPes);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Container> containerList) {
        return null;
    }

    @Override
    public void deallocateContainerFromHost(Container container) {
        ContainerHost host = containerHostTable.remove(container.getUid());
        int deallocatedPes = containerUsedPes.remove(container.getUid());
        if (host != null) {
            host.containerDestroy(container);
            hostFreePes.put(host.getId(), hostFreePes.get(host.getId()) + deallocatedPes);
        }
    }

    @Override
    public ContainerHost getContainerHost(Container container) {
        return containerHostTable.get(container.getUid());
    }

    @Override
    public ContainerHost getContainerHost(int containerId, int userId) {
        return containerHostTable.get(Container.getUid(userId, containerId));
    }

    @Override
    public int getFreePesForHost(int hostId) {
        return hostFreePes.get(hostId);
    }
}
