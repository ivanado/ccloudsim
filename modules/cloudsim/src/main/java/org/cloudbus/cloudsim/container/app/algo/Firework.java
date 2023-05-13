package org.cloudbus.cloudsim.container.app.algo;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.UniformRandomGenerator;
import org.cloudbus.cloudsim.container.app.ObjectiveFunction;
import org.cloudbus.cloudsim.container.app.Task;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Firework {
    public int id;
    public int position;
    public int lowerBound;
    public int upperBound;
    public double fitnessValue;
    private double amplitude;
    private List<Spark> sparks;
    public List<ContainerHost> hostsToSearch;
    private int numberOfSparks;
    private double normalizedAmplitude;

    public Firework(List<ContainerHost> hostsToSearch) {
        this.id = IDs.pollId(Firework.class);

        this.position = MathUtil.randomInt(upperBound);
        this.fitnessValue = -1;
        this.hostsToSearch = hostsToSearch;
        this.lowerBound = 0;
        this.numberOfSparks = 0;
        this.upperBound = hostsToSearch.size() - 1;
    }

    public void calculateFitness(Task taskToSchedule) {
        ContainerHost allocationCandidateHost = hostsToSearch.get(position);
        double fitness = ObjectiveFunction.calculate(taskToSchedule, allocationCandidateHost);
        this.fitnessValue = fitness;
        calculateSparkFitness(taskToSchedule);
    }

    public void calculateSparkFitness(Task taskToSchedule) {
        this.sparks.forEach(s -> s.calculateFitness(taskToSchedule));

    }

    public List<Spark> generateSparks() {
        List<Spark> sparks = new ArrayList();//spark is denoted only by oisition which isindex in hostlist
        for (int i = 0; i < numberOfSparks; i++) {
            UniformRandomGenerator generator = new UniformRandomGenerator(new JDKRandomGenerator());
            int randomizedAmpl = (int) Math.round(generator.nextNormalizedDouble() * amplitude);
            int sparkPosition = position + randomizedAmpl;
            sparks.add(new Spark(sparkPosition, this));
        }
        this.sparks = sparks;
        return sparks;
    }

    public List<Spark> getBestSparks(int count) {
        List<Spark> bestSparks = this.sparks.stream().sorted((s1, s2) -> (int) (s2.fitnessValue - s1.fitnessValue)).collect(Collectors.toList()).subList(0, count);

        return bestSparks;
    }

    public void setAmplitude(double amplitude) {
        this.amplitude = amplitude;
    }
    public void setAmplitude(double amplitude, double normalizedAmplitude) {
        this.normalizedAmplitude=normalizedAmplitude;
        this.amplitude = amplitude;
    }

    public void setNumberOfSparks(int noOfSparks) {
        this.numberOfSparks = noOfSparks;
    }
}
