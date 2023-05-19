package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.container.app.PrintUtils
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.utils.IDs

class Gwo {

    int numberOfPacks = 0
    int wolvesPerPack = 0
    List<Pack> packs = []

    def initPacks(List<Firework> fireworks) {
        IDs.resetGwo()
        for (int i = 0; i < numberOfPacks; i++) {
            this.packs.add(new Pack(wolvesPerPack, fireworks.get(i)))
        }
    }


    def updateWolvesPositions(double a) {
        for (Pack pack : this.packs) {
            pack.updatePositions(a)
        }
    }

    def calculateFitness(Task taskToSchedule) {
        for (Pack pack : this.packs) {
            pack.wolves.each { it.calculateFitnessFunctionValue(taskToSchedule) }
        }

    }

    def rankWolves() {
        for (Pack pack : this.packs) {
            pack.rankWolves()
        }
    }

    String getIterationLogMessage(int iteration) {
        StringBuilder sb = new StringBuilder()
        GreyWolf currentBest = packs.collect { it.getWolfByRank(Rank.ALPHA) }.min { it.fitnessValue }
        sb.append(iteration).append(PrintUtils.COMMA)
        sb.append(currentBest.objectives["thresholdDistance"]).append(PrintUtils.COMMA)
        sb.append(currentBest.objectives["clusterBalance"]).append(PrintUtils.COMMA)
        sb.append(currentBest.objectives["systemFailureRate"]).append(PrintUtils.COMMA)
        sb.append(currentBest.objectives["totalNetworkDistance"]).append(PrintUtils.COMMA)
        sb.append(currentBest.objectives.values().sum()).append(PrintUtils.NEW_LINE)
    }

    GreyWolf getBestWolfFromALlPacks() {
        return packs.collect { it.getWolfByRank(Rank.ALPHA) }.min { it.fitnessValue }
    }
}
