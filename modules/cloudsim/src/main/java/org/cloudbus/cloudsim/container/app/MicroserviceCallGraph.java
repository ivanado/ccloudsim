package org.cloudbus.cloudsim.container.app;

import java.util.List;

public class MicroserviceCallGraph {

    public static List<Microservice> get(int userRequestType) {
        Microservice ms1 = new Microservice("A", 15);
        Microservice ms2 = new Microservice("B", 15);
        Microservice ms3 = new Microservice("D", 15);
        ms2.setProvider(ms1);
        ms1.setConsumer(ms2);
        ms3.setProvider(ms2);

        return List.of(ms1, ms2, ms3);
    }


}

