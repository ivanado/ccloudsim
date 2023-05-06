package org.cloudbus.cloudsim.container.core;

public class ContainerCloudSimTags {
    /**
     * Starting constant value for network-related tags
     **/
    private static final int ContainerSimBASE = 400;

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
     * Denotes the containers are failed in the data center.
     * Invoked in the failure injector.
     */
    public static final int CONTAINER_FAIL = ContainerSimBASE + 5;
    /**
     * Denotes the containers are deallocated in the data center.
     * Invoked in the dc
     */
    public static final int CONTAINER_DESTROY = ContainerSimBASE + 6;

    /**
     * Denotes the user req assigned to the data center.
     * Invoked internally in the Task scheduler
     */
    public static final int USER_REQUEST_SUBMIT = ContainerSimBASE + 8;
    /**
     * Denotes the user req is finished.
     * Invoked internally in the Task scheduler
     */
    public static final int USER_REQUEST_RETURN = ContainerSimBASE + 9;

    /**
     * Denotes the task assigned to the data center.
     * Invoked in broker
     */
    public static final int TASK_SUBMIT = ContainerSimBASE + 8;
    /**
     * Denotes the user req is finished.
     * Invoked in Broker
     */
    public static final int TASK_RETURN = ContainerSimBASE + 9;
    /**
     * Denotes the  host failure via injection.
     * Invoked in the Task scheduler
     */
    public static final int HOST_FAIL = ContainerSimBASE + 10;
    /**
     * Denotes the  host deallocated from DC.
     */
    public static final int HOST_DESTROY = ContainerSimBASE + 11;
    /**
     * Denotes the  host recovery from failure.
     * Invoked in the Injection
     */
    public static final int HOST_RECOVER = ContainerSimBASE + 12;
    /**
     * Denotes the  host recovered from failure.
     * Invoked in the data centar
     */
    public static final int HOST_RECOVER_ACK = ContainerSimBASE + 13;
    /**
     * Denotes the DC info is logged to console.
     * Invoked in the dc
     */
    public static final int CONTAINER_DC_LOG = ContainerSimBASE + 14;
    /**
     * Denotes the containers are deallocated in the data center.
     * Invoked in the dc
     */
    public static final int CONTAINER_DC_EVENT = ContainerSimBASE + 7; //invoked repeatedly to process the cloudlet
    private ContainerCloudSimTags() {
        // TODO Auto-generated constructor stub
        throw new UnsupportedOperationException("ContainerCloudSim Tags cannot be instantiated");

    }
}
