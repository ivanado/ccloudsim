package org.cloudbus.cloudsim.container.app.algo;

import org.cloudbus.cloudsim.container.app.ObjectiveFunction;
import org.cloudbus.cloudsim.container.app.Task;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.utils.IDs;

public class Spark {
    public int id;
    public int position;
    public Firework firework;

    public double fitnessValue;

    public Spark(int position, Firework firework) {
        this.id = IDs.pollId(Spark.class);
        this.firework = firework;
        this.position = position;
        this.fitnessValue = -1;
    }

    public void calculateFitness(Task taskToSchedule){
        ContainerHost allocationCandidateHost = this.firework.hostsToSearch.get(position);
        this.fitnessValue= ObjectiveFunction.calculate(taskToSchedule, allocationCandidateHost);
    }

}
