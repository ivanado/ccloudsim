package org.cloudbus.cloudsim.examples.container.bm;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.core.ContainerDatacenterBroker;
import org.cloudbus.cloudsim.container.faultInjectors.ContainerHostFaultInjector;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.Calendar;
import java.util.List;

public class BMContainerCloudExample {

    public static void main(String[] args) {


        CloudSim.init(1, Calendar.getInstance(), false);

        List<ContainerHost> hosts = CloudFactory.createHosts(2);

        ContainerDatacenter datacenter = CloudFactory.createDatacenter("BM-DC", hosts);
        ContainerDatacenterBroker broker = new ContainerDatacenterBroker("BM-Broker");
        ContainerHostFaultInjector hostFaultInjector = new ContainerHostFaultInjector(datacenter);
        CloudSim.startSimulation();

        broker.printCloudletReport();


        CloudSim.stopSimulation();
        Log.printLine("finished!");


    }


}