# **Day 3 - Proxmox VM Setup and Configuration**  
**Date:** 26.11.2024  

---  

## **Log**  

### VM Creation and Disk Attachment  
- **8:00 AM:** Began setting up a new CentOS VM on Proxmox using the GUI.  
- **8:20 AM:** Created VM with 4GB RAM, 2 cores, and network bridge `vmbr0`.  
- **8:30 AM:** Imported the CentOS `.qcow2` disk:  
   ```bash  
   qm importdisk 101 centos-disk.qcow2 local-lvm  
   ```  
- **8:45 AM:** Attached the imported disk to the VM:  
   ```bash  
   qm set 101 --scsi0 local-lvm:vm-101-disk-0  
   ```  

### VM Configuration  
- **9:00 AM:** Configured VM settings to use `OVMF` firmware and added an EFI disk:  
   ```bash  
   qm set 101 --efidisk0 local-lvm:vm-101-efi,format=qcow2,efitype=4m  
   ```  
- **9:20 AM:** Modified machine type to `q35` and verified settings.  

### VM Boot and Troubleshooting  
- **9:30 AM:** Attempted to boot VM; boot failed with a missing bootloader error.  
- **9:40 AM:** Added a CD-ROM device and uploaded a CentOS ISO:  
   ```bash  
   qm set 101 --cdrom /var/lib/vz/template/iso/centos.iso  
   ```  
- **10:00 AM:** Booted VM using ISO and repaired the bootloader.  
- **10:30 AM:** Successfully booted VM into CentOS.  

### Ubuntu VM Setup  
- **11:00 AM:** Created a new Ubuntu VM using GUI and attached the imported `.qcow2` disk.  
- **11:20 AM:** Configured VM with `SeaBIOS` firmware and machine type `i440fx`.  
- **11:40 AM:** Started VM; encountered kernel panic during boot.  
- **12:00 PM:** Switched firmware to `OVMF` and added EFI disk. Boot successful.  

