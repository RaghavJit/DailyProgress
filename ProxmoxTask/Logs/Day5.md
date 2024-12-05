# **Day 3 - Proxmox Installation and VM Migration**  

**Date:** 28.11.2024  

---  

## **Log**  

### **Setup and Migration**  
- **8:00 AM:** Logged in and initialized the setup.  
- **8:15 AM:** Started running `migrate.sh` for VM migration.  
- **9:00 AM:** Completed debugging; the VM is now running successfully.  

### **Issue Identification and Correction**  
- **9:10 AM:** Encountered an issue where the disk was not attaching to the VM.  
- **9:15 AM:** Identified incorrect boot order as the root cause.  
- **9:30 AM:** Re-ran `migrate.sh` and corrected mistakes using the Proxmox GUI.  

### **Testing and Script Refinement**  
- **9:45 AM:** Began noting test cases for further improvements.  
- **10:30 AM:** Completed the basic version of the script, though further debugging is still needed.  
- **11:45 AM:** Continued refining the script and testing for edge cases.  

---  

## **Key Insights**  
1. **Boot Order Management:** Ensuring the correct boot order is essential for proper VM functionality.  
2. **Iterative Testing:** Regular testing and refining of the script help in identifying and resolving issues effectively.  