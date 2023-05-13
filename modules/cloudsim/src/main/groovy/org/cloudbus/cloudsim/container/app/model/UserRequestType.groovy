package org.cloudbus.cloudsim.container.app.model


class UserRequestType {
    int id;
    List<Microservice> msCallGraph;

    UserRequestType(int type) {
        this.id = type;
        this.msCallGraph = MicroserviceCallGraph.get(type);

    }

    int getMicroserviceCount() {
        return this.msCallGraph.size();
    }

    static UserRequestType getUserRequestTypeOne() {
        return new UserRequestType(1);
    }
}
