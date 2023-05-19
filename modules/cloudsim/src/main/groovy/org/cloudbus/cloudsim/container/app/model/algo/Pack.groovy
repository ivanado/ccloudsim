package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.Log
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.utils.IDs

class Pack {
    Firework firework
    int id
    int noOfWolves
    List<GreyWolf> wolves
    List<ContainerHost> hostsToSearch //subset of all hosts
    private double r1
    private double r2

    private double distance

    GreyWolf getWolfByRank(Rank rank) {
        this.wolves.find { it.rank == rank }
    }

    Pack(int noOfWolves, Firework firework) {
        this.id = IDs.pollId(Pack.class)
        this.distance = -1
        this.noOfWolves = noOfWolves
        this.hostsToSearch = firework.hostsToSearch
        this.firework = firework
        this.wolves = new ArrayList<>()

        this.r1 = Math.random().round()
        this.r2 = Math.random()

        createWolvesWithInitialPositionAndRank()
        Log.printLine("Pack #", id, " created with ", noOfWolves, " wolves")
    }

    void createWolvesWithInitialPositionAndRank() {
        for (int i = 0; i < noOfWolves; i++) {
            Spark s = this.firework.bestSparks.get(i)
            GreyWolf gw = new GreyWolf(currentPosition: s.position, currentHostCandidate: hostsToSearch.get(s.position), rank: Rank.getRank(i + 1), fitnessValue: s.fitnessValue)
            this.wolves.add(gw)
        }
    }

    void updatePositions(double a) {

        double amplitude = this.firework.normalizedAmplitude
        double E = 2 * a * r1
        double C = amplitude * r2
        for (GreyWolf currentWolf : this.wolves) {
            int newPosition = calculateNewPosition(C, currentWolf, E)

            int oldPosition = currentWolf.currentPosition
            int normalizedNewPosition = Math.abs(newPosition % (hostsToSearch.size() - 1))

            currentWolf.currentPosition = normalizedNewPosition
            currentWolf.currentHostCandidate = hostsToSearch.get(currentWolf.currentPosition)
//            Log.printLine("Pack#$id ", "\tupdatePosition for gw#$currentWolf.id from $oldPosition to $currentWolf.currentPosition", "\t (hostCandidate = $currentWolf.currentHostCandidate)")

        }
//        Log.print(NEW_LINE)

    }

    private int calculateNewPosition(double C, GreyWolf currentWolf, double E) {
        double distanceAlpha = C * getWolfByRank(Rank.ALPHA).currentPosition - currentWolf.currentPosition
        double distanceBeta = C * getWolfByRank(Rank.BETA).currentPosition - currentWolf.currentPosition
        double distanceDelta = C * getWolfByRank(Rank.DELTA).currentPosition - currentWolf.currentPosition
        double X1 = getWolfByRank(Rank.ALPHA).currentPosition - E * distanceAlpha
        double X2 = getWolfByRank(Rank.BETA).currentPosition - E * distanceBeta
        double X3 = getWolfByRank(Rank.DELTA).currentPosition - E * distanceDelta
        int newPosition = (int) (X1 + X2 + X3) / 3
        newPosition
    }

    void calculateFitness(Task taskToSchedule) {
        for (GreyWolf greyWolf : wolves) {
            greyWolf.calculateFitnessFunctionValue(taskToSchedule)
        }
    }

    void rankWolves() {
        wolves.sort { it.fitnessValue }
        for (int i = 0; i < wolves.size(); i++) {
            Rank rankToSet = i < 3 ? Rank.getRank(i + 1) : Rank.OMEGA
            wolves.get(i).setRank(rankToSet)
        }
    }

    @Override
    String toString() {
        return "Pack-$id"
    }
}


