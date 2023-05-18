package org.cloudbus.cloudsim.container.app.model


import org.cloudbus.cloudsim.container.utils.IDs


class Microservice {

    String name
    int id
    int requiredPes

    Microservice consumer

    Microservice provider


    Microservice(String name, int requiredPes) {
        this.id = IDs.pollId(Microservice.class)
        this.requiredPes = requiredPes
        this.name = name
        this.provider = null
        this.consumer = null
    }

    @Override
    String toString() {
        return name
    }

    boolean hasProvider() {
        return provider != null
    }

    boolean hasConsumer() {
        return consumer != null
    }


    double getResourceConsumption() {
        return (double) requiredPes / DatacenterMetrics.MAX_HOST_PES
    }

    @Override
    boolean equals(Object obj) {
        return this.id == ((Microservice) obj).getId()
    }
}
