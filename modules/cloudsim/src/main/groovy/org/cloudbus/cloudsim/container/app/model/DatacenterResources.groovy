package org.cloudbus.cloudsim.container.app.model


import org.cloudbus.cloudsim.container.core.Container
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.core.CloudSim

class DatacenterResources {
    public static final double MAX_HOST_PES = 32
    public static final double MS_RESOURCE_THRESHOLD = 0.8D
    private static DatacenterResources INSTANCE = null

    List<ContainerHost> runningHosts
    List<ContainerHost> failedHosts

    List<Microservice> allMicroservices
    List<Microservice> runningMicroservices
    Map<Integer, List<ContainerHost>> runningMicroservicesHosts
    Map<Integer, List<Container>> microserviceRunningContainers
    Map<Integer, List<Container>> microserviceFinishedContainers
    Map<Integer, List<Container>> microserviceFailedContainers

    Map<Integer, List<UserRequest>> userRequestsByType
    Map<ContainerHost, List<Double>> hostFailureTimes
    Map<Integer, List<Double>> microserviceContainerFailureTimes

    private DatacenterResources() {
        this.runningHosts = new ArrayList<>()
        this.failedHosts = new ArrayList<>()
        this.allMicroservices = new ArrayList<>()
        this.runningMicroservices = new ArrayList<>()
        this.runningMicroservicesHosts = new HashMap<>()
        this.microserviceRunningContainers = new HashMap<>()
        this.microserviceFinishedContainers = new HashMap<>()
        this.microserviceFailedContainers = new HashMap<>()
        this.hostFailureTimes = new HashMap<>()
        this.userRequestsByType = new HashMap<>()

    }

    List<ContainerHost> getHostsWithFreePes(int requiredPes) {
        return this.runningHosts.stream().filter(h -> h.getNumberOfFreePes() >= requiredPes).toList()
    }

    List<ContainerHost> getHostsRunningMicroserviceContainer(int msId) {
        return this.runningHosts.stream().filter(h -> h
                .getContainerList()
                .stream()
                .anyMatch(c -> c.getMicroserviceId() == msId)
        ).toList()
    }

    List<Container> getMicroserviceContainer(int msId) {
        return this.microserviceRunningContainers.get(msId)
    }

    int getUserRequestCountByType(int type) {
        return userRequestsByType.get(type).size()
    }

    void addUserRequest(UserRequest userRequest) {
        userRequestsByType.computeIfAbsent(userRequest.getType().getId(), ur -> new ArrayList<>()).add(userRequest)
    }


    double getContainerResourceConsumption(int msId, int userRequestType) {
        double noOfUserRequests = (double) userRequestsByType.get(userRequestType).size()
        int userRequestMsCount = userRequestsByType.get(userRequestType).stream().findFirst().get().getType().getMicroserviceCount()
        List msContainers = microserviceRunningContainers.get(msId)
        int containerReplicaCount = msContainers == null ? 0 : msContainers.size()
        Microservice ms = getById(msId)

        return noOfUserRequests * userRequestMsCount * ms.getResourceConsumption() / containerReplicaCount
    }

    double getContainerResourceConsumption(Task taskToSchedule) {
        int msId = taskToSchedule.getMicroservice().getId()
        int userRequestType = taskToSchedule.getUserRequest().getType().getId()
        double noOfUserRequests = userRequestsByType.get(userRequestType).size()
        int userRequestMsCount = userRequestsByType.get(userRequestType).stream().findFirst().get().getType().getMicroserviceCount()
        List<Container> msContainers = new ArrayList<>(microserviceRunningContainers.get(msId))
        msContainers.add(taskToSchedule.getContainer())
        int containerReplicaCount = msContainers == null ? 0 : msContainers.size()
        Microservice ms = getById(msId)

        return noOfUserRequests * userRequestMsCount * ms.getResourceConsumption() / containerReplicaCount
    }

    Microservice getById(int msId) {
        return allMicroservices.stream().filter(ms -> ms.getId() == msId).findFirst().orElse(null)
    }

    double getHostFailureRate(ContainerHost host) {
        List<Double> failureTimes = hostFailureTimes.get(host)
        return CloudSim.clock() / failureTimes.size()
    }

    double getMicroserviceContainerFailureRate(int microserviceId) {
        List<Double> failureTimes = microserviceContainerFailureTimes.get(microserviceId)
        return CloudSim.clock() / failureTimes.size()

    }


    static synchronized DatacenterResources get() {
        if (INSTANCE == null)
            INSTANCE = new DatacenterResources()

        return INSTANCE
    }

    static void main(String[] args) {


    }
}
