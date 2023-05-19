package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost

class Fwa {
    List<Firework> fireworks = []
    double bestFitness

    double maxAmplitude = 0
    int numberOfFireworks = 0
    List<ContainerHost> allHosts = []
    int maxPerFwSparkCount = 0
    Map<Firework, List<Spark>> bestSparksPerFirework = [:]

    void initFireworks() {
        this.maxAmplitude = 0
        int hostsPerFirework = allHosts.size() / numberOfFireworks


        for (int i = 0; i < numberOfFireworks; i++) {
            int toIdx = i == numberOfFireworks - 1
                    ? allHosts.size()
                    : (i + 1) * hostsPerFirework

            List<ContainerHost> hosts = allHosts.subList(i * hostsPerFirework, toIdx)
            fireworks.add(new Firework(hosts, maxPerFwSparkCount))
        }
    }

    void calculateAmplitudes(Task taskToSchedule) {
        for (Firework firework : fireworks) {
            firework.calculateFitnessValues(taskToSchedule)
            double maximumExplosionAmplitude = firework.upperBound

            double amplitude = maximumExplosionAmplitude * (firework.fwFitnessValue - bestFitness + Double.MIN_VALUE) / (fireworks.collect{f -> f.fwFitnessValue - bestFitness}.sum() + Double.MIN_VALUE)
            maxAmplitude = maxAmplitude < amplitude ? amplitude : maxAmplitude
            double normalizedAmplitude = 2 * amplitude / maxAmplitude
            firework.setAmplitude(amplitude, normalizedAmplitude)
        }

    }


    void findBestSparks(Task taskToSchedule, int sparkCount) {
        bestSparksPerFirework = fireworks.collectEntries { f -> [(f): f.selectBestSparks(taskToSchedule, sparkCount)] }

    }

     void generateSparks() {
         for (Firework firework : fireworks) {
             firework.generateSparks()
         }
     }
}
