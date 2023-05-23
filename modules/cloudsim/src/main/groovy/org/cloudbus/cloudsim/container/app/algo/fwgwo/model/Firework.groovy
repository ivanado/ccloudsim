package org.cloudbus.cloudsim.container.app.algo.fwgwo.model

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
    double fwFitnessValue
    double amplitude
    List<Spark> sparks = []
    List<ContainerHost> hostsToSearch
    int numberOfSparks
    double normalizedAmplitude = 0
    List<Integer> exploredPositions = []
    List<Spark> bestSparks = []


    Firework(List<ContainerHost> hostsToSelect, int maxFwSparkCount) {
        this.id = IDs.pollId(Firework.class)
        this.upperBound = hostsToSelect.size() - 1
        this.lowerBound = 0
        this.position = MathUtil.randomInt(upperBound)
        this.hostsToSearch = hostsToSelect
        this.fwFitnessValue = -1
        this.numberOfSparks = maxFwSparkCount
        this.amplitude = 0
        this.normalizedAmplitude = 0
    }


    List<Spark> getAllSparks() {
        def l = new ArrayList(sparks)
        l.add(new FwSpark(this))
        return l
    }

    void calculateFitnessValues(Task taskToSchedule) {
        ContainerHost allocationCandidateHost = hostsToSearch.get(position)
        double fitness = ObjectiveFunction.calculate(taskToSchedule, allocationCandidateHost).values().sum() as Double
        this.fwFitnessValue = fitness
        calculateSparkFitness(taskToSchedule)
    }

    void calculateSparkFitness(Task taskToSchedule) {
        this.sparks.forEach(s -> s.calculateFitness(taskToSchedule))

    }

    void generateSparks() {
        List<Integer> positions = []
        def hostMaxIndex = hostsToSearch.size() - 1  //hosts with resurces available for cl,oudlet processing
        List<Spark> sparks = new ArrayList()//spark is denoted only by oisition which isindex in hostlist
        for (int i = 0; (hostMaxIndex < numberOfSparks && i < hostMaxIndex) || (hostMaxIndex >= numberOfSparks && i < numberOfSparks); i++) {
            UniformRandomGenerator generator = new UniformRandomGenerator(new JDKRandomGenerator())
            int randomizedAmpl = (int) Math.round(generator.nextNormalizedDouble() * amplitude)

            int sparkPosition =
                    hostMaxIndex > 0
                            ? Math.abs(position + randomizedAmpl) % hostMaxIndex
                            : 0

//            while (positions.contains(sparkPosition)) {
//                randomizedAmpl = (int) Math.round(generator.nextNormalizedDouble() * amplitude)
//                sparkPosition = Math.abs(sparkPosition + randomizedAmpl) % (hostsToSearch.size() - 1)
//            }
            sparks.add(new Spark(sparkPosition, this))
        }
        this.sparks = sparks
    }

    List<Spark> selectBestSparks(Task taskToSchedule, int count) {
        calculateFitnessValues(taskToSchedule)
        bestSparks = getAllSparks().sort { it.fitnessValue }.take(count)
        return bestSparks

    }

//
//    private void findBestSparks(Task task) {
//        fireworks.forEach(f -> {
//            f.calculateFitnessValues(task)
////            fireworkFitness.put(f, f.fitnessValue);
//        })
//        def fitnessValues = this.collect { [it.fwFitnessValue].addAll(it.sparks*.fitnessValue) }
//        this.worstFitness = fitnessValues.max()
//        this.bestFitness = fitnessValues.min()
//            calculateAmplitudes(f)
//            f.generateSparks()
//            f.calculateFitnessValues(task)
//            int wolvesPerPack =4
//            List<Spark> bestSparks = f.getBestSparks(wolvesPerPack, task)
//        })
//    }
//    List<Spark> getBestSparks(int sparkCount, Task task) {
//        this.sparks.forEach(s -> s.calculateFitness(task))
//
//        List<Spark> bestSparks = this.sparks.sort { it.fitnessValue }.take(sparkCount)
//        return bestSparks
//    }

    void setAmplitude(double amplitude) {
        this.amplitude = amplitude
        this.normalizedAmplitude = amplitude
    }

    void setAmplitude(double amplitude, double normalizedAmplitude) {
        this.normalizedAmplitude = normalizedAmplitude
        this.amplitude = amplitude
    }

    void setNumberOfSparks(int noOfSparks) {
        this.numberOfSparks = noOfSparks
    }

    @Override
    boolean equals(Object obj) {
        Firework other = (Firework) obj
        return other.id == id
    }
}