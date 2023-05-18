package org.cloudbus.cloudsim.container.app.model.algo

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.UniformRandomGenerator
import org.cloudbus.cloudsim.container.app.model.ObjectiveFunction
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.utils.IDs
import org.cloudbus.cloudsim.util.MathUtil


class Firework {
    int id
    int position
    int lowerBound
    int upperBound
    double fitnessValue
    double amplitude
    List<Spark> sparks
    List<ContainerHost> hostsToSearch
    int numberOfSparks
    double normalizedAmplitude

    Firework(List<ContainerHost> hostsToSearch) {
        this.id = IDs.pollId(Firework.class)

        this.position = MathUtil.randomInt(upperBound)
        this.fitnessValue = -1
        this.hostsToSearch = hostsToSearch
        this.lowerBound = 0
        this.numberOfSparks = 0
        this.upperBound = hostsToSearch.size() - 1
        this.sparks = []
    }

    void calculateFitness(Task taskToSchedule) {
        ContainerHost allocationCandidateHost = hostsToSearch.get(position)
        double fitness = ObjectiveFunction.calculate(taskToSchedule, allocationCandidateHost).values().sum()
        this.fitnessValue = fitness
        calculateSparkFitness(taskToSchedule)
    }

    void calculateSparkFitness(Task taskToSchedule) {
        this.sparks.forEach(s -> s.calculateFitness(taskToSchedule))

    }

    List<Spark> generateSparks() {
        List<Integer> positions = []
        List<Spark> sparks = new ArrayList()//spark is denoted only by oisition which isindex in hostlist
        for (int i = 0; i < numberOfSparks; i++) {
            UniformRandomGenerator generator = new UniformRandomGenerator(new JDKRandomGenerator())
            int randomizedAmpl = (int) Math.round(generator.nextNormalizedDouble() * amplitude)
            int sparkPosition = Math.abs(position + randomizedAmpl) % (hostsToSearch.size() - 1)
            while (positions.contains(sparkPosition)) {
                randomizedAmpl = (int) Math.round(generator.nextNormalizedDouble() * amplitude)
                sparkPosition = Math.abs(sparkPosition + randomizedAmpl) % (hostsToSearch.size() - 1)
            }
            positions.add(sparkPosition)
            sparks.add(new Spark(sparkPosition, this))
        }
        this.sparks = sparks
        return sparks
    }

    List<Spark> getBestSparks(int count, Task task) {
        if (task != null) {
            this.sparks.forEach(s -> s.calculateFitness(task))
        }
        List<Spark> bestSparks = this.sparks.sort { it.fitnessValue }.take(count)
        return bestSparks
    }

    void setAmplitude(double amplitude) {
        this.amplitude = amplitude
    }

    void setAmplitude(double amplitude, double normalizedAmplitude) {
        this.normalizedAmplitude = normalizedAmplitude
        this.amplitude = amplitude
    }

    void setNumberOfSparks(int noOfSparks) {
        this.numberOfSparks = noOfSparks
    }
}