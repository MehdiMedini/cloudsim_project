import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import java.util.*;

public class VMSchedulerComparison {
    public static void main(String[] args) {
        try {
            // 1. Initialize CloudSim
            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUser, calendar, false);

            // 2. Create Datacenter and Broker
            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            DatacenterBroker broker = new DatacenterBroker("Broker");

            // 3. Create VMs with DIFFERENT Schedulers to compare
            // VM 0 uses SpaceShared (FCFS logic)
            Vm vm1 = new Vm(0, broker.getId(), 1000, 1, 512, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared());

            // 4. Create Tasks (Cloudlets)
            List<Cloudlet> cloudletList = new ArrayList<>();
            long length = 40000; // Total instructions
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            for(int i=0; i<2; i++) {
                Cloudlet cloudlet = new Cloudlet(i, length, 1, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(broker.getId());
                cloudletList.add(cloudlet);
            }

            // 5. Submit to Broker and Start
            List<Vm> vmlist = new ArrayList<>();
            vmlist.add(vm1);
            broker.submitVmList(vmlist);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // 6. Print Results
            printCloudletList(newList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to create the environment
    private static Datacenter createDatacenter(String name) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(1000)));

        List<Host> hostList = new ArrayList<>();
        hostList.add(new Host(0, new RamProvisionerSimple(2048), new BwProvisionerSimple(10000), 1000000, peList, new VmSchedulerTimeShared(peList)));

        String arch = "x86"; String os = "Linux"; String vmm = "Xen";
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, 3.0, 0.0, 0.01, 0.02, 0.05);

        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) { return null; }
    }

    private static void printCloudletList(List<Cloudlet> list) {
        System.out.println("Cloudlet ID | Status | Time | Start Time | Finish Time");
        for (Cloudlet c : list) {
            System.out.println(c.getCloudletId() + " | SUCCESS | " + c.getActualCPUTime() + " | " + c.getExecStartTime() + " | " + c.getFinishTime());
        }
    }
}