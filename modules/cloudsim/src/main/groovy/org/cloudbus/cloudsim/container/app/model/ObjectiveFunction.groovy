package org.cloudbus.cloudsim.container.app.model


import org.cloudbus.cloudsim.container.core.Container
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.util.MathUtil

class ObjectiveFunction {
    private static DatacenterMetrics dcMetrics = DatacenterMetrics.get()

    static double calculateThresholdDistance(Task taskToSchedule) {
        Set<Microservice> runningMicroservices = new HashSet<>(dcMetrics.runningMicroservices)
        runningMicroservices.add(taskToSchedule.microservice)

        return runningMicroservices.stream().mapToDouble(ms ->
                ms == taskToSchedule.microservice
                        ? Math.abs(dcMetrics.getContainerResourceConsumption(taskToSchedule) - DatacenterMetrics.MS_RESOURCE_THRESHOLD)
                        : Math.abs(dcMetrics.getContainerResourceConsumption(ms.getId(), taskToSchedule.userRequest.type) - DatacenterMetrics.MS_RESOURCE_THRESHOLD)
        ).sum()

    }

    private static double physicalMachineUsage(ContainerHost host, UserRequestType userRequestType) {
        Set<Integer> msIds = host.getRunningMicroserviceIds()
        double containersResourceConsumption = msIds.size() > 0 ? msIds.collect { msId -> dcMetrics.getContainerResourceConsumption(msId, userRequestType) }.sum() as Double : 0
        return containersResourceConsumption / host.getCapacity()

    }

    private static double physicalMachineUsage(ContainerHost allocationCandidateHost, Task taskToSchedule) {
        Set<Integer> hostMsIds = new HashSet<>(allocationCandidateHost.getRunningMicroserviceIds())

        double physicalMachineContainersResourcesUsage = hostMsIds.size() > 0
                ? hostMsIds.collect { msId -> dcMetrics.getContainerResourceConsumption(msId, taskToSchedule.userRequest.type) }.sum() as Double
                : 0
        physicalMachineContainersResourcesUsage += dcMetrics.getContainerResourceConsumption(taskToSchedule)

        return physicalMachineContainersResourcesUsage / allocationCandidateHost.getCapacity(taskToSchedule.container.getNumberOfPes())
    }

    static double calculateBalancedClusterUse(Task taskToSchedule, ContainerHost allocationCandidateHost) {
        List<Double> physicalMachinesUsage = dcMetrics.runningHosts.collect { host ->
            host.is(allocationCandidateHost)
                    ? physicalMachineUsage(allocationCandidateHost, taskToSchedule)
                    : physicalMachineUsage(host, taskToSchedule.userRequest.type)
        }


        return MathUtil.stDev(physicalMachinesUsage)

    }

    static double calculateSystemFailureRate(Task taskToSchedule) {

        Set<Microservice> allMicroservices = dcMetrics.getRunningMicroservices(taskToSchedule.microservice)

        return allMicroservices.collect { ms -> calculateServiceFailure(ms) }.sum() as Double
    }

    static double calculateServiceFailure(Microservice microservice) {
        List<ContainerHost> hosts = dcMetrics.runningMicroservicesHosts.get(microservice.getId())
        List<Double> hostsFailureRate = hosts.collect { h -> dcMetrics.getHostFailureRate(h) + dcMetrics.getMicroserviceContainerFailureRate(microservice.getId()) }
        double serviceFailure = 1
        hostsFailureRate.forEach(hostFailureRate -> serviceFailure *= hostsFailureRate)
        return serviceFailure
    }

    static double calculateTotalNetworkDistance(Task taskToSchedule, ContainerHost allocationCandidateHost) {

        Set<Microservice> allMicroservices = dcMetrics.getRunningMicroservices(taskToSchedule.microservice)

        return allMicroservices.collect { ms ->
            taskToSchedule.microservice == ms
                    ? calculateServiceMeanDistance(taskToSchedule, allocationCandidateHost)
                    : calculateServiceMeanDistance(ms)
        }.sum() as Double

    }

    private static double calculateServiceMeanDistance(Microservice microservice) {
        List<Container> containers = dcMetrics.microserviceRunningContainers.get(microservice.getId())
        List<Container> providerContainers = dcMetrics.microserviceRunningContainers.get(microservice.getProvider().getId())

        double distance = containers.collect {
            container ->
                providerContainers.collect {
                    providerContainer -> providerContainer.getNetworkDistance(container)
                }.sum() as Double
        }.sum() as Double


        int containerCount = containers.size()
        int providerContainerCount = providerContainers.size()

        return distance / (containerCount * providerContainerCount)

    }

    private static double calculateServiceMeanDistance(Task taskToSchedule, ContainerHost allocationCandidateHost) {
        List<Container> containers = dcMetrics.microserviceRunningContainers.get(taskToSchedule.microservice.getId()) ?: []

        List<Container> providerContainers = !taskToSchedule.getProviders().isEmpty()
                ? taskToSchedule.providers*.container
                : []
        if (containers.isEmpty() || providerContainers.isEmpty()) {
            return 0.0
        }

        double distance = containers.collect { c -> providerContainers.collect { pc -> pc.getNetworkDistance(c) }.sum()   as Double }.sum()  as Double

        distance += providerContainers.collect { pc -> pc.getNetworkDistance(allocationCandidateHost) }.sum()  as Double

        int containerCount = containers.size() + 1
        int providerContainerCount = providerContainers.size()

        return distance / (containerCount * providerContainerCount)

    }


    static Map calculate(Task taskToSchedule, ContainerHost allocationCandidateHost) {

        double td = calculateThresholdDistance(taskToSchedule)
        double cb = calculateBalancedClusterUse(taskToSchedule, allocationCandidateHost)
        double sf = 0//calculateSystemFailureRate(taskToSchedule)
        double tnd = calculateTotalNetworkDistance(taskToSchedule, allocationCandidateHost)
        return [thresholdDistance: td, clusterBalance: cb, systemFailureRate: sf, totalNetworkDistance: tnd]
    }

}
