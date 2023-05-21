package org.cloudbus.cloudsim.container.app.model

import org.apache.commons.math3.random.RandomDataGenerator
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

    Map<UserRequest, List<Task>> tasksPerUserRequest
    Map<UserRequest, List<Task>> finishedTasksPerUserRequest
    List<Task> runningTasks
    List<Task> finishedTasks
    List<Task> failedTasks

    List<ContainerCloudlet> runningCloudlets
    List<ContainerCloudlet> finishedCloudlets
    List<ContainerCloudlet> failedCloudlets
    List<Map> objectiveFunctionValues = []

    private DatacenterMetrics() {
        this.runningHosts = new ArrayList<>()
        this.failedHosts = new ArrayList<>()
        this.allMicroservices = MicroserviceCallGraph.getByType().getByType(1)
        this.runningMicroservices = new ArrayList<>()
        this.runningMicroservicesHosts = new HashMap<>()
        this.microserviceRunningContainers = new HashMap<>()
        this.microserviceFinishedContainers = new HashMap<>()
        this.microserviceFailedContainers = new HashMap<>()
        this.tasksPerUserRequest = new HashMap<>()
        this.finishedTasksPerUserRequest = new HashMap<>()
        this.hostFailureTimes = new HashMap<>()
        this.userRequestsByType = new HashMap<>()
        this.runningTasks = new ArrayList<>()
        this.finishedTasks = new ArrayList<>()
        this.failedTasks = new ArrayList<>()
        this.runningCloudlets = new ArrayList<>()
        this.finishedCloudlets = new ArrayList<>()
        this.failedCloudlets = new ArrayList<>()
    }

    List<ContainerHost> getRunningHostsWithFreePes(int requiredPes) {
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


    double getContainerResourceConsumption(int msId, UserRequestType requestType) {
        int noOfUserRequests = userRequestsByType.get(requestType.id)?.size() ?: 0
        int userRequestMsCount = requestType.microserviceCount
        List msContainers = microserviceRunningContainers.get(msId)
        int containerReplicaCount = msContainers == null ? 0 : msContainers.size()
        Microservice ms = getById(msId)

        return noOfUserRequests * userRequestMsCount * ms.getResourceConsumption() / containerReplicaCount
    }

    double getContainerResourceConsumption(Task taskToSchedule) {
        int msId = taskToSchedule.getMicroservice().getId()
        int userRequestType = taskToSchedule.getUserRequest().getType().getId()
        double noOfUserRequests = userRequestsByType.get(userRequestType)?.size() ?: 0
        int userRequestMsCount = taskToSchedule.userRequest.getMicroserviceCont()
        List<Container> msContainers = new ArrayList<>(microserviceRunningContainers.get(msId) ?: [])
        msContainers << taskToSchedule.getContainer()
        int containerReplicaCount = msContainers == null ? 0 : msContainers.size()
        Microservice ms = getById(msId)

        return noOfUserRequests * userRequestMsCount * ms.getResourceConsumption() / containerReplicaCount
    }

    Microservice getById(int msId) {
        return allMicroservices.find { ms -> ms.getId() == msId }
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
        final RandomDataGenerator randomDataGenerator = new RandomDataGenerator();
100.times {
    println( randomDataGenerator.nextPoisson(100))

}

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
        List<Task> tasks = this.tasksPerUserRequest[task.userRequest] ?: []
        tasks.add(task)
        this.tasksPerUserRequest[task.userRequest] = tasks
        this.runningTasks.add(task)
    }

    def finishTask(Task task) {
        this.runningTasks.remove(task)
        this.finishedTasks.add(task)
        List<Task> finished = this.finishedTasksPerUserRequest[task.userRequest] ?: []
        finished.add(task)
        this.finishedTasksPerUserRequest[task.userRequest] = finished
    }

    def failTask(Task task) {
        this.runningTasks.remove(task)
        this.failedTasks.add(task)
    }

    boolean hasTasksToProcess() {

        return tasksPerUserRequest.values().flatten().size() > finishedTasksPerUserRequest.values().flatten().size()

    }

    boolean allUserRequestTasksProcessed() {
        List tasks = tasksPerUserRequest.values().flatten()
        List finished = finishedTasksPerUserRequest.values().flatten()
        return tasks.every { finished.contains(it) }
    }

    Task getTask(ContainerCloudlet cloudlet) {
        return tasksPerUserRequest.values().flatten().find { Task t -> t.cloudlet.getCloudletId() == cloudlet.getCloudletId() } as Task
    }

    Task getTask(Container container) {
        return tasksPerUserRequest.values().flatten().find { Task t -> t.container.getId() == container.getId() } as Task
    }

    Task getTask(int containerId) {
        return tasksPerUserRequest.values().flatten().find { Task t -> t.container.getId() == containerId } as Task
    }

    void addObjectiveFunctionValueForTask(Map map, Task task) {
        this.objectiveFunctionValues.add(map)
    }

    Set<Microservice> getRunningMicroservices(Microservice msToSchedule) {
        Set<Microservice> allMicroservices = new HashSet<>(runningMicroservices)
        allMicroservices << msToSchedule
        return allMicroservices
    }

    boolean hasRunningHostsWithResourcesAvailable(int pes) {
        return !getRunningHostsWithFreePes(pes).isEmpty()
    }

    boolean areUserRequestTasksProcessed(UserRequest userRequest) {
        List tasks = tasksPerUserRequest[userRequest]
        List finished = finishedTasksPerUserRequest[userRequest]
        return tasks.size() == finished.size() && tasks.collect { it.getId() }.every { finished.collect { it.id }.contains(it) }
    }

    List<Task> getFinishedUserRequestTasks(UserRequest userRequest) {
        return finishedTasksPerUserRequest[userRequest]
    }

    void printMetrics() {

        println("===================OBJECTIVES PER TASK===============================")
        println("thresholdDistance,clusterBalance,systemFailureRate,totalNetworkDistance,objectiveFunction")
        objectiveFunctionValues.each { println("${it.thresholdDistance},${it.clusterBalance},${it.systemFailureRate},${it.totalNetworkDistance},${it.values().sum()}") }
        File outputLog = new File("modules/cloudsim/build/ObjectivesPerTask.log")
        if (outputLog.exists()) outputLog.delete()
        outputLog << "thresholdDistance clusterBalance systemFailureRate totalNetworkDistance objectiveFunction\n"
        objectiveFunctionValues.each { outputLog << ("${it.thresholdDistance} ${it.clusterBalance} ${it.systemFailureRate} ${it.totalNetworkDistance} ${it.values().sum()}\n") }

        Map best = [:]
        Map worst = [:]
        Map of = objectiveFunctionValues[0]
        best = of
        worst = of
        def bestObjective = worst.values().sum()
        def worstObjective = worst.values().sum()
        def sumObjective = 0;
        Map sums = [thresholdDistance: 0, clusterBalance: 0, systemFailureRate: 0, totalNetworkDistance: 0]
        objectiveFunctionValues.each { it ->
            sums.thresholdDistance = sums.thresholdDistance + it.thresholdDistance
            sums.clusterBalance = sums.clusterBalance + it.clusterBalance
            sums.systemFailureRate = sums.systemFailureRate + it.systemFailureRate
            sums.totalNetworkDistance = sums.totalNetworkDistance + it.totalNetworkDistance
//            if (it.thresholdDistance > worst.thresholdDistance) {
//                worst.thresholdDistance = it.thresholdDistance
//            } else {
//                best.thresholdDistance = it.thresholdDistance
//            }
//            if (it.clusterBalance > worst.clusterBalance) {
//                worst.clusterBalance = it.clusterBalance
//            } else {
//                best.clusterBalance = it.clusterBalance
//            }
//            if (it.systemFailureRate > worst.systemFailureRate) {
//                worst.systemFailureRate = it.systemFailureRate
//            } else {
//                best.systemFailureRate = it.systemFailureRate
//            }
//            if (it.totalNetworkDistance > worst.totalNetworkDistance) {
//                worst.totalNetworkDistance = it.totalNetworkDistance
//            } else {
//                best.totalNetworkDistance = it.totalNetworkDistance
//            }
            def obj = it.values().sum()
            if (obj > worstObjective) {
                worstObjective = obj
                worst = it
            } else {
                bestObjective = obj
                best=it
            }
            sumObjective += obj
        }
        File l = new File("modules/cloudsim/build/stats.log")
        if (l.exists()) l.delete()
        //log stats
        def c = userRequestsByType.values().collect { it.size()}.sum()
        l << "${c} BEST ${best.thresholdDistance} ${best.clusterBalance} ${best.systemFailureRate} ${best.totalNetworkDistance} ${bestObjective}\n"
        l << "${c} WORST ${worst.thresholdDistance} ${worst.clusterBalance} ${worst.systemFailureRate} ${worst.totalNetworkDistance} ${worstObjective}\n"
        l << "${c} MEAN ${sums.thresholdDistance / objectiveFunctionValues.size()} ${worst.clusterBalance / objectiveFunctionValues.size()} ${worst.systemFailureRate / objectiveFunctionValues.size()} ${worst.totalNetworkDistance / objectiveFunctionValues.size()} ${sumObjective / objectiveFunctionValues.size()}\n"
    }
}
