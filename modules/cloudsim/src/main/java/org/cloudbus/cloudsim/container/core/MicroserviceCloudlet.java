package org.cloudbus.cloudsim.container.core;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelNull;

import java.util.List;


public class MicroserviceCloudlet extends ContainerCloudlet {
    @Getter
    @Setter
    private MicroserviceCloudlet producerMs; //upstream ms
    @Getter
    @Setter
    private List<MicroserviceCloudlet> consumersMs; //downstream ms
    @Getter
    @Setter
    private ContainerHost host;


    //TODO fix utilization model for cpu
    public MicroserviceCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, new UtilizationModelNull(), new UtilizationModelNull());
        this.producerMs = null;
        this.consumersMs = null;

    }


    public void addConsumerMs(MicroserviceCloudlet cloudlet) {
        if (consumersMs == null) {
            consumersMs = List.of(cloudlet);
        } else {
            consumersMs.add(cloudlet);
        }
    }

    public String toString() {
        return "cloudlet-" + getCloudletId();
    }
}
