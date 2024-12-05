# **Day 7 - Proxmox Installation and VM Migration**  
**Date:** 30.11.2024  

---  

## **Log**  
- **8:00 AM:** Logged into Proxmox and started setup.  
- **8:10 AM:** Transferred `.ova` file to Proxmox using `scp`.  
- **8:20 AM:** Extracted `.ova` and started disk conversion.  
  ```bash  
  tar -xvf sample.ova  
  qemu-img convert -f vmdk sample-disk.vmdk -O qcow2 sample-disk.qcow2  
  ```  
- **8:45 AM:** Conversion completed.  
- **9:00 AM:** Created a VM with initial configuration.  
- **9:20 AM:** Attached the converted `.qcow2` disk to the VM.  
  ```bash  
  qm set 101 --scsi0 local-lvm:vm-101-disk-0  
  ```  
- **9:30 AM:** VM boot failed.  
- **9:40 AM:** Identified incorrect firmware settings; switched to OVMF.  
  ```bash  
  qm set 101 --bios ovmf --efidisk0 local-lvm:vm-101-disk-1  
  ```  
- **10:00 AM:** Created an EFI disk and reattached storage.  
- **10:20 AM:** Successfully booted VM.  
- **10:30 AM:** Performed cleanup and documented steps.  

---  
