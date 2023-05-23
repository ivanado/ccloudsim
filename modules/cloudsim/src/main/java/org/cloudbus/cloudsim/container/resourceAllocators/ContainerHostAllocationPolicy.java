package org.cloudbus.cloudsim.container.resourceAllocators;

import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.ContainerPlacementPolicy;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.FirstFitContainerPlacementPolicy;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.FwGwoContainerPlacementPolicy;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.lists.ContainerPeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerHostAllocationPolicy extends ContainerAllocationPolicy {
    public List<Integer> freePes;
    public Map<String, Integer> containerUsedPes;
    private final Map<String, ContainerHost> containerHostTable;

    private final ContainerPlacementPolicy placementPolicy;


    public ContainerHostAllocationPolicy() {
        this.freePes = new ArrayList<>();
        this.containerUsedPes = new HashMap<>();
        this.containerHostTable = new HashMap<>();
        this.placementPolicy = new FwGwoContainerPlacementPolicy();

    }

    @Override
    public boolean allocateHostForContainer(Container container, List<ContainerHost> containerHostList) {
        setContainerHostList(containerHostList);

        int requiredPes = container.getNumberOfPes();
        boolean result = false;

        if (!this.containerHostTable.containsKey(container.getUid())) {
            ContainerHost containerHost = placeContainerOnHostHost(container, containerHostList);
            result = containerHost != null && containerHost.containerCreate(container);

            if (result) {
                containerHostTable.put(container.getUid(), containerHost);
                containerUsedPes.put(container.getUid(), requiredPes);

                ContainerPeList.allocateFreePes(containerHost.getPeList(), requiredPes);
            }
        }

        return result;
    }


    private ContainerHost placeContainerOnHostHost(Container containerToAllocate, List<ContainerHost> containerHostList) {
        List<ContainerHost> availableHosts = containerHostList.stream().filter(containerHost -> containerHost.getNumberOfFreePes() >= containerToAllocate.getNumberOfPes()).toList();
        Task taskForContainer = DatacenterMetrics.get().getTask(containerToAllocate);
        return placementPolicy.getContainerHost(availableHosts, taskForContainer);
    }

    @Override
    public boolean allocateHostForContainer(Container container, ContainerHost containerHost) {
        boolean result = containerHost != null && containerHost.containerCreate(container);
        int requiredPes = container.getNumberOfPes();
        if (result) {
            containerHostTable.put(container.getUid(), containerHost);
            containerUsedPes.put(container.getUid(), requiredPes);
            ContainerPeList.allocateFreePes(containerHost.getPeList(), requiredPes);
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
            ContainerPeList.freeBusyPes(host.getPeList(), deallocatedPes);
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
}
