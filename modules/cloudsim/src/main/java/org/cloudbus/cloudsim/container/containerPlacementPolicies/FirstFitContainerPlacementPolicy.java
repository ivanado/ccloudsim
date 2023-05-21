package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics;
import org.cloudbus.cloudsim.container.app.model.ObjectiveFunction;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.core.ContainerHost;

import java.util.List;

public class FirstFitContainerPlacementPolicy extends ContainerPlacementPolicy {

    public ContainerHost getContainerHost(List<ContainerHost> hosts, Task task) {
        if( hosts.isEmpty()){
            return null;
        }
        ContainerHost selectedHost =hosts.get(0);
        Log.printLine(getClass().getSimpleName(), ": Selected host #", selectedHost, " for running task ", task.toString());
        DatacenterMetrics.get().addObjectiveFunctionValueForTask(ObjectiveFunction.calculate(task, selectedHost), task);

        return selectedHost;
    }
}

