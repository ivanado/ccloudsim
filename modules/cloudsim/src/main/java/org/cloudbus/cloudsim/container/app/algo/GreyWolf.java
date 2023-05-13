package org.cloudbus.cloudsim.container.app.algo;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.container.app.ObjectiveFunction;
import org.cloudbus.cloudsim.container.app.Task;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.utils.IDs;

public class GreyWolf {
    int id;
    @Getter
    @Setter
    int rank;
    @Getter
    @Setter
    ContainerHost currentPosition;

    int currentPositionIndex;

    public GreyWolf() {
        this.id = IDs.pollId(GreyWolf.class);
        this.rank = -1;
        this.currentPositionIndex = -1;
        this.currentPosition = null;
    }

    public double getFitnessFunctionValue(Task taskToSchedule) {
        //calculate all metrics for a microservice being deployed in a new container on the currentPositionHost
        return ObjectiveFunction.calculate(taskToSchedule, currentPosition);
    }
}
