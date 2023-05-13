package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.container.app.model.ObjectiveFunction
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.utils.IDs

class GreyWolf {
    int id = IDs.pollId(GreyWolf.class);

    Rank rank = Rank.OMEGA;

    ContainerHost currentPosition = null;

    int currentPositionIndex = -1;
    double fitnessValue = -1;


    def calculateFitnessFunctionValue(Task taskToSchedule) {
        //calculate all metrics for a microservice being deployed in a new container on the currentPositionHost
        this.fitnessValue = ObjectiveFunction.calculate(taskToSchedule, currentPosition);
    }
}
