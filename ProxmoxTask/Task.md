# **VM Migration from VirtualBox to Proxmox**

## **Task Overview**

The task involved migrating VirtualBox VMs to Proxmox using OVA files. The objective was to import these VMs into Proxmox and ensure they boot up successfully. A script was developed to automate the migration process, making it easy to use, handling failures, and requesting user input to abstract Proxmox commands.

---

### **Task Requirements:**
1. **OVA Files:** Import provided OVA files into Proxmox.
2. **VM Boot-Up:** Ensure the migrated VMs boot up correctly after import.
3. **Automation Script:** 
    - Develop a script to automate the entire process.
    - The script should be user-friendly and handle possible failures.
    - It should prompt the user for input to abstract Proxmox commands for better ease of use.

---

### **Logs for Task Duration:**
- [**Day 1**](./Logs/Day1.md)
- [**Day 2**](./Logs/Day2.md)
- [**Day 3**](./Logs/Day3.md)
- [**Day 4**](./Logs/Day4.md)
- [**Day 5**](./Logs/Day5.md)
- [**Day 6**](./Logs/Day6.md)
- [**Day 7**](./Logs/Day7.md)
- [**Day 8**](./Logs/Day8.md)
- [**Day 9**](./Logs/Day9.md)

---

### **Proxmox Tools Documentation:**
For detailed information on the Proxmox CLI tools used in the migration, please refer to the [**Proxmox CLI Documentation**](./ProxmoxCLI.md).

---

### **Script Documentation:**
To understand the functionality of the migration script, please refer to the [**Script Documentation**](./Script.md).

---

### **Migration Script:**
The script used for automating the migration process is available at the following link:  
[**Migration Script**](./migrate)

---

### **Conclusion:**
The migration task was successfully executed by utilizing Proxmox's powerful tools and automation scripts, simplifying the process of transferring and booting VMs. The script was designed with usability in mind, allowing for an efficient and error-free migration process.