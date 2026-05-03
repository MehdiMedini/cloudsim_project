import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import java.util.*;

public class VMSchedulerComparison {

    public static void main(String[] args) {
        System.out.println("===== REALISTIC VM SCHEDULING COMPARISON =====");

        runSimulation("SpaceShared", new CloudletSchedulerSpaceShared());
        runSimulation("TimeShared", new CloudletSchedulerTimeShared());
    }

    private static void runSimulation(String title, CloudletScheduler scheduler) {
        try {
            System.out.println("\n=== " + title + " ===");

            CloudSim.init(1, Calendar.getInstance(), false);

            Datacenter datacenter = createDatacenter("Datacenter_0");
            DatacenterBroker broker = new DatacenterBroker("Broker");

            int brokerId = broker.getId();

            // 🔹 Create multiple VMs (heterogeneous)
            List<Vm> vmList = new ArrayList<>();

            vmList.add(new Vm(0, brokerId, 500, 1, 512, 1000, 10000, "Xen", scheduler));
            vmList.add(new Vm(1, brokerId, 1000, 1, 1024, 1000, 10000, "Xen", scheduler));
            vmList.add(new Vm(2, brokerId, 2000, 2, 2048, 2000, 20000, "Xen", scheduler));

            broker.submitVmList(vmList);

            // 🔹 Create realistic workload (10 cloudlets)
            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel model = new UtilizationModelFull();

            long[] lengths = {20000, 40000, 10000, 30000, 50000, 25000, 35000, 15000, 45000, 20000};

            for (int i = 0; i < lengths.length; i++) {
                Cloudlet cloudlet = new Cloudlet(
                        i,
                        lengths[i],
                        1,
                        300,
                        300,
                        model, model, model
                );

                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();

            List<Cloudlet> results = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printResults(title, results);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 More realistic datacenter (multiple hosts)
    private static Datacenter createDatacenter(String name) {

        List<Host> hostList = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            List<Pe> peList = new ArrayList<>();

            // Each host has 2 CPUs
            peList.add(new Pe(0, new PeProvisionerSimple(1000)));
            peList.add(new Pe(1, new PeProvisionerSimple(1000)));

            hostList.add(new Host(
                    i,
                    new RamProvisionerSimple(4096),
                    new BwProvisionerSimple(10000),
                    1000000,
                    peList,
                    new VmSchedulerTimeShared(peList)
            ));
        }

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        "x86", "Linux", "Xen",
                        hostList,
                        3.0, 0.0, 0.01, 0.02, 0.05
                );

        try {
            return new Datacenter(
                    name,
                    characteristics,
                    new VmAllocationPolicySimple(hostList),
                    new LinkedList<>(),
                    0
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static void printResults(String title, List<Cloudlet> list) {
        System.out.println("\nResults (" + title + ")");
        System.out.println("ID | Time | Start | Finish | Waiting");

        double totalWaiting = 0;
        double makespan = 0;

        for (Cloudlet c : list) {
            double waiting = c.getExecStartTime();
            double finish = c.getFinishTime();

            totalWaiting += waiting;
            makespan = Math.max(makespan, finish);

            System.out.println(
                    c.getCloudletId() + " | " +
                            c.getActualCPUTime() + " | " +
                            c.getExecStartTime() + " | " +
                            finish + " | " +
                            waiting
            );
        }

        double avgWaiting = totalWaiting / list.size();
        double throughput = list.size() / makespan;

        System.out.println("\n📊 Metrics:");
        System.out.println("Makespan: " + makespan);
        System.out.println("Average Waiting Time: " + avgWaiting);
        System.out.println("Throughput (tasks/sec): " + throughput);
    }
}