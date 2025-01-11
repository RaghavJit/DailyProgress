## How to assign IPV6 static to VM

1. **Creating the VM:**  
   The first step was to create a VM on a local machine where RHEL could be installed. After the VM was created, the following points were noted:  
   - Set a password for SSH login.  
   - Disable hardware nesting and nested paging.  
   - Ensure the firewall is turned off.  

2. **Importing the VM:**  
   To import the machine, the following command was used:  
   ```bash
   vbm import RHEL.ova --vsys 0 --name "RHEL_1"
   ```

3. **Assigning an IP to the VM:**  
   To SSH into the VM, its IP was needed. The method used was to create a host-only network (`vmbr0`) connection and then obtain the IP to SSH.  
   ```bash
   vbm hostonlyif create
   ```
   This created a `vboxnet0` host-only network with the subnet `192.168.56.0/24`.  
   ```bash
   vbm modifyvm <VM-NAME> --nic<interface number> hostonly --hostonlyadapter<interface number> <interface with DHCP>
   vbm modifyvm RHEL_1 --nic1 hostonly --hostonlyadapter1 vmbr0
   ```

   To obtain the IP, the following methods were used:
   ```bash
   nmap -sn 192.168.56.0/24 
   # or
   cat ~/.config/VirtualBox/HostInterfaceNetworking-vboxnet0-Dhcpd.leases
   ```

   > **Note:** VirtualBox resolves DHCP only on the first interface; all other interfaces must be manually activated using `dhclient`.  

   ```bash
   dhclient <interface name>
   # or
   dhclient -6 <interface name>
   ```

4. **Creating a Bridged Network:**  
   To assign an IPv6 to the VM (via DHCP), a bridged interface was created. This interface made the VM behave as if it was connected to the same DHCP server as the host machine.  
   ```bash
   vbm modifyvm <VM-NAME> --nic<interface number> bridged --bridgeadapter<interface number> <host interface>
   vbm modifyvm RHEL_1 --nic2 bridged --bridgeadapter2 enp3efdo9
   ```
   > **Note:** The bridged connection was configured on `nic2` and not `nic1`. The `nic1` in host-only mode obtained an IP, enabling SSH access to the VM.

5. **Assigning IPv6:**  
   Since the bridged connection was on `nic2`, it did not automatically obtain an IP. An IPv6 address was manually assigned. The host (`yamuna.fossee.in`) had IPv6 `2a01:4f9:3080:2d4d::2`, so an IP in the same subnet was assigned to the VM (e.g., `2a01:4f9:3080:2d4d::5`). This was done using either `nmtui` (recommended) or the `ip` command.  
   ```text
   Address: 2a01:4f9:3080:2d4d::5/64
   Gateway: 2a01:4f9:3080:2d4d::2
   DNS1: 2001:4860:4860::8888
   DNS2: 2606:4700:4700::1111
   ```
   Using [ip](https://man7.org/linux/man-pages/man8/ip.8.html) command
   ```
   sudo ip -6 addr add 2a01:4f9:3080:2d4d::5/64 dev enp0s8

   sudo ip -6 route add default via 2a01:4f9:3080:2d4d::2 dev enp0s8

   echo "nameserver 2001:4860:4860::8888" | sudo tee -a /etc/resolv.conf
   echo "nameserver 2606:4700:4700::1111" | sudo tee -a /etc/resolv.conf
   ```

6. **Enabling Internet Access:**  
   Although the VM now had a globally accessible IPv6 address, many services (e.g., `dnf`) did not support IPv6. Therefore, an IPv4 was assigned for internet access.  
   ```bash
   vbm controlvm RHEL_1 acpipowerbutton
   vbm modifyvm RHEL_1 --nic1 NAT
   ```
   > **Note:** NAT was configured on `nic1` for proper functioning.
