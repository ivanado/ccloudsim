package org.cloudbus.cloudsim.container.app.algo;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.container.app.Task;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.utils.IDs;

public class GreyWolf {
    int id;
    int rank;
    @Getter
    @Setter
    ContainerHost currentPosition;

    public GreyWolf(int rank) {
        this.id = IDs.pollId(GreyWolf.class);
        this.rank = rank;
        this.currentPosition = null;
    }

    public double getFitnessFunctionValue(Task taskToSchedule) {
        //calculate all metrics for a microservice being deployed in a new container on the currentPositionHost

        return 0;

    }
}
