package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.utils.IDs

class FwaGwo {
    public static final String COMMA = ","
    List<Firework> fireworks
    double bestFitness
    double worstFitness

    double maxSparks
    private double maxAmplitude

    List<Pack> packs
    int wolvesPerPack = 4

    FwaGwo(int numberOfPacks, List<ContainerHost> allHosts) {
        this.maxAmplitude = 0
        initFireworks(numberOfPacks, allHosts)
        initPacks(numberOfPacks, wolvesPerPack, allHosts)

        this.packs.eachWithIndex { Pack p, int i -> p.firework = this.fireworks.get(i) }

    }

    private void initPacks(int numberOfPacks, int wolvesPerPack, List<ContainerHost> allHosts) {
        this.packs = new ArrayList<>()
        IDs.resetGwo()
        for (int i = 0; i < numberOfPacks; i++) {
            this.packs.add(new Pack(wolvesPerPack, fireworks.get(i)))
        }
    }

    private void initFireworks(int numberOfPacks, List<ContainerHost> allHosts) {
        this.fireworks = new ArrayList<>()
        int hostsPerFirework = allHosts.size() / numberOfPacks
        maxSparks = hostsPerFirework / 4D
        for (int i = 0; i < numberOfPacks; i++) {
            int toIdx = i == numberOfPacks - 1
                    ? allHosts.size()
                    : (i + 1) * hostsPerFirework
            List<ContainerHost> hosts = allHosts.subList(i * hostsPerFirework, toIdx)
            fireworks.add(new Firework(hosts))
        }


    }

    ContainerHost run(Task task) {
        findBestSparks(task)
        packs.forEach(pack -> {
            pack.initialRankAndPositionWolves()
        })
        StringBuilder sb = new StringBuilder() //output objective function value per iteration
        sb.append("iteration,thresholdDistance,clusterBalance,systemFailureRate,totalNetworkDistance,objectiveFunction\n")
        int maxIterations = 10
        int i = 0
        double a
        while (i < maxIterations) {
            a = 2 - (double) i / maxIterations
            packs.forEach(p -> p.updatePositions(a))
            packs.forEach(p -> p.calculateFitness(task))
            packs.forEach(p -> p.rankWolves())
            GreyWolf currentBest = packs.collect { it.getWolfByRank(Rank.ALPHA) }.min { it.fitnessValue }
            sb.append(i).append(COMMA)
            sb.append(currentBest.objectives["thresholdDistance"]).append(COMMA)
            sb.append(currentBest.objectives["clusterBalance"]).append(COMMA)
            sb.append(currentBest.objectives["systemFailureRate"]).append(COMMA)
            sb.append(currentBest.objectives["totalNetworkDistance"]).append(COMMA)
            sb.append(currentBest.objectives.values().sum()).append("\n")
            i++

        }
        File outputLog = new File("modules/cloudsim/build/FWGWO-iterations.log")
        outputLog.text = sb.toString()

        GreyWolf best = packs.collect { it.getWolfByRank(Rank.ALPHA) }.min { it.fitnessValue }
        DatacenterMetrics.get().setBestObjectiveFunctionValues(best.objectives)
        return packs.collect { it.getWolfByRank(Rank.ALPHA) }.min { it.fitnessValue }.currentHostCandidate
    }

    private void findBestSparks(Task task) {
        fireworks.forEach(f -> {
            f.calculateFitnessValues(task)
//            fireworkFitness.put(f, f.fitnessValue);
        })
//        this.worstFitness = fireworks.stream().mapToDouble(f -> f.fwFitnessValue).max().orElse(0)
        this.bestFitness = fireworks.collect{f -> f.fwFitnessValue}.min()
        fireworks.forEach(f -> {
            calculateNumberOfSparks(f)
            calculateAmplitude(f)
            f.generateSparks()
            f.calculateFitnessValues(task)
            List<Spark> bestSparks = f.selectBestSparks(wolvesPerPack, task)
        })
    }

    private double calculateAmplitude(Firework firework) {
        if (fireworks.size() == 1) {
            firework.setAmplitude(0.5)
            maxAmplitude = 0.5
            return 0.5
        }
        if (fireworks.size() == 2) {
            double amplitude = firework.id % 2 == 0 ? 1 : 2
            firework.setAmplitude(amplitude)
            maxAmplitude = maxAmplitude < amplitude ? amplitude : maxAmplitude
            return firework.id % 2 == 0 ? 1 : 2
        } else {
            double maximumExplosionAmplitude = firework.upperBound

            double amplitude = maximumExplosionAmplitude * (firework.fwFitnessValue - bestFitness + Double.MIN_VALUE) / (fireworks.stream().mapToDouble(f -> f.fwFitnessValue - bestFitness).sum() + Double.MIN_VALUE)
            maxAmplitude = maxAmplitude < amplitude ? amplitude : maxAmplitude
            double normalizedAmplitude = 2 * amplitude / maxAmplitude
            firework.setAmplitude(amplitude, normalizedAmplitude)
            return amplitude
        }
    }

    private void calculateNumberOfSparks(Firework firework) {
        int noOfSparks = (int) Math.round(maxSparks * (worstFitness - firework.fwFitnessValue + Double.MIN_VALUE) / (fireworks.stream().mapToDouble(f -> worstFitness - f.fwFitnessValue).sum() + Double.MIN_VALUE))
        firework.setNumberOfSparks(wolvesPerPack + 2)

    }
}
