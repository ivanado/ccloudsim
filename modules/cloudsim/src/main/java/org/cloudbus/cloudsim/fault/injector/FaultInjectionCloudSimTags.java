package org.cloudbus.cloudsim.fault.injector;

public class FaultInjectionCloudSimTags {
    /**
     * Starting constant value for host fault tags
     **/
    private static final int FAULT_INJECTION_BASE = 1000;

    public static final int HOST_FAIL = FAULT_INJECTION_BASE + 1;

    public static final int CONTAINER_FAIL = FAULT_INJECTION_BASE + 2;


    public static final int CONTAINER_DESTROY = FAULT_INJECTION_BASE + 3;

    public static final int HOST_RECOVER = FAULT_INJECTION_BASE + 4;

}
