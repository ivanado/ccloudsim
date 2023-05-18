package org.cloudbus.cloudsim.container.hostSelectionPolicies;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.app.model.algo.FwaGwo;
import org.cloudbus.cloudsim.container.core.ContainerHost;

import java.util.List;

public class FwGwoHostSelectionPolicy {

    public ContainerHost selectHost(List<ContainerHost> hosts, Task task){
        ContainerHost selectedHost= new FwaGwo(1,hosts).run(task);
        Log.printLine("Selected host # ", selectedHost.toString());
        return selectedHost;
    }


}
