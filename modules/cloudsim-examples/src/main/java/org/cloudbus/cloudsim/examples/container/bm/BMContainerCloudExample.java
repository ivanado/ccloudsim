package org.cloudbus.cloudsim.examples.container.bm;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.core.ContainerDatacenterBroker;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.schedulers.UserRequestScheduler;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BMContainerCloudExample {

    public static void main(String[] args) {


        CloudSim.init(1, Calendar.getInstance(), false);

        List<ContainerHost> hosts = CloudFactory.createHosts(1000);

        ContainerDatacenter datacenter = CloudFactory.createDatacenter("BM-DC", hosts);
        ContainerDatacenterBroker broker = new ContainerDatacenterBroker("BM-Broker");
        UserRequestScheduler taskScheduler = new UserRequestScheduler("BM-TaskScheduler", broker.getId(), new ArrayList<>());
        broker.bind(taskScheduler);
//        ContainerHostFaultInjector hostFaultInjector = new ContainerHostFaultInjector(datacenter);
//        ContainerFaultInjector containerFaultInjector = new ContainerFaultInjector(datacenter);
        CloudSim.startSimulation();

        taskScheduler.printTasksReport();


        CloudSim.stopSimulation();
        Log.printLine("finished!");


    }


}