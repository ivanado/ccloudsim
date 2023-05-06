package org.cloudbus.cloudsim.container.core;

public class ContainerCloudSimTags {
    /**
     * Starting constant value for network-related tags
     **/
    private static final int ContainerSimBASE = 400;
    /**
     * Denotes the receiving of a cloudlet  in the data center broker
     * entity.
     */
    public static final int FIND_VM_FOR_CLOUDLET = ContainerSimBASE + 1;

    /**
     * Denotes the creating a new VM is required in the data center.
     * Invoked in the data center broker.
     */
    public static final int CREATE_NEW_VM = ContainerSimBASE + 2;
    /**
     * Denotes the containers are submitted to the data center.
     * Invoked in the data center broker.
     */
    public static final int CONTAINER_SUBMIT = ContainerSimBASE + 3;

    /**
     * Denotes the containers are created in the data center.
     * Invoked in the data center.
     */
    public static final int CONTAINER_CREATE_ACK = ContainerSimBASE + 4;
    /**
     * Denotes the containers are migrated to another Vm.
     * Invoked in the data center.
     */
    public static final int CONTAINER_MIGRATE = ContainerSimBASE + 10;
    /**
     * Denotes a new VM is created in data center by the local scheduler
     * Invoked in the data center.
     */
    public static final int VM_NEW_CREATE = ContainerSimBASE + 11;

    private static final int CONTAINERS_ON_HOSTS_BASE = ContainerSimBASE + 1000;

    public static final int SCHEDULE_USER_REQUEST_TASKS = CONTAINERS_ON_HOSTS_BASE + 2;

    public static final int SUBMIT_TASK = CONTAINERS_ON_HOSTS_BASE + 3;

    public static final int TASK_COMPLETE = CONTAINERS_ON_HOSTS_BASE + 4;

    public static final int HOST_DATACENTER_EVENT = CONTAINERS_ON_HOSTS_BASE + 5;

    public static final int DATACENTER_PRINT = CONTAINERS_ON_HOSTS_BASE + 6;
    public static final int CONTAINER_DESTROY = CONTAINERS_ON_HOSTS_BASE + 7;
    public static final int HOST_FAIL = CONTAINERS_ON_HOSTS_BASE + 8;
    public static final int CONTAINER_FAIL = CONTAINERS_ON_HOSTS_BASE + 9;
    public static final int HOST_RECOVER = CONTAINERS_ON_HOSTS_BASE + 10;

    private ContainerCloudSimTags() {
        // TODO Auto-generated constructor stub
        /** Private Constructor */
        throw new UnsupportedOperationException("ContainerCloudSim Tags cannot be instantiated");

    }
}
