package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.app.algo.fwgwo.FwGwo;
import org.cloudbus.cloudsim.container.core.ContainerHost;

import java.util.List;

public class FwGwoContainerPlacementPolicy extends ContainerPlacementPolicy{

    public ContainerHost getContainerHost(List<ContainerHost> hosts, Task task) {
        ContainerHost selectedHost = new FwGwo(5, hosts).selectHostForContainerAllocation(task);
        Log.printLine(getClass().getSimpleName(), ": Selected host #", selectedHost, " for running task ", task.toString());
        return selectedHost;
    }


}
