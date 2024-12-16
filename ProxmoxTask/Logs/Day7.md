# **Day 7 - Proxmox Installation and VM Migration**  
**Date:** 30.11.2024  

---  

## **Log**  
- **8:00 PM:** Logged into Proxmox and started setup.  
- **8:10 PM:** Transferred `.ova` file to Proxmox using `scp`.  
- **8:20 PM:** Extracted `.ova` and started disk conversion.  
  ```bash  
  tar -xvf sample.ova  
  qemu-img convert -f vmdk sample-disk.vmdk -O qcow2 sample-disk.qcow2  
  ```  
- **8:45 PM:** Conversion completed.  
- **9:00 PM:** Created a VM with initial configuration.  
- **9:20 PM:** Attached the converted `.qcow2` disk to the VM.  
  ```bash  
  qm set 101 --scsi0 local-lvm:vm-101-disk-0  
  ```  
- **9:30 PM:** VM boot failed.  
- **9:40 PM:** Identified incorrect firmware settings; switched to OVMF.  
  ```bash  
  qm set 101 --bios ovmf --efidisk0 local-lvm:vm-101-disk-1  
  ```  
- **10:00 PM:** Created an EFI disk and reattached storage.  
- **10:20 PM:** Successfully booted VM.  
- **10:30 PM:** Performed cleanup and documented steps.  

---  
