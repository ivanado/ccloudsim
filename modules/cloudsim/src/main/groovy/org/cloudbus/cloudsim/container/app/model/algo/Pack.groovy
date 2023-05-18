package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.Log
import org.cloudbus.cloudsim.container.app.model.ObjectiveFunction
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.utils.IDs

class Pack {
    Firework firework;
    int id;
    int noOfWolves;
    List<GreyWolf> greyWolves;
    List<ContainerHost> hostsToSearch; //subset of all hosts
    private int initialFireworkLocationIdx;
    private double r

    private double distance;

    GreyWolf getByRank(Rank rank) {
        this.greyWolves.find { it.rank == rank }
    }

    Pack(int noOfWolves, Firework firework) {
        this.id = IDs.pollId(Pack.class);
        this.distance = -1;
        this.noOfWolves = noOfWolves;
        this.hostsToSearch = firework.hostsToSearch;
        this.firework = firework;
        this.greyWolves = new ArrayList<>();
        for (int i = 0; i < noOfWolves; i++) {
            this.greyWolves.add(new GreyWolf());
        }
        this.initialFireworkLocationIdx = firework.position;
        this.r = Math.random()
        Log.printLine("Pack #", id," created with ",noOfWolves, " wolwes")
    }


    void initialRankAndPositionWolves() {
        List<Spark> bestSparks = firework.getBestSparks(noOfWolves, null);
        for (int i = 0; i < this.greyWolves.size(); i++) {
            Spark spark = bestSparks.get(i);
            GreyWolf gw = this.greyWolves.get(i);
            gw.rank = Rank.getRank( i + 1 < 5 ? i + 1 : 4)
            gw.currentPositionIndex = spark.position;
            gw.currentPosition = hostsToSearch.get(gw.currentPositionIndex);
            Log.printLine("Pack#$id updatePosition for gw#$gw.id to $gw.currentPositionIndex")

        }
    }

    void updatePositions(double a) {
        double amplitude = this.firework.normalizedAmplitude
        double E = 1 * a - r;

        for (GreyWolf greyWolf : this.greyWolves) {
            //dAlpha=c1*posAlpha-poscurrebt->c1=normalized amplitude*r-----normalAmplitude*r(posalpha - pos current)
            //dbeta=c2*posBeta-poscurrebt->c2=normalized amplitude*r-----normalAmplitude*r(posbeta - pos current)
            double distanceAlpha = amplitude * r * Math.abs(getByRank(Rank.ALPHA).currentPositionIndex - greyWolf.currentPositionIndex)
            double distanceBeta = amplitude * r * Math.abs(getByRank(Rank.BETA).currentPositionIndex - greyWolf.currentPositionIndex)
            double distanceDelta = amplitude * r*Math.abs(getByRank(Rank.DELTA).currentPositionIndex - greyWolf.currentPositionIndex)
            double X1 = Math.abs(getByRank(Rank.ALPHA).currentPositionIndex - E * distanceAlpha)
            double X2 = Math.abs(getByRank(Rank.BETA).currentPositionIndex - E * distanceBeta)
            double X3 = Math.abs(getByRank(Rank.DELTA).currentPositionIndex - E * distanceDelta)
            int newPosition = (int) (X1 + X2 + X3) / 3;

            greyWolf.currentPositionIndex = newPosition % (hostsToSearch.size() - 1)
            greyWolf.currentPosition = hostsToSearch.get(greyWolf.currentPositionIndex)
            Log.printLine("Pack#$id  updatePosition for gw#$greyWolf.id to $greyWolf.currentPositionIndex")

        }
    }

    void calculateFitness(Task taskToSchedule) {
        for (GreyWolf greyWolf : greyWolves) {
            greyWolf.calculateFitnessFunctionValue(taskToSchedule)
        }
    }

    void rank() {
        greyWolves.sort { it.fitnessValue }
        greyWolves.get(0).setRank(Rank.ALPHA)
        greyWolves.get(1).setRank(Rank.BETA)
        greyWolves.get(2).setRank(Rank.DELTA)
        for (int i = 3; i < greyWolves.size(); i++) {
            greyWolves.get(i).setRank(Rank.OMEGA)
        }
    }

    @Override
    String toString() {
        return "Pack-$id"
    }
}


enum Rank {
    ALPHA, BETA, DELTA, OMEGA

    static Rank getRank(int rank) {
        switch (rank) {
            case 1: return ALPHA
            case 2: return BETA
            case 3: return DELTA
            default: return OMEGA
        }
    }
}