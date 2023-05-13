package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.utils.IDs

class Pack {
    Firework firework;
    int id;
    int noOfWolves;
    List<GreyWolf> greyWolves;
    List<ContainerHost> hostsToSearch; //subset of all hosts
    private int initialFireworkLocationIdx;


    private double distance;

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
    }


    void rankAndPositionWolves() {
        List<Spark> bestSparks = firework.getBestSparks(noOfWolves);
        for (int i = 0; i < this.greyWolves.size(); i++) {
            Spark spark = bestSparks.get(i);
            GreyWolf gw = this.greyWolves.get(i);
            gw.rank = i + 1 < 5 ? i + 1 : 4;
            gw.currentPositionIndex = spark.position;
            gw.currentPosition = hostsToSearch.get(gw.currentPositionIndex);
        }
    }

    void updatePositions() {

    }

}

enum Rank {
    ALPHA(1), BETA(2), DELTA(3), OMEGA(4)
}