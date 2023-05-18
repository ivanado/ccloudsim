package org.cloudbus.cloudsim.container.app.model


class MicroserviceCallGraph {

    Microservice ms1 = new Microservice( 15)
    Microservice ms2 = new Microservice( 15)
    Microservice ms3 = new Microservice( 15)
    Microservice ms4 = new Microservice( 15)
    Microservice ms5 = new Microservice( 15)
    Microservice ms6 = new Microservice( 15)

    private MicroserviceCallGraph() {
        ms2.setProvider(ms1)
        ms3.setProvider(ms1)
        ms4.setProvider(ms2)
        ms5.setProvider(ms2)
        ms5.setProvider(ms3)
        ms6.setProvider(ms4)
        ms6.setProvider(ms5)
    }

    List<Microservice> getByType(int userRequestType) {


        return List.of(ms1, ms2, ms3, ms4, ms5, ms6)
    }
    static MicroserviceCallGraph instance = null

    static MicroserviceCallGraph getByType() {
        if (instance == null) {
            instance = new MicroserviceCallGraph()
        }
        return instance
    }

}
