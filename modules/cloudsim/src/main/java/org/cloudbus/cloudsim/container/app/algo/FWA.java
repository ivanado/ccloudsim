package org.cloudbus.cloudsim.container.app.algo;

import org.cloudbus.cloudsim.container.app.Task;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.util.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FWA {
    List<Firework> fireworks;
    Map<Firework, Double> fireworkFitness;
    double bestFitness;
    double worstFitness;

    double maxSparks;
    private Map<Firework, List<Spark>> fireworkSparks;

    public FWA(int numberOfPacks, List<ContainerHost> allHosts) {
        this.fireworks = new ArrayList<>();
        this.fireworkSparks = new HashMap<>();
        int hostsPerFirework = allHosts.size() / numberOfPacks;
        maxSparks = hostsPerFirework / 4D;
        for (int i = 0; i < numberOfPacks; i++) {
            int toIdx = i == numberOfPacks - 1
                    ? allHosts.size() - 1
                    : (i + 1) * hostsPerFirework - 1;
            List<ContainerHost> hosts = allHosts.subList(i * hostsPerFirework, toIdx);
            fireworks.add(new Firework(hosts));
        }
    }

    public void run(Task task) {
        fireworks.forEach(f -> {
            f.calculateFitness(task);
            fireworkFitness.put(f, f.fitnessValue);
        });
        this.worstFitness = fireworks.stream().mapToDouble(f -> f.fitnessValue).max().orElse(0);
        this.bestFitness = fireworks.stream().mapToDouble(f -> f.fitnessValue).min().orElse(0);
        fireworks.forEach(f -> {
            calculateNumberOfSparks(f);
            calculateAmplitude(f);
            f.generateSparks();
            f.calculateFitness(task);
            List<Spark> bestSparks = f.getBestSparks(4);
            this.fireworkSparks.put(f, bestSparks);
        });
    }

    private double calculateAmplitude(Firework firework) {
        if (fireworks.size() == 1) {
            firework.setAmplitude(0.5);
            return 0.5;
        }
        if (fireworks.size() == 2) {
            firework.setAmplitude(firework.id % 2 == 0 ? 1 : 2);
            return firework.id % 2 == 0 ? 1 : 2;
        } else {
            double maximumExplosionAmplitude = MathUtil.min(firework.position, firework.upperBound - firework.position);

            double amplitude = maximumExplosionAmplitude * (firework.fitnessValue - bestFitness + Double.MIN_VALUE) / (fireworks.stream().mapToDouble(f -> f.fitnessValue - bestFitness).sum() + Double.MIN_VALUE);
            firework.setAmplitude(amplitude);
            return amplitude;
        }
    }

    private void calculateNumberOfSparks(Firework firework) {
        int noOfSparks = (int) Math.round(maxSparks * (worstFitness - firework.fitnessValue + Double.MIN_VALUE) / (fireworks.stream().mapToDouble(f -> worstFitness - f.fitnessValue).sum() + Double.MIN_VALUE));
        firework.setNumberOfSparks(noOfSparks);

    }
}
