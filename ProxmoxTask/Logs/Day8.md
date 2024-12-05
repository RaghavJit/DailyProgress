# **Day 8 - Proxmox Installation and VM Migration**  
**Date:** 1.12.2024  

---  

## **Log**  
- **8:00 AM:** Logged into Proxmox.  
- **8:10 AM:** Started setup process.  
- **8:15 AM:** Transferred `.ova` file to Proxmox via `scp`.  
- **8:20 AM:** Ran script to import the `.ova`.  
- **8:40 AM:** Conversion to `.qcow2` failed due to a typo.  
- **8:45 AM:** Corrected the typo and restarted the process.  
- **9:15 AM:** Conversion completed; disk imported successfully.  
- **9:20 AM:** Created VM and attached disk.  
- **9:35 AM:** VM boot failed.  
- **9:40 AM:** Identified incorrect disk attachment as the issue.  
- **9:50 AM:** Deleted disks and imported a corrected `.qcow2`.  
- **10:15 AM:** Script failed again due to incorrect VM creation order.  
- **10:30 AM:** Corrected the order and restarted the script.  
- **10:45 AM:** Script failed due to wrong disk attachment.  
- **11:00 AM:** Deleted all disks, storage, and VMs. Restarted script from scratch.  
- **11:15 AM:** Script failed again; `.qcow2` format was incorrect.  
- **11:20 AM:** Deleted files and added a failsafe to the script.  
- **11:25 AM:** Ran the script again.  
- **11:35 AM:** Script completed successfully.  
- **11:40 AM:** Added a menu option in the script for disk selection.  

