package org.cloudbus.cloudsim.container.app.model

import org.cloudbus.cloudsim.container.core.Container
import org.cloudbus.cloudsim.container.core.ContainerCloudlet
import org.cloudbus.cloudsim.container.core.ContainerHost
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.util.MathUtil

class DatacenterMetrics {
    public static final double MAX_HOST_PES = 32
    public static final double MS_RESOURCE_THRESHOLD = 0.8
    private static DatacenterMetrics INSTANCE = null

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

    Map<UserRequest, List<Task>> allTasksByUserRequest
    List<Task> runningTasks
    List<Task> finishedTasks
    List<Task> failedTasks

    List<ContainerCloudlet> runningCloudlets
    List<ContainerCloudlet> finishedCloudlets
    List<ContainerCloudlet> failedCloudlets
    Map bestObjectiveFunctionValues

    private DatacenterMetrics() {
        this.runningHosts = new ArrayList<>()
        this.failedHosts = new ArrayList<>()
        this.allMicroservices = MicroserviceCallGraph.get().get(1)
        this.runningMicroservices = new ArrayList<>()
        this.runningMicroservicesHosts = new HashMap<>()
        this.microserviceRunningContainers = new HashMap<>()
        this.microserviceFinishedContainers = new HashMap<>()
        this.microserviceFailedContainers = new HashMap<>()
        this.allTasksByUserRequest = new HashMap<>()
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
        double noOfUserRequests = userRequestsByType.get(userRequestType)?.size() ?: 0
        int userRequestMsCount = userRequestsByType.get(userRequestType).stream().findFirst().get().getType().getMicroserviceCount()
        List<Container> msContainers = new ArrayList<>(microserviceRunningContainers.get(msId) ?: new ArrayList<Container>())
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

    static synchronized DatacenterMetrics get() {
        if (INSTANCE == null)
            INSTANCE = new DatacenterMetrics()

        return INSTANCE
    }

    static void main(String[] args) {


    }

    def failCloudlet(ContainerCloudlet cloudlet) {
        runningCloudlets.remove(cloudlet)
        failedCloudlets.add(cloudlet)
    }

    def finishCloudlet(ContainerCloudlet cloudlet) {
        runningCloudlets.remove(cloudlet)
        finishedCloudlets.add(cloudlet)
    }

    def startCloudlet(ContainerCloudlet cloudlet) {
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
        List<Container> containers = microserviceRunningContainers.values().collectMany { it }
        if (containers.isEmpty()) {
            return null
        }

        final int idx = MathUtil.randomInt(containers.size())
        return containers.get(idx)
    }

    def startTask(Task task) {
        List<Task> tasks = this.allTasksByUserRequest[task.userRequest.id] ?: []
        tasks.add(task)
        this.allTasksByUserRequest[task.userRequest.id] = tasks
        this.runningTasks.add(task)
    }

    def finishTask(Task task) {
        this.runningTasks.remove(task)
        this.finishedTasks.add(task)
    }

    def failTask(Task task) {
        this.runningTasks.remove(task)
        this.failedTasks.add(task)
    }

    boolean hasTasksToProcess() {

        return allTasksByUserRequest.values().flatten().size() > finishedTasks.size()

    }

    boolean allTasksProcessed() {
        return allTasksByUserRequest.values().flatten().size() == finishedTasks.size()
    }

    Task getTask(ContainerCloudlet cloudlet) {
        return allTasksByUserRequest.values().flatten().find { t -> t.cloudlet.getCloudletId() == cloudlet.getCloudletId() } as Task
    }

    Task getTask(Container container) {
        return allTasksByUserRequest.values().flatten().find { t -> t.container.getId() == container.getId() }
    }

    void setBestObjectiveFunctionValues(Map map) {
        this.bestObjectiveFunctionValues = map
    }
}