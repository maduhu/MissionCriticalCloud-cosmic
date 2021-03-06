package com.cloud.hypervisor;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

import java.util.List;
import java.util.Map;

public interface HypervisorGuru extends Adapter {

    HypervisorType getHypervisorType();

    /**
     * Convert from a virtual machine to the
     * virtual machine that the hypervisor expects.
     *
     * @param vm
     * @return
     */
    VirtualMachineTO implement(VirtualMachineProfile vm);

    /**
     * Give hypervisor guru opportunity to decide if certain command needs to be delegated to other host, mainly to secondary storage VM host
     *
     * @param hostId original hypervisor host
     * @param cmd    command that is going to be sent, hypervisor guru usually needs to register various context objects into the command object
     * @return delegated host id if the command will be delegated
     */
    Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd);

    /**
     * @return true if VM can be migrated independently with CloudStack, and therefore CloudStack needs to track and reflect host change
     * into CloudStack database, false if CloudStack enforces VM sync logic
     */
    boolean trackVmHostChange();

    /**
     * @param profile
     * @return
     */
    NicTO toNicTO(NicProfile profile);

    /**
     * Give hypervisor guru opportunity to decide if certain command needs to be done after expunge VM from DB
     *
     * @param vm
     * @return a list of Commands
     */
    List<Command> finalizeExpunge(VirtualMachine vm);

    /**
     * Give the hypervisor guru the opportinity to decide if additional clean is
     * required for nics before expunging the VM
     */
    List<Command> finalizeExpungeNics(VirtualMachine vm, List<NicProfile> nics);

    List<Command> finalizeExpungeVolumes(VirtualMachine vm);

    Map<String, String> getClusterSettings(long vmId);
}
