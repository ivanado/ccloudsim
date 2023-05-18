package org.cloudbus.cloudsim.container.app.model


class UserRequestType {
    int id
    List<Microservice> msCallGraph

    UserRequestType(int type) {
        this.id = type
        this.msCallGraph = MicroserviceCallGraph.getByType().getByType(type)

    }

    int getMicroserviceCount() {
        return this.msCallGraph.size()
    }

    static UserRequestType getUserRequestType() {
        return new UserRequestType(1)
    }
}
