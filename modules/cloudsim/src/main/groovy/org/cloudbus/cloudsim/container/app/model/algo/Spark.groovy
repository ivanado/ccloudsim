package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.container.app.model.ObjectiveFunction
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.utils.IDs

class Spark {
    int id;
    int position;
    Firework firework;

    double fitnessValue;

    Spark(int position, Firework firework) {
        this.id = IDs.pollId(Spark.class);
        this.firework = firework;
        this.position = position;
        this.fitnessValue = -1;
    }

    void calculateFitness(Task taskToSchedule) {
        ContainerHost allocationCandidateHost = this.firework.hostsToSearch.get(position);
        this.fitnessValue = ObjectiveFunction.calculate(taskToSchedule, allocationCandidateHost);
    }
}
