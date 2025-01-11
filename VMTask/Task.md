# **Assigning IPv6 Static to VirtualBox VM**

## **Task Overview**
The task was to create some virtual machines on the `yamuna.fossee.in` server using VirtualBox. Then, make those VMs globally accessible using IPv6. 

---

### **Task Requirements:**
1. Hosting services on IPv6 should be possible.
2. Securely SSH into the server using IPv6.
3. Apply standard FOSSEE `firewalld` rules for IP whitelisting.

---

### **Assign IPV6 to VM**
The process involved creating a VirtualBox VM, configuring a host-only network (vmbr0) for initial IP allocation, and enabling SSH access. A bridged network was then set up on a secondary interface to assign a static IPv6 address manually, ensuring global accessibility. Finally, a NAT connection on the primary interface provided IPv4 connectivity for internet access. [Process](./Process.md)

---

### **IP white listing**
Next step in the task was to configure the firewall such that only certain services and subnets are able to access our machine. The reduces Attack Surface and make our servers more secure. I was not able to complete this part of the task. Refer to [this](./WhiteListing.md) for my findings.

---

### **Conclusion:**  
The task was successfully executed by leveraging VirtualBox's powerful tools and automation scripts. The process involved efficiently setting up IPv6 addresses for VMs and ensuring seamless connectivity, making the VMs globally accessible while adhering to FOSSEE security standards.

However when [firewall rule](./FirewallRules/) were applied, they interfered with networking and blocked all incoming traffic.

I recommend these options:
1. **KVM/QEMU:** Easy to use and more powerful cmd.
1. **Proxmox:** Has a convenient to use WebUI along with a more powerful cmd, it also has the feature to make globally accessible VMs.
