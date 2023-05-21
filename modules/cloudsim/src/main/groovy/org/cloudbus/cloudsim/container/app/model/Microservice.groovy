package org.cloudbus.cloudsim.container.app.model


import org.cloudbus.cloudsim.container.utils.IDs


class Microservice {

    String name
    int id
    int requiredPes


//    Microservice consumer

//    Microservice provider
    List<Microservice> providers = []
    List<Microservice> consumers = []

    Microservice(int requiredPes) {
        this.id = IDs.pollId(Microservice.class)
        this.requiredPes = requiredPes
        this.name = "ms-$id"
//        this.provider = null
//        this.consumer = null
    }

    @Override
    String toString() {
        return name
    }

    void setProvider(Microservice provider) {
        providers.add(provider)
        provider.setConsumer(this)
    }

    void setProviders(Microservice... ps) {
        providers.addAll(ps.toList())
    }

    void setConsumer(Microservice consumer){
        consumers.add(consumer)
    }
    void setConsumers(Microservice... cs){
        consumers.add(cs)
    }
    double getResourceConsumption() {
        return (double) requiredPes / DatacenterMetrics.MAX_HOST_PES
    }

    @Override
    boolean equals(Object obj) {
        return this.id == ((Microservice) obj).getId()
    }
}
