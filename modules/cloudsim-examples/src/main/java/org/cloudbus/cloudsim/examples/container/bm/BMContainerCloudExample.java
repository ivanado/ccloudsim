package org.cloudbus.cloudsim.examples.container.bm;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.bm.BMContainerDatacenter;
import org.cloudbus.cloudsim.container.core.bm.BMContainerDatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.Calendar;
import java.util.List;

public class BMContainerCloudExample {

    public static void main(String[] args) {




        CloudSim.init(1, Calendar.getInstance(), false);

        List<ContainerHost> hosts = CloudFactory.createHosts(2);

        BMContainerDatacenter datacenter = CloudFactory.createDatacenter("BM-DC", hosts);
        BMContainerDatacenterBroker broker =new BMContainerDatacenterBroker("BM-Broker");
        CloudSim.startSimulation();

broker.printCloudletReport();


        CloudSim.stopSimulation();
        Log.printLine( "finished!");


    }



}