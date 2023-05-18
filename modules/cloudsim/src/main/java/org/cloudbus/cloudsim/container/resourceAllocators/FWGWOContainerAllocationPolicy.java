package org.cloudbus.cloudsim.container.resourceAllocators;

import org.cloudbus.cloudsim.container.app.model.DatacenterResources;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.FwGwoHostSelectionPolicy;
import org.cloudbus.cloudsim.container.lists.ContainerPeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FWGWOContainerAllocationPolicy extends ContainerAllocationPolicy {
    public List<Integer> freePes;
    public Map<String, Integer> containerUsedPes;
    private Map<String, ContainerHost> containerHostTable;

    private FwGwoHostSelectionPolicy selectionPolicy;


    public FWGWOContainerAllocationPolicy() {
        this.freePes = new ArrayList<>();
        this.containerUsedPes = new HashMap<>();
        this.containerHostTable = new HashMap<>();
        this.selectionPolicy = new FwGwoHostSelectionPolicy();

    }

    @Override
    public boolean allocateHostForContainer(Container container, List<ContainerHost> containerHostList) {
        setContainerHostList(containerHostList);

        int requiredPes = container.getNumberOfPes();
        boolean result = false;

        if (!this.containerHostTable.containsKey(container.getUid())) {
            ContainerHost containerHost = getHost(container, containerHostList);
            result = containerHost != null && containerHost.containerCreate(container);

            if (result) {
                containerHostTable.put(container.getUid(), containerHost);
                containerUsedPes.put(container.getUid(), requiredPes);

                ContainerPeList.allocateFreePes(containerHost.getPeList(), requiredPes);
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

    private ContainerHost getHost(Container containerToAllocate, List<ContainerHost> containerHostList) {
        List<ContainerHost> availableHosts = containerHostList.stream().filter(containerHost -> containerHost.getNumberOfFreePes() >= containerToAllocate.getNumberOfPes()).toList();
        Task taskForContainer = DatacenterResources.get().getTask(containerToAllocate);
        return selectionPolicy.selectHost(availableHosts, taskForContainer);
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

    @Override
    public int getFreePesForHost(int hostId) {
        return 0;//hostFreePes.get(hostId);
    }
}
