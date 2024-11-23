# **Day 1 - Proxmox Installation and VM Migration**

**Date:** 11.23.2024

---

## **Log**

### **Proxmox Installation**
- **7:00 AM:** Logged in and started Proxmox setup.
- **7:20 AM:** Installed Proxmox on the desktop.
- **7:30 AM:** Encountered a kernel panic issue after the installation.
- **7:45 AM:** Resolved the issue by changing the Proxmox version and reinstalling. Proxmox is now running.

  **Steps Followed:**
  1. Re-downloaded the Proxmox ISO for a different version.
  2. Reinstalled Proxmox following the official guide:
     - [Proxmox VE Installation Guide](https://pve.proxmox.com/wiki/Installation)

---

### **Virtual Machine Migration**
- **8:00 AM:** Copied the `.ova` file from the laptop to the Proxmox server.
- **9:00 AM:** Extracted the `.ova` file to obtain the `.vmdk` file.
  ```bash
  tar -xvf Chat.Scipy.In.ova
  ```
- **9:10 AM:** Converted the `.vmdk` file to `.qcow2` format using `qemu-img`:
  ```bash
  qemu-img convert -f vmdk -O qcow2 Chat.Scipy.In.vmdk test.chat.scipy.qcow2
  ```
- **9:20 AM:** Created a test VM using `qm create` can also be done in GUI (recommended):
  ```bash
  qm create 100 --name fossee --memory 4096 --cores 2 --net0 virtio,bridge=vmbr0
  ```
- **9:30 AM:** Imported the `.qcow2` file into the VM:
  ```bash
  qm importdisk 100 test.chat.scipy.qcow2 local-lvm
  ```
- **9:35 AM:** Started the VM:
  ```bash
  qm start 100
  ```
  **Issue:** VM did not boot successfully.

---

### **Troubleshooting**
- **9:40 AM:** Attempted to replace machine types (`q35`, `i440fx`) and firmware (`SeaBIOS`, `OVMF`).
- **9:45 AM:** Found that OVMF required an EFI disk, so created one:
  ```bash
  qm set 100 --efidisk0 local-qcow2:0,format=qcow2,efitype=4m
  ```
- **10:00 AM:** Created a new VM with modified configuration to match the source VM's settings:
  ```bash
  qm create 100 --name fossee --memory 4096 --cores 2 --net0 virtio,bridge=vmbr0
  ```
- **10:15 AM:** Re-imported the `.qcow2` file to the new VM:
  ```bash
  qm importdisk 100 test.chat.scipy.qcow2 local-lvm
  ```
- **10:30 AM:** Exported a test VM from VirtualBox back to an `.ova` and repeated the process.
- **10:40 AM:** Uploaded an ISO file to Proxmox and used it to create a VM:
  ```bash
  qm create 100 --name ISOTestVM --memory 2048 --cores 2 --net0 virtio,bridge=vmbr0 --cdrom /var/lib/vz/template/iso/centos.iso --disk 0,format=raw,size=32G
  ```
  **Result:** Successfully created and booted the VM from the ISO.

- **10:50 AM:** Exported a new `.ova` file from VirtualBox, copied it to Proxmox, and repeated the migration process. This time it worked successfully.

---

## **Documentation**

### **Key Commands Used**
1. **Create a VM**:
   ```bash
   qm create <vmid> --name <vm_name> --memory <RAM_size> --cores <num_cores> --net0 virtio,bridge=vmbr0
   ```
   Example:
   ```bash
   qm create 100 --name TestVM --memory 2048 --cores 2 --net0 virtio,bridge=vmbr0
   ```

2. **Import Disk**:
   ```bash
   qm importdisk <vmid> <path_to_disk> <storage_name>
   ```
   Example:
   ```bash
   qm importdisk 100 my_vm-disk.qcow2 local-lvm
   ```

3. **Set Disk Configuration**:
   ```bash
   qm set <vmid> --scsi0 <storage>:<disk_id>,format=raw
   ```

4. **Create EFI Disk** (for OVMF firmware):
   ```bash
   qm set <vmid> --efidisk0 <storage>:<disk_id>,format=qcow,efitype=4m
   ```

5. **Start the VM**:
   ```bash
   qm start <vmid>
   ```

6. **Stop the VM**:
   ```bash
   qm stop <vmid>
   ```

7. **Destroy the VM**:
   ```bash
   qm destroy <vmid>
   ```

8. **List VMs**:
   ```bash
   qm list
   ```

---

## **Lessons Learned**
1. **Default VirtualBox Settings for `.ova` Files**:
   - VirtualBox OVAs often use specific configurations:
     - BIOS/UEFI firmware
     - Machine type (chipset: `i440fx` or `q35`)
   - Ensure these are correctly set in Proxmox for successful migration.

2. **Alternate VM Migration Strategies**:
   - Export as `.ova` and adjust settings in Proxmox.
   - Directly use `.iso` for creating a VM and configure it from scratch.

3. **Importance of Configuration Matching**:
   - Matching VM hardware settings (e.g., firmware, disk format, machine type) to the source environment is crucial.

---

## **Tasks for Tomorrow**
1. Research a proper method for exporting CentOS-based VMs from VirtualBox for migration.
2. Explore alternate ways to migrate VMs from VirtualBox to Proxmox (e.g., `qemu-img` conversion, direct import methods).