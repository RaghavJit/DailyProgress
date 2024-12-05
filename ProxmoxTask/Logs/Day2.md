# **Day 2 - Proxmox VM Setup and Migration**  
**Date:** 25.11.2024  

---  

## **Log**  

### VM Disk Conversion and Transfer  
- **8:00 AM:** Started extracting and converting Red Hat `.ova` file.  
  ```bash  
  tar -xvf rhel.ova  
  ```  
- **8:10 AM:** Converted `.vmdk` to `.qcow2` format using `qemu-img`.  
  ```bash  
  qemu-img convert -f vmdk rhel-disk.vmdk -O qcow2 rhel-disk.qcow2  
  ```  
- **8:20 AM:** Conversion completed successfully.  
- **8:30 AM:** Transferred Ubuntu ISO to Proxmox via `scp`.  
- **8:55 AM:** Completed Ubuntu disk extraction and conversion process.  
  ```bash  
  tar -xvf ubuntu.ova  
  qemu-img convert -f vmdk ubuntu-disk.vmdk -O qcow2 ubuntu-disk.qcow2  
  ```  

### RHEL VM Migration and Configuration  
- **9:00 AM:** Built a new RHEL VM in VirtualBox and exported as `.ova`.  
- **9:20 AM:** Imported the RHEL disk into Proxmox.  
  ```bash  
  qm importdisk 100 rhel-disk.qcow2 custom --format qcow2  
  ```  
- **10:00 AM:** Created the RHEL VM in Proxmox using the GUI.  
- **10:20 AM:** Attached the imported CentOS disk to the new VM.  
  ```bash  
  qm set 100 --scsi0 custom:vm-100-disk-0  
  ```  
- **10:25 AM:** Attempted to start the VM, but it failed to boot.  
- **10:45 AM:** Made multiple configuration changes (firmware, machine type).  
- **11:00 AM:** Successfully booted RHEL VM after adjustments.  

### VM Rebuild and Cleanup  
- **11:20 AM:** Destroyed existing VM and removed attached storage.  
  ```bash  
  qm destroy 100  
  ```  
- **11:25 AM:** Recreated the VM and re-imported the disk.  
  ```bash  
  qm importdisk 101 ubuntu-disk.qcow2 local-lvm  
  ```  
- **11:45 AM:** Attached the Ubuntu disk and configured the VM.  
- **12:00 PM:** Ubuntu VM failed to boot.  
