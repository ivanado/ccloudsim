package org.cloudbus.cloudsim.container.app;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.container.utils.IDs;

import static org.cloudbus.cloudsim.container.core.DatacenterResources.MAX_HOST_PES;

public class Microservice {

    @Getter
    private final String name;
    @Getter
    private int id;
    @Getter
    private int requiredPes;

    @Getter
    @Setter
    private Microservice consumer;

    @Getter
    @Setter
    private Microservice provider;

    private int scaleLevel;

    public Microservice(String name, int requiredPes) {
        this.id = IDs.pollId(Microservice.class);
        this.requiredPes = requiredPes;
        this.scaleLevel = 0;
        this.name = name;
        this.provider = null;
        this.consumer = null;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean hasProvider() {
        return provider != null;
    }

    public boolean hasConsumer() {
        return consumer != null;
    }


    public double getResourceConsumption() {
        return (double) requiredPes / MAX_HOST_PES;
    }

    @Override
    public boolean equals(Object obj) {
        return this.id == ((Microservice) obj).getId();
    }
}
