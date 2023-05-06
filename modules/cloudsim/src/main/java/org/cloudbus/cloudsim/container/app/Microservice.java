package org.cloudbus.cloudsim.container.app;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.container.utils.IDs;

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
        return "ms-" + getId();
    }

    public boolean hasProvider() {
        return provider != null;
    }
    public boolean hasConsumer() {
        return consumer != null;
    }
}
