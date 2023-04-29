package org.cloudbus.cloudsim.container.core.bm;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BMContainerAllocationPolicySimple extends ContainerAllocationPolicy {
    List<Integer> freePes;
    Map<String,Integer> containerUsedPes;
    Map<Integer,Integer> hostFreePes;
    protected List<Integer> getFreePes() {
        return freePes;
    }
    /** The host table. */
    private Map<String, ContainerHost> containerHostTable;




    public BMContainerAllocationPolicySimple(){
        this.freePes=new ArrayList<>();
        this.hostFreePes =new HashMap<>();
        this.containerUsedPes=new HashMap<>();
        this.containerHostTable=new HashMap<>();
    }
    @Override
    public boolean allocateHostForContainer(Container container, List<ContainerHost> containerHostList) {
        //		the available container list is updated. It gets is from the data center.
        setContainerHostList(containerHostList);
        for (ContainerHost containerHost : getContainerHostList()) {
            hostFreePes.put(containerHost.getId(), containerHost.getNumberOfFreePes());
//            getFreePes().add(containerHost.getNumberOfPes());

        }
        int requiredPes = container.getNumberOfPes();
        boolean result = false;

        if (!this.containerHostTable.containsKey(container.getUid())) { // if this vm was not created
//            do {// we still trying until we find a host or until we try all of them
//                int moreFree = Integer.MIN_VALUE;
//                int idx = -1;
//
//                // we want the host with less pes in use
//                for (int i = 0; i < freePesTmp.size(); i++) {
//                    if (freePesTmp.get(i) > moreFree) {
//                        moreFree = freePesTmp.get(i);
//                        idx = i;
//                    }
//                }
//
//                ContainerHost containerHost = getContainerHostList().get(idx);
            ContainerHost containerHost = chooseHostForContainer(requiredPes,containerHostList );
                result =  containerHost != null && containerHost.containerCreate(container);

                if (result) { // if vm were succesfully created in the host
                    containerHostTable.put(container.getUid(), containerHost);
                    containerUsedPes.put(container.getUid(), requiredPes);
                    hostFreePes.put(containerHost.getId(), hostFreePes.get(containerHost.getId())-requiredPes);
//                    getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
                    result = true;
                }

        }


        return result;

    }

    private ContainerHost chooseHostForContainer(int requiredPes, List<ContainerHost> containerHostList) {
        //----------->TODO plug in FWGWO here to choose the optimal host
        return containerHostList.stream().filter(host-> host.getNumberOfFreePes() >=requiredPes).findFirst().orElse(null);
    }

    @Override
    public boolean allocateHostForContainer(Container container, ContainerHost containerHost) {
      boolean  result =  containerHost != null && containerHost.containerCreate(container);
        int requiredPes = container.getNumberOfPes();
        if (result) { // if vm were succesfully created in the host
            containerHostTable.put(container.getUid(), containerHost);
            containerUsedPes.put(container.getUid(), requiredPes);
            hostFreePes.put(containerHost.getId(), hostFreePes.get(container.getUid())-requiredPes);
//                    getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
            result = true;
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
//        ContainerVm contain erVm = getContainerVmTable().remove(container.getUid());
//        int idx = getContainerVmList().indexOf(containerVm);
//        int pes = getUsedPes().remove(container.getUid());
        int deallocatedPes=containerUsedPes.remove(container.getUid());
        if (host != null) {
            host.containerDestroy(container);
//            getFreePes().set(idx, getFreePes().get(idx) + pes);
            hostFreePes.put(host.getId(), hostFreePes.get(host.getId())+deallocatedPes);
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
