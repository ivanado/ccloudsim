package org.cloudbus.cloudsim.container.app

import org.cloudbus.cloudsim.Log
import org.cloudbus.cloudsim.container.app.model.UserRequest
import org.cloudbus.cloudsim.container.core.ContainerDatacenter
import org.cloudbus.cloudsim.container.core.ContainerDatacenterBroker
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.container.schedulers.UserRequestScheduler
import org.cloudbus.cloudsim.core.CloudSim

class Simulation {
    static void main(String[] args) {

        CloudSim.init(1, Calendar.getInstance(), false)

        List<ContainerHost> hosts = CloudFactory.createHosts(200)

        ContainerDatacenter datacenter = CloudFactory.createDatacenter("BM-DC", hosts)
        ContainerDatacenterBroker broker = new ContainerDatacenterBroker("BM-Broker")

        List<UserRequest> userRequests = []
        200.times {
           userRequests << new UserRequest(broker.getId())
        }

        UserRequestScheduler taskScheduler = new UserRequestScheduler("BM-TaskScheduler", broker.getId(), userRequests)
        broker.bind(taskScheduler)
//        ContainerHostFaultInjector hostFaultInjector = new ContainerHostFaultInjector(datacenter);
//        ContainerFaultInjector containerFaultInjector = new ContainerFaultInjector(datacenter);

        CloudSim.startSimulation()

        taskScheduler.printTasksReport()
broker.printSystemMetrics()

        CloudSim.stopSimulation()
        Log.printLine("finished!")


    }
}
