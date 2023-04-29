package org.cloudbus.cloudsim.container.core.bm;

import lombok.Getter;
import lombok.Setter;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisioner;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisioner;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.schedulers.ContainerScheduler;

import java.util.ArrayList;
import java.util.List;

public class  BMContainerHost extends ContainerHost {
    /**
     * The id.
     */
    @Getter @Setter
    private int id;

    /**
     * The allocation policy.
     */
    @Getter @Setter
    private ContainerScheduler containerScheduler;

    /**
     * The vm list.
     */
    @Getter
    private final List<Container> containerList = new ArrayList<>();

    /**
     * The pe list.
     */
    @Getter
    private List<? extends ContainerPe> peList;

    /**
     * Tells whether this machine is working properly or has failed.
     */
    @Getter
    private boolean failed;

    /**
     * The datacenter where the host is placed.
     */
    @Getter @Setter
    private BMContainerDatacenter datacenter;


    @Getter @Setter
    private ContainerRamProvisioner containerRamProvisioner;

    @Getter @Setter
    private ContainerBwProvisioner containerBwProvisioner;



    public BMContainerHost(
            int id,
            List<? extends ContainerPe> peList,
            ContainerRamProvisioner containerRamProvisioner,
            ContainerBwProvisioner containerBwProvisioner,

            ContainerScheduler containerScheduler) {
        super(id,containerRamProvisioner,containerBwProvisioner,0,peList,containerScheduler);
        setId(id);
        setContainerBwProvisioner(containerBwProvisioner);
        setContainerRamProvisioner(containerRamProvisioner);
        setContainerScheduler(containerScheduler);
        setPeList(peList);
        setFailed(false);

    }
    /**
     * Returns maximum available MIPS among all the PEs.
     *
     * @return max mips
     */
    public double getMaxAvailableMips() {
        //Log.printLine("Host: Maximum Available Pes:......");
        return containerScheduler.getMaxAvailableMips();
    }

    /**
     * Gets the free mips.
     *
     * @return the free mips
     */
    public double getAvailableMips() {
        //Log.printLine("Host: Get available Mips");
        return containerScheduler.getAvailableMips();
    }
    public boolean containerCreate(Container container) {

        if (!getContainerScheduler().allocatePesForContainer(container, container.getCurrentRequestedMips())) {
            Log.printConcatLine("[Scheduler.containerCreate] Allocation of container #", container.getId(), " to Host #", getId(),
                    " failed by MIPS");
            getContainerRamProvisioner().deallocateRamForContainer(container);
            getContainerBwProvisioner().deallocateBwForContainer(container);
            return false;
        }

        containerList.add(container);
        return true;
    }

}
