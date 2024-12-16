# **Day 8 - Proxmox Installation and VM Migration**  
**Date:** 1.12.2024  

---  

## **Log**  
- **8:00 PM:** Logged into Proxmox.  
- **8:10 PM:** Started setup process.  
- **8:15 PM:** Transferred `.ova` file to Proxmox via `scp`.  
- **8:20 PM:** Ran script to import the `.ova`.  
- **8:40 PM:** Conversion to `.qcow2` failed due to a typo.  
- **8:45 PM:** Corrected the typo and restarted the process.  
- **9:15 PM:** Conversion completed; disk imported successfully.  
- **9:20 PM:** Created VM and attached disk.  
- **9:35 PM:** VM boot failed.  
- **9:40 PM:** Identified incorrect disk attachment as the issue.  
- **9:50 PM:** Deleted disks and imported a corrected `.qcow2`.  
- **10:15 PM:** Script failed again due to incorrect VM creation order.  
- **10:30 PM:** Corrected the order and restarted the script.  
- **10:45 PM:** Script failed due to wrong disk attachment.  
- **11:00 PM:** Deleted all disks, storage, and VMs. Restarted script from scratch.  
- **11:15 PM:** Script failed again; `.qcow2` format was incorrect.  
- **11:20 PM:** Deleted files and added a failsafe to the script.  
- **11:25 PM:** Ran the script again.  
- **11:35 PM:** Script completed successfully.  
- **11:40 PM:** Added a menu option in the script for disk selection.  

