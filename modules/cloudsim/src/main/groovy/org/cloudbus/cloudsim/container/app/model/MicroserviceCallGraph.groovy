package org.cloudbus.cloudsim.container.app.model


class MicroserviceCallGraph {

    Microservice ms1 = new Microservice("A", 15)
    Microservice ms2 = new Microservice("B", 15)
    Microservice ms3 = new Microservice("D", 15)

    private MicroserviceCallGraph() {
        ms2.setProvider(ms1)
        ms1.setConsumer(ms2)
        ms3.setProvider(ms2)
    }

    List<Microservice> get(int userRequestType) {


        return List.of(ms1, ms2, ms3)
    }
    static MicroserviceCallGraph instance = null

    static MicroserviceCallGraph get() {
        if (this.instance == null) {
            this.instance = new MicroserviceCallGraph()
        }
        return this.instance
    }

}
