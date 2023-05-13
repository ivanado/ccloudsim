package org.cloudbus.cloudsim.container.app.algo;

import org.cloudbus.cloudsim.container.app.Microservice;
import org.cloudbus.cloudsim.container.app.MicroserviceCallGraph;

import java.util.List;

public class UserRequestType {

    public  int id;
   public List<Microservice> msCallGraph;

    public UserRequestType(int type) {
        this.id = type;
        this.msCallGraph = MicroserviceCallGraph.get(type);

    }
    public int getMicroserviceCount(){
        return this.msCallGraph.size();
    }

    public static UserRequestType getUserRequestTypeOne(){
        return new UserRequestType(1);
    }
}
