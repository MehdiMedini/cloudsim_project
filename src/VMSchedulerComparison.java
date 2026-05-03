import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import java.util.*;

public class VMSchedulerComparison {

    public static void main(String[] args) {
        System.out.println("========== VM Scheduling Comparison ==========\n");

        // Run SpaceShared simulation
        runSimulation("SpaceShared", new CloudletSchedulerSpaceShared());

        // Run TimeShared simulation
        runSimulation("TimeShared", new CloudletSchedulerTimeShared());
    }

    private static void runSimulation(String title, CloudletScheduler scheduler) {
        try {
            System.out.println("\n===== Running " + title + " Simulation =====");

            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUser, calendar, false);

            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            DatacenterBroker broker = new DatacenterBroker("Broker");

            // Create VM with selected scheduler
            Vm vm = new Vm(0, broker.getId(), 1000, 1, 512,
                    1000, 10000, "Xen", scheduler);

            List<Vm> vmlist = new ArrayList<>();
            vmlist.add(vm);

            // Create Cloudlets
            List<Cloudlet> cloudletList = new ArrayList<>();
            long length = 40000;
            long fileSize = 300;
            long outputSize = 300;

            UtilizationModel utilizationModel = new UtilizationModelFull();

            for (int i = 0; i < 2; i++) {
                Cloudlet cloudlet = new Cloudlet(i, length, 1,
                        fileSize, outputSize,
                        utilizationModel, utilizationModel, utilizationModel);

                cloudlet.setUserId(broker.getId());
                cloudletList.add(cloudlet);
            }

            broker.submitVmList(vmlist);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();

            List<Cloudlet> results = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printResults(title, results);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(1000)));

        List<Host> hostList = new ArrayList<>();
        hostList.add(new Host(
                0,
                new RamProvisionerSimple(2048),
                new BwProvisionerSimple(10000),
                1000000,
                peList,
                new VmSchedulerTimeShared(peList) // Host-level scheduling
        ));

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        arch, os, vmm, hostList,
                        3.0, 0.0, 0.01, 0.02, 0.05
                );

        try {
            return new Datacenter(name, characteristics,
                    new VmAllocationPolicySimple(hostList),
                    new LinkedList<>(), 0);
        } catch (Exception e) {
            return null;
        }
    }

    private static void printResults(String title, List<Cloudlet> list) {
        System.out.println("\n--- Results for " + title + " ---");
        System.out.println("Cloudlet ID | Status | Time | Start | Finish");

        double totalTime = 0;
        double makespan = 0;

        for (Cloudlet c : list) {
            double time = c.getActualCPUTime();
            double finish = c.getFinishTime();

            totalTime += time;
            makespan = Math.max(makespan, finish);

            System.out.println(
                    c.getCloudletId() + " | SUCCESS | " +
                            time + " | " +
                            c.getExecStartTime() + " | " +
                            finish
            );
        }

        System.out.println("\nTotal Execution Time: " + totalTime);
        System.out.println("Makespan (Total Finish Time): " + makespan);
    }
}