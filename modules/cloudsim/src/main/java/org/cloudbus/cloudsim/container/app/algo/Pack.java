package org.cloudbus.cloudsim.container.app.algo;

import org.cloudbus.cloudsim.container.core.ContainerHost;

import java.util.List;

public class Pack {
    public int noOfWolves;

    public List<ContainerHost> hostsToSearch; //position of the pack
    private ContainerHost initialFireworkLocation;

    public Pack(int noOfWolves, List<ContainerHost> hosts) {
        this.noOfWolves = noOfWolves;
        this.hostsToSearch = hosts;
    }

    public void positionInitialFirework() {
        //select random host
        final int idx = (int) (Math.random() * hostsToSearch.size());
        initialFireworkLocation = hostsToSearch.get(idx);
    }

    public void generateSparks() {

    }


}
