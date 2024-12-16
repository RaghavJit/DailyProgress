# **Day 2 - Proxmox VM Setup and Migration**  
**Date:** 25.11.2024  

---  

## **Log**  

### VM Disk Conversion and Transfer  
- **8:00 PM:** Started extracting and converting Red Hat `.ova` file.  
  ```bash  
  tar -xvf rhel.ova  
  ```  
- **8:10 PM:** Converted `.vmdk` to `.qcow2` format using `qemu-img`.  
  ```bash  
  qemu-img convert -f vmdk rhel-disk.vmdk -O qcow2 rhel-disk.qcow2  
  ```  
- **8:20 PM:** Conversion completed successfully.  
- **8:30 PM:** Transferred Ubuntu ISO to Proxmox via `scp`.  
- **8:55 PM:** Completed Ubuntu disk extraction and conversion process.  
  ```bash  
  tar -xvf ubuntu.ova  
  qemu-img convert -f vmdk ubuntu-disk.vmdk -O qcow2 ubuntu-disk.qcow2  
  ```  

### RHEL VM Migration and Configuration  
- **9:00 PM:** Built a new RHEL VM in VirtualBox and exported as `.ova`.  
- **9:20 PM:** Imported the RHEL disk into Proxmox.  
  ```bash  
  qm importdisk 100 rhel-disk.qcow2 custom --format qcow2  
  ```  
- **10:00 PM:** Created the RHEL VM in Proxmox using the GUI.  
- **10:20 PM:** Attached the imported CentOS disk to the new VM.  
  ```bash  
  qm set 100 --scsi0 custom:vm-100-disk-0  
  ```  
- **10:25 PM:** Attempted to start the VM, but it failed to boot.  
- **10:45 PM:** Made multiple configuration changes (firmware, machine type).  
- **11:00 PM:** Successfully booted RHEL VM after adjustments.  

### VM Rebuild and Cleanup  
- **11:20 PM:** Destroyed existing VM and removed attached storage.  
  ```bash  
  qm destroy 100  
  ```  
- **11:25 PM:** Recreated the VM and re-imported the disk.  
  ```bash  
  qm importdisk 101 ubuntu-disk.qcow2 local-lvm  
  ```  
- **11:45 PM:** Attached the Ubuntu disk and configured the VM.  
- **12:00 PM:** Ubuntu VM failed to boot.  
