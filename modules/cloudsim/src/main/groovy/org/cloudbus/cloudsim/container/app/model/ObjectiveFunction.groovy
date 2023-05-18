package org.cloudbus.cloudsim.container.app.model


import org.cloudbus.cloudsim.container.core.Container
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.util.MathUtil

class ObjectiveFunction {
    private static DatacenterMetrics dcResources = DatacenterMetrics.get()

    static double calculateThresholdDistance(Task taskToSchedule) {
        Set<Microservice> runningMicroservices = new HashSet<>(dcResources.runningMicroservices)
        runningMicroservices.add(taskToSchedule.microservice)

        return runningMicroservices.stream().mapToDouble(ms ->
                ms == taskToSchedule.microservice
                        ? Math.abs(dcResources.getContainerResourceConsumption(taskToSchedule) - DatacenterMetrics.MS_RESOURCE_THRESHOLD)
                        : Math.abs(dcResources.getContainerResourceConsumption(ms.getId(), taskToSchedule.userRequest.type.id) - DatacenterMetrics.MS_RESOURCE_THRESHOLD)
        ).sum()

    }

    private static double physicalMachineUsage(ContainerHost host, int userRequestType) {
        Set<Integer> msIds = host.getRunningMicroserviceIds()
        double containersResourceConsumption = MathUtil.sum(msIds.stream().map(msId -> dcResources.getContainerResourceConsumption(msId, userRequestType)).toList())
        return containersResourceConsumption / host.getCapacity()

    }

    private static double physicalMachineUsage(ContainerHost allocationCandidateHost, Task taskToSchedule) {


        Set<Integer> msIds = new HashSet<>(allocationCandidateHost.getRunningMicroserviceIds())
        msIds.remove(taskToSchedule.microservice.getId())

        double physicalMachineContainersResourcesUsage =
                msIds.stream().mapToDouble(msId ->
                        dcResources.getContainerResourceConsumption(msId, taskToSchedule.userRequest.type.id)
                ).sum()
        +dcResources.getContainerResourceConsumption(taskToSchedule)
        return physicalMachineContainersResourcesUsage / allocationCandidateHost.getCapacity(taskToSchedule.container.getNumberOfPes())
    }

    static double calculateBalancedClusterUse(Task taskToSchedule, ContainerHost allocationCandidateHost) {

        List<Double> physicalMachinesUsage = dcResources.runningHosts.stream().map(host ->
                host.is(allocationCandidateHost)
                        ? physicalMachineUsage(allocationCandidateHost, taskToSchedule)
                        : physicalMachineUsage(host, taskToSchedule.userRequest.type.id)
        ).toList()

        return MathUtil.stDev(physicalMachinesUsage)

    }

    static double calculateSystemFailureRate(Task taskToSchedule) {

        Set<Microservice> allMicroservices = new HashSet<>(dcResources.runningMicroservices)
        allMicroservices.add(taskToSchedule.microservice)

        return allMicroservices.stream().mapToDouble(ms -> calculateServiceFailure(ms)).sum()
    }

    static double calculateServiceFailure(Microservice microservice) {
        List<ContainerHost> hosts = dcResources.runningMicroservicesHosts.get(microservice.getId())
        return hosts.stream().mapToDouble(h ->
                dcResources.getHostFailureRate(h) + dcResources.getMicroserviceContainerFailureRate(microservice.getId())
        ).reduce(1, (a, b) -> a * b)
    }

    static double calculateTotalNetworkDistance(Task taskToSchedule, ContainerHost allocationCandidateHost) {

        Set<Microservice> runningMicroservices = new HashSet<>(dcResources.runningMicroservices)
        runningMicroservices.add(taskToSchedule.microservice)

        return runningMicroservices.stream().mapToDouble(ms ->
                taskToSchedule.microservice.getId() == ms.getId()
                        ? calculateServiceMeanDistance(taskToSchedule, allocationCandidateHost)
                        : calculateServiceMeanDistance(ms)
        ).sum()

    }

    private static double calculateServiceMeanDistance(Microservice microservice) {
        List<Container> containers = dcResources.microserviceRunningContainers.get(microservice.getId())
        List<Container> providerContainers = dcResources.microserviceRunningContainers.get(microservice.getProvider().getId())

        double distance = containers.stream().mapToDouble(c -> providerContainers.stream().mapToInt(pc -> pc.getNetworkDistance(c)).sum()).sum()


        int containerCount = containers.size()
        int providerContainerCount = providerContainers.size()

        return distance / (containerCount * providerContainerCount)

    }

    private static double calculateServiceMeanDistance(Task taskToSchedule, ContainerHost allocationCandidateHost) {
        List<Container> containers = dcResources.microserviceRunningContainers.get(taskToSchedule.microservice.getId())?:[]
        List<Container> providerContainers = taskToSchedule.microservice.getProvider() != null
                ? dcResources.microserviceRunningContainers.get(taskToSchedule.microservice.getProvider().getId())
                : []
        if (containers.isEmpty() || providerContainers.isEmpty()) {
            return 0.0
        }

        double distance = containers.stream().mapToDouble(c -> providerContainers.stream().mapToDouble(pc -> pc.getNetworkDistance(c)).sum()).sum()

        distance += providerContainers.stream().mapToDouble(pc -> pc.getNetworkDistance(allocationCandidateHost)).sum()

        int containerCount = containers.size() + 1
        int providerContainerCount = providerContainers.size()

        return distance / (containerCount * providerContainerCount)

    }


    static Map calculate(Task taskToSchedule, ContainerHost allocationCandidateHost) {

        double td = calculateThresholdDistance(taskToSchedule)
        double cb = calculateBalancedClusterUse(taskToSchedule, allocationCandidateHost)
        double sf = 0//calculateSystemFailureRate(taskToSchedule)
        double tnd = calculateTotalNetworkDistance(taskToSchedule, allocationCandidateHost)
        return [thresholdDistance: td, clusterBalance:  cb, systemFailureRate: sf, totalNetworkDistance: tnd]
    }

}
