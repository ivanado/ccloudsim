package org.cloudbus.cloudsim.container.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Microservices {

    public List<Microservice> microservices;

    public Map<Integer, Double> microserviceThresholdDistance;

    public Microservices(){
        this.microservices = new ArrayList<>();
        microserviceThresholdDistance=new HashMap<>();
        for(int i=0;i<7;i++ ){
            Microservice ms= new Microservice("ms-" + (char)(65+i), 10);
            microservices.add(ms);
        }
    }
    public void calculateThresholdDistance(){

    }

    public double getThresholdDistance(){
        return microserviceThresholdDistance.values().stream().reduce((a,b)-> a+b).get();
    }

}
