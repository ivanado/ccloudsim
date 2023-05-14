package org.cloudbus.cloudsim.container.app.model

import org.cloudbus.cloudsim.Cloudlet
import org.cloudbus.cloudsim.container.core.Container
import org.cloudbus.cloudsim.container.core.ContainerCloudlet
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.util.MathUtil

class DatacenterResources {
    public static final double MAX_HOST_PES = 32
    public static final double MS_RESOURCE_THRESHOLD = 0.8
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

    List<Task> runningTasks
    List<Task> finishedTasks
    List<Task> failedTasks

    List<ContainerCloudlet> runningCloudlets
    List<ContainerCloudlet> finishedCloudlets
    List<ContainerCloudlet> failedCloudlets


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
        this.runningTasks = new ArrayList<>()
        this.finishedTasks = new ArrayList<>()
        this.failedTasks = new ArrayList<>()
        this.runningCloudlets = new ArrayList<>()
        this.finishedCloudlets = new ArrayList<>()
        this.failedCloudlets = new ArrayList<>()
    }

    List<ContainerHost> getHostsWithFreePes(int requiredPes) {
        return this.runningHosts.findAll { h -> h.getNumberOfFreePes() >= requiredPes }
    }

    List<ContainerHost> getHostsRunningMicroserviceContainer(int msId) {

        return this.runningHosts.findAll { h -> h.getContainerList().any { c -> c.getMicroserviceId() == msId } }
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

    ContainerHost getRandomHost() {
        if (runningHosts.isEmpty()) {
            return null
        }

        final int idx = MathUtil.randomInt(runningHosts.size())
        return runningHosts.get(idx)
    }

    static synchronized DatacenterResources get() {
        if (INSTANCE == null)
            INSTANCE = new DatacenterResources()

        return INSTANCE
    }

    static void main(String[] args) {


    }

    def failCloudlet(ContainerCloudlet cloudlet) {
        runningCloudlets.remove(cloudlet)
        failedCloudlets.add(cloudlet)
    }

    def finishedCloudlet(ContainerCloudlet cloudlet) {
        runningCloudlets.remove(cloudlet)
        finishedCloudlets.add(cloudlet)
    }

    def runningCloudlet(ContainerCloudlet cloudlet) {
        runningCloudlets.add(cloudlet)
    }

    boolean hasRunningHosts() {
        return !runningHosts.isEmpty()
    }

    def failContainer(Container container) {
        this.microserviceRunningContainers[container.getMicroserviceId()]?.remove(container)
        this.microserviceFailedContainers.computeIfAbsent(container.getMicroserviceId(), c -> new ArrayList<>()).add(container)
    }

    def finishContainer(Container container) {
        this.microserviceRunningContainers[container.getMicroserviceId()]?.remove(container)
        this.microserviceFinishedContainers.computeIfAbsent(container.getMicroserviceId(), c -> new ArrayList<>()).add(container)
    }

    def startContainer(Container container) {
        this.microserviceRunningContainers.computeIfAbsent(container.getMicroserviceId(), c -> new ArrayList<>()).add(container)
    }

    Container getRandomContainerToFail() {
        List<Container> containers = microserviceRunningContainers.values().flatten()
        if (containers.isEmpty()) {
            return null
        }

        final int idx = MathUtil.randomInt(containers.size())
        return containers.get(idx)
    }
}
