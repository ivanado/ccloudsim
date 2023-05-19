package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.container.app.model.ObjectiveFunction
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.utils.IDs

class GreyWolf {
    int id

    Rank rank

    ContainerHost currentHostCandidate = null

    int currentPosition = -1
    double fitnessValue = -1
    Map objectives

    GreyWolf() {
        this.id = IDs.pollId(GreyWolf.class)
        this.rank = Rank.OMEGA
    }

    def calculateFitnessFunctionValue(Task taskToSchedule) {
        //calculate all metrics for a microservice being deployed in a new container on the currentPositionHost
        this.objectives = ObjectiveFunction.calculate(taskToSchedule, currentHostCandidate)
        this.fitnessValue = this.objectives.values().sum() as Double
    }



    @Override
    String toString() {
        return "$rank wolf-$id [position:$currentPosition, hostCandidate: $currentHostCandidate, fitnessVal: $fitnessValue]"
    }
}
