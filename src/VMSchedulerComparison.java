import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import java.util.*;

public class VMSchedulerComparison {

    public static void main(String[] args) {
        System.out.println("=== FCFS vs Round Robin Comparison ===");

        // FCFS (SpaceShared)
        runSimulation("FCFS", new CloudletSchedulerSpaceShared());

        // Round Robin (TimeShared)
        runSimulation("Round Robin", new CloudletSchedulerTimeShared());
    }

    private static void runSimulation(String name, CloudletScheduler scheduler) {
        try {
            System.out.println("\n--- " + name + " ---");

            CloudSim.init(1, Calendar.getInstance(), false);

            Datacenter datacenter = createDatacenter();
            DatacenterBroker broker = new DatacenterBroker("Broker");

            int brokerId = broker.getId();

            // 🔹 ONE VM only
            Vm vm = new Vm(0, brokerId, 1000, 1, 512,
                    1000, 10000, "Xen", scheduler);

            List<Vm> vmList = new ArrayList<>();
            vmList.add(vm);
            broker.submitVmList(vmList);

            // 🔹 THREE cloudlets
            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel model = new UtilizationModelFull();

            for (int i = 0; i < 3; i++) {
                Cloudlet cloudlet = new Cloudlet(
                        i,
                        40000, // same length
                        1,
                        300,
                        300,
                        model, model, model
                );
                cloudlet.setUserId(brokerId);

                // Force all tasks to same VM
                cloudlet.setVmId(0);

                cloudletList.add(cloudlet);
            }

            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();
            List<Cloudlet> result = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            printResults(result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter() {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(1000)));

        List<Host> hostList = new ArrayList<>();
        hostList.add(new Host(
                0,
                new RamProvisionerSimple(2048),
                new BwProvisionerSimple(10000),
                1000000,
                peList,
                new VmSchedulerTimeShared(peList)
        ));

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        "x86", "Linux", "Xen",
                        hostList,
                        3.0, 0.0, 0.01, 0.02, 0.05
                );

        try {
            return new Datacenter(
                    "Datacenter",
                    characteristics,
                    new VmAllocationPolicySimple(hostList),
                    new LinkedList<>(),
                    0
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static void printResults(List<Cloudlet> list) {
        System.out.println("ID | Start | Finish | Time");

        for (Cloudlet c : list) {
            System.out.println(
                    c.getCloudletId() + " | " +
                            c.getExecStartTime() + " | " +
                            c.getFinishTime() + " | " +
                            c.getActualCPUTime()
            );
        }
    }
}