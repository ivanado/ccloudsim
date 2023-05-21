package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.core.ContainerHost;

import java.util.List;

public abstract class ContainerPlacementPolicy {
    public abstract ContainerHost getContainerHost(List<ContainerHost> hosts, Task task);
}
