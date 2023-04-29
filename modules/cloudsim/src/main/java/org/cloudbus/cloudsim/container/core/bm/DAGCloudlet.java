package org.cloudbus.cloudsim.container.core.bm;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.container.core.ContainerHost;

import java.util.List;

public class DAGCloudlet extends ContainerCloudlet {
    @Getter @Setter
    private Cloudlet parent; //upstream ms
    @Getter @Setter
    private List<Cloudlet> children; //downstream ms
    @Getter @Setter
    private ContainerHost host;
    public DAGCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.parent = null;
        this.children = null;

    }


    public void addChild(DAGCloudlet cloudlet) {
        if(children==null){
            children=List.of(cloudlet);
        }else {
            children.add(cloudlet);
        }
    }

    public String toString(){
        return "cloudlet-"+getCloudletId();
    }
}
