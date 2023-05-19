package org.cloudbus.cloudsim.container.app.model.algo


import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.utils.IDs

import static org.cloudbus.cloudsim.vmplus.util.TextUtil.COMMA
import static org.cloudbus.cloudsim.vmplus.util.TextUtil.NEW_LINE

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
        sb.append(iteration).append(COMMA)
        sb.append(currentBest.objectives["thresholdDistance"]).append(COMMA)
        sb.append(currentBest.objectives["clusterBalance"]).append(COMMA)
        sb.append(currentBest.objectives["systemFailureRate"]).append(COMMA)
        sb.append(currentBest.objectives["totalNetworkDistance"]).append(COMMA)
        sb.append(currentBest.objectives.values().sum()).append(NEW_LINE)
    }

    GreyWolf getBestWolfFromALlPacks() {
        return packs.collect { it.getWolfByRank(Rank.ALPHA) }.min { it.fitnessValue }
    }
}
