package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.container.app.model.Microservice;
import org.cloudbus.cloudsim.container.app.model.Task;
import org.cloudbus.cloudsim.container.app.model.UserRequest;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisionerSimple;
import org.cloudbus.cloudsim.container.schedulers.ContainerSchedulerTimeShared;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatacenterResources {
    public static final double MAX_HOST_PES = 32;
    public static final double MS_RESOURCE_THRESHOLD = 0.8D;
    private static DatacenterResources INSTANCE = null;

    public List<ContainerHost> runningHosts;
    public List<ContainerHost> failedHosts;

    public List<Microservice> allMicroservices;
    public List<Microservice> runningMicroservices;
    public Map<Integer, List<ContainerHost>> runningMicroservicesHosts;
    public Map<Integer, List<Container>> microserviceRunningContainers;
    public Map<Integer, List<Container>> microserviceFinishedContainers;
    public Map<Integer, List<Container>> microserviceFailedContainers;

    public Map<Integer, List<UserRequest>> userRequestsByType;
    public Map<ContainerHost,List<Double>> hostFailureTimes;
    public Map<Integer,List<Double>> microserviceContainerFailureTimes;

    private DatacenterResources() {
        this.runningHosts = new ArrayList<>();
        this.failedHosts = new ArrayList<>();
        this.allMicroservices = new ArrayList<>();
        this.runningMicroservices = new ArrayList<>();
        this.runningMicroservicesHosts = new HashMap<>();
        this.microserviceRunningContainers = new HashMap<>();
        this.microserviceFinishedContainers = new HashMap<>();
        this.microserviceFailedContainers = new HashMap<>();
        this.hostFailureTimes = new HashMap<>();
        this.userRequestsByType = new HashMap<>();

    }

    public List<ContainerHost> getHostsWithFreePes(int requiredPes) {
        return this.runningHosts.stream().filter(h -> h.getNumberOfFreePes() >= requiredPes).toList();
    }

    public List<ContainerHost> getHostsRunningMicroserviceContainer(int msId) {
        return this.runningHosts.stream().filter(h -> h
                .getContainerList()
                .stream()
                .anyMatch(c -> c.getMicroserviceId() == msId)
        ).toList();
    }

    public List<Container> getMicroserviceContainer(int msId) {
        return this.microserviceRunningContainers.get(msId);
    }

    public int getUserRequestCountByType(int type) {
        return userRequestsByType.get(type).size();
    }

    public void addUserRequest(UserRequest userRequest) {
        userRequestsByType.computeIfAbsent(userRequest.getType().getId(), ur -> new ArrayList<>()).add(userRequest);
    }


    public double getContainerResourceConsumption(int msId, int userRequestType) {
        double noOfUserRequests = (double) userRequestsByType.get(userRequestType).size();
        int userRequestMsCount = userRequestsByType.get(userRequestType).stream().findFirst().get().getType().getMicroserviceCount();
        List msContainers = microserviceRunningContainers.get(msId);
        int containerReplicaCount = msContainers == null ? 0 : msContainers.size();
        Microservice ms = getById(msId);

        return noOfUserRequests * userRequestMsCount * ms.getResourceConsumption() / containerReplicaCount;
    }

    public double getContainerResourceConsumption(Task taskToSchedule) {
        int msId = taskToSchedule.getMicroservice().getId();
        int userRequestType = taskToSchedule.getUserRequest().getType().getId();
        double noOfUserRequests = userRequestsByType.get(userRequestType).size();
        int userRequestMsCount = userRequestsByType.get(userRequestType).stream().findFirst().get().getType().getMicroserviceCount();
        List<Container> msContainers = new ArrayList<>(microserviceRunningContainers.get(msId));
        msContainers.add(taskToSchedule.getContainer());
        int containerReplicaCount = msContainers == null ? 0 : msContainers.size();
        Microservice ms = getById(msId);

        return noOfUserRequests * userRequestMsCount * ms.getResourceConsumption() / containerReplicaCount;
    }

    public Microservice getById(int msId) {
        return allMicroservices.stream().filter(ms -> ms.getId() == msId).findFirst().orElse(null);
    }

    public double getHostFailureRate(ContainerHost host) {
        List<Double> failureTimes = hostFailureTimes.get(host);
        return CloudSim.clock() / failureTimes.size();
    }

    public double getMicroserviceContainerFailureRate(int microserviceId) {
        List<Double> failureTimes = microserviceContainerFailureTimes.get(microserviceId);
        return CloudSim.clock() / failureTimes.size();

    }




    public static synchronized DatacenterResources get() {
        if (INSTANCE == null)
            INSTANCE = new DatacenterResources();

        return INSTANCE;
    }

    public static void main(String[] args) {
Task t = new Task(new Microservice("m",11), 1, new UserRequest(1));

        Container tmpContainer = t.getContainer();
//        System.out.println(t.container.getHost().getId());
        List<ContainerPe> peList=Arrays.asList(new ContainerPe(IDs.pollId(ContainerPe.class), new ContainerPeProvisionerSimple(1000)));
        tmpContainer.setHost(new ContainerHost(666, new ContainerRamProvisionerSimple(100), new ContainerBwProvisionerSimple(100),0, peList, new ContainerSchedulerTimeShared(peList)));

        System.out.println(t.getContainer().getHost().getId());
        System.out.println(tmpContainer.getHost().getId());

    }
}
