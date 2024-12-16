# **Day 3 - Proxmox Installation and VM Migration**  
**Date:** 29.11.2024  

---  

## **Log**  
- **8:00 PM:** Logged into Proxmox and initiated setup.  
- **8:10 PM:** Started running the extraction script.  
  ```bash  
  tar -xvf sample.ova  
  ```  
- **8:20 PM:** Ran the disk conversion script.  
  ```bash  
  qemu-img convert -f vmdk sample-disk.vmdk -O qcow2 sample-disk.qcow2  
  ```  
- **8:30 PM:** Attempted to create storage using a script.  
- **8:40 PM:** Storage creation script failed.  
- **8:45 PM:** Debugged and found the issue: **Thinpool option** not set correctly.  
  ```bash  
  lvcreate --type thin-pool --name vm_storage_pool vg0  
  ```  
- **9:00 PM:** Debugging completed; storage creation successful.  
- **10:00 PM:** Ran the VM creation script.  
- **10:10 PM:** Incorrect disk mounted; deleted VM.  
  ```bash  
  qm stop 100 && qm destroy 100  
  ```  
- **10:12 PM:** Started the script again without specifying `scsi0`.  
- **10:20 PM:** Power outage occurred; Proxmox server switched off.  
- **10:45 PM:** Power restored.  
- **10:50 PM:** Resumed setup process.  
- **10:55 PM:** Ran the script again.  
- **11:20 PM:** Added storage successfully.  
- **11:30 PM:** Imported `.qcow2` and attached storage, but selected the wrong disk.  
- **11:45 PM:** VM failed to start; **KVM flag** was unset due to sudden power-off resetting virtualization settings in BIOS.  
  ```bash  
  qm set 100 --cpu host  
  ```  
