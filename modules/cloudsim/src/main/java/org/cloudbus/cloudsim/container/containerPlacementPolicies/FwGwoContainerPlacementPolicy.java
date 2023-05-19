package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.app.model.algo.FwGwo;
import org.cloudbus.cloudsim.container.core.ContainerHost;

import java.util.List;

public class FwGwoContainerPlacementPolicy {

    public ContainerHost getContainerHost(List<ContainerHost> hosts, Task task) {
        ContainerHost selectedHost = new FwGwo(1, hosts).selectHostForContainerAllocation(task);
        Log.printLine(getClass().getSimpleName(), ": Selected host #", selectedHost.toString(), " for running task ", task.toString());
        return selectedHost;
    }


}
