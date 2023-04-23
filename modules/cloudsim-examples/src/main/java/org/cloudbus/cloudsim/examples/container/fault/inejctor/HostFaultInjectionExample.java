package org.cloudbus.cloudsim.examples.container.fault.inejctor;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModelNull;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.CotainerPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPe;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmRamProvisionerSimple;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.core.ContainerDatacenterBroker;
import org.cloudbus.cloudsim.container.core.ContainerDatacenterCharacteristics;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.container.core.PowerContainer;
import org.cloudbus.cloudsim.container.core.PowerContainerDatacenterCM;
import org.cloudbus.cloudsim.container.core.PowerContainerVm;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.container.resourceAllocators.PowerContainerAllocationPolicySimple;
import org.cloudbus.cloudsim.container.schedulers.ContainerCloudletSchedulerDynamicWorkload;
import org.cloudbus.cloudsim.container.schedulers.ContainerSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.container.schedulers.ContainerVmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.container.ConstantsExamples;
import org.cloudbus.cloudsim.examples.container.UtilizationModelPlanetLabInMemoryExtended;
import org.cloudbus.cloudsim.fault.injector.HostFaultInjector;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class HostFaultInjectionExample {

    public static void main(String[] args) {
        Log.printLine("Starting ContainerCloudHostFailureInjectionExample...");

        try {

            int num_user = 1;

            Calendar calendar = Calendar.getInstance();

            boolean trace_flag = false;
            CloudSim.init(num_user, calendar, trace_flag);

            ContainerAllocationPolicy containerAllocationPolicy = new PowerContainerAllocationPolicySimple();


            double overUtilizationThreshold = 0.80;
            double underUtilizationThreshold = 0.70;

            List<ContainerHost> hostList = createHostList(ConstantsExamples.NUMBER_HOSTS);
            ContainerVmAllocationPolicy vmAllocationPolicy = new ContainerVmAllocationPolicySimple(hostList);

            int overBookingFactor = 80;
            ContainerDatacenterBroker broker = createBroker(overBookingFactor);
            int brokerId = broker.getId();

            List<ContainerCloudlet> cloudletList = createContainerCloudletList(brokerId, ConstantsExamples.NUMBER_CLOUDLETS);
            List<Container> containerList = createContainerList(brokerId, ConstantsExamples.NUMBER_CLOUDLETS);
            List<ContainerVm> vmList = createVmList(brokerId, ConstantsExamples.NUMBER_VMS);

            String logAddress = "~/Results";

            ContainerDatacenter datacenter = createDatacenter("DC",
                    PowerContainerDatacenterCM.class, hostList, vmAllocationPolicy, containerAllocationPolicy,
                    getExperimentName("ContainerCloudHostFailureInjectionExample-1", String.valueOf(overBookingFactor)),
                    ConstantsExamples.SCHEDULING_INTERVAL, logAddress,
                    ConstantsExamples.VM_STARTTUP_DELAY, ConstantsExamples.CONTAINER_STARTTUP_DELAY);
            HostFaultInjector hfi = new HostFaultInjector(datacenter);

            broker.submitCloudletList(cloudletList.subList(0, containerList.size()));
            broker.submitContainerList(containerList);
            broker.submitVmList(vmList);

            CloudSim.terminateSimulation(86400.00);

            double endTime = CloudSim.startSimulation();

            System.out.printf("%n# Total simulation time: %s%n", endTime);

            List<ContainerCloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);
            printFaultStats(hfi);
            Log.printLine("HFI finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static void printFaultStats(HostFaultInjector fault) {

        System.out.printf("# Number of failed hosts: %d%n", fault.getHostFaultsNumber());

        System.out.printf("# Hosts MTBF: %d minutes%n", fault.meanTimeBetweenHostFaultsInMinutes());
        System.out.printf("# Hosts MTBF: %d seconds%n", fault.meanTimeBetweenHostFaultsInSeconds());

        System.out.println("# " + fault.getHostFailureTimesReport());
    }

    private static String getExperimentName(String... args) {
        StringBuilder experimentName = new StringBuilder();

        for (int i = 0; i < args.length; ++i) {
            if (!args[i].isEmpty()) {
                if (i != 0) {
                    experimentName.append("_");
                }

                experimentName.append(args[i]);
            }
        }

        return experimentName.toString();
    }


    private static ContainerDatacenterBroker createBroker(int overBookingFactor) {

        ContainerDatacenterBroker broker = null;

        try {
            broker = new ContainerDatacenterBroker("Broker", overBookingFactor);
        } catch (Exception var2) {
            var2.printStackTrace();
            System.exit(0);
        }

        return broker;
    }


    private static void printCloudletList(List<ContainerCloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (ContainerCloudlet containerCloudlet : list) {
            cloudlet = containerCloudlet;
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatusString() == "Success") {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + cloudlet.getResourceId()
                        + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent
                        + dft.format(cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime()));
            }
        }
    }


    private static ArrayList<ContainerVm> createVmList(int brokerId, int containerVmsNumber) {
        ArrayList<ContainerVm> containerVms = new ArrayList<>();

        for (int i = 0; i < containerVmsNumber; ++i) {
            ArrayList<ContainerPe> peList = new ArrayList<>();
            int vmType = i / (int) Math.ceil((double) containerVmsNumber / 4.0D);
            for (int j = 0; j < ConstantsExamples.VM_PES[vmType]; ++j) {
                peList.add(new ContainerPe(j,
                        new CotainerPeProvisionerSimple((double) ConstantsExamples.VM_MIPS[vmType])));
            }
            containerVms.add(new PowerContainerVm(IDs.pollId(ContainerVm.class), brokerId,
                    (double) ConstantsExamples.VM_MIPS[vmType], (float) ConstantsExamples.VM_RAM[vmType],
                    ConstantsExamples.VM_BW, ConstantsExamples.VM_SIZE, "Xen",
                    new ContainerSchedulerTimeSharedOverSubscription(peList),
                    new ContainerRamProvisionerSimple(ConstantsExamples.VM_RAM[vmType]),
                    new ContainerBwProvisionerSimple(ConstantsExamples.VM_BW),
                    peList, ConstantsExamples.SCHEDULING_INTERVAL));


        }

        return containerVms;
    }

    public static List<ContainerHost> createHostList(int hostsNumber) {
        ArrayList<ContainerHost> hostList = new ArrayList<>();
        for (int i = 0; i < hostsNumber; ++i) {
            int hostType = i / (int) Math.ceil((double) hostsNumber / 3.0D);
            ArrayList<ContainerVmPe> peList = new ArrayList<>();
            for (int j = 0; j < ConstantsExamples.HOST_PES[hostType]; ++j) {
                peList.add(new ContainerVmPe(j,
                        new ContainerVmPeProvisionerSimple((double) ConstantsExamples.HOST_MIPS[hostType])));
            }

            hostList.add(new ContainerHost(IDs.pollId(ContainerHost.class),
                    new ContainerVmRamProvisionerSimple(ConstantsExamples.HOST_RAM[hostType]),
                    new ContainerVmBwProvisionerSimple(1000000L), 1000000L, peList,
                    new ContainerVmSchedulerTimeSharedOverSubscription(peList)));
        }

        return hostList;
    }



    public static ContainerDatacenter createDatacenter(String name, Class<? extends ContainerDatacenter> datacenterClass,
                                                       List<ContainerHost> hostList,
                                                       ContainerVmAllocationPolicy vmAllocationPolicy,
                                                       ContainerAllocationPolicy containerAllocationPolicy,
                                                       String experimentName, double schedulingInterval, String logAddress, double VMStartupDelay,
                                                       double ContainerStartupDelay) throws Exception {
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0D;
        double cost = 3.0D;
        double costPerMem = 0.05D;
        double costPerStorage = 0.001D;
        double costPerBw = 0.0D;
        ContainerDatacenterCharacteristics characteristics = new
                ContainerDatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage,
                costPerBw);
        ContainerDatacenter datacenter = new ContainerDatacenter(name, characteristics, vmAllocationPolicy,
                containerAllocationPolicy, new LinkedList<>(), schedulingInterval, experimentName, logAddress);

        return datacenter;
    }


    public static List<Container> createContainerList(int brokerId, int containersNumber) {
        ArrayList<Container> containers = new ArrayList<>();

        for (int i = 0; i < containersNumber; ++i) {
            int containerType = i / (int) Math.ceil((double) containersNumber / 3.0D);

            containers.add(new PowerContainer(IDs.pollId(Container.class), brokerId, (double) ConstantsExamples.CONTAINER_MIPS[containerType], ConstantsExamples.
                    CONTAINER_PES[containerType], ConstantsExamples.CONTAINER_RAM[containerType], ConstantsExamples.CONTAINER_BW, 0L, "Xen",
                    new ContainerCloudletSchedulerDynamicWorkload(ConstantsExamples.CONTAINER_MIPS[containerType], ConstantsExamples.CONTAINER_PES[containerType]), ConstantsExamples.SCHEDULING_INTERVAL));
        }

        return containers;
    }


    public static List<ContainerCloudlet> createContainerCloudletList(int brokerId, int numberOfCloudlets) {

        File planetLabFolder = new File("modules/cloudsim-examples/src/main/resources/workload/planetlab/");
        List<ContainerCloudlet> cloudletList = new ArrayList<>();
        long fileSize = 300L;
        long outputSize = 300L;
        UtilizationModelNull utilizationModelNull = new UtilizationModelNull();
        File[] planetLabFolderFiles = planetLabFolder.listFiles();
        int createdCloudlets = 0;
        for (File folder : planetLabFolderFiles) {
            File inputFolder = new File(folder.toString());
            File[] files = inputFolder.listFiles();
            int currentFileIndex = 0;
            while (createdCloudlets < numberOfCloudlets) {
                ContainerCloudlet cloudlet = null;
                try {
                    assert files != null;
                    File file = files[currentFileIndex++];
                    cloudlet = new ContainerCloudlet(IDs.pollId(ContainerCloudlet.class), ConstantsExamples.CLOUDLET_LENGTH, 1,
                            fileSize, outputSize,
                            new UtilizationModelPlanetLabInMemoryExtended(file.getAbsolutePath(), 300.0D),
                            utilizationModelNull, utilizationModelNull);
                } catch (Exception var13) {
                    var13.printStackTrace();
                    System.exit(0);
                }
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
                createdCloudlets += 1;
            }
            return cloudletList;


        }
        return cloudletList;
    }
}
