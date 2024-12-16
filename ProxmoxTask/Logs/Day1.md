# **Day 1 - Proxmox Installation and VM Migration**

**Date:** 24.11.2024

---

## **Log**

### **Proxmox Installation**
- **7:00 PM:** Logged in and started Proxmox setup.
- **7:20 PM:** Installed Proxmox on the desktop.
- **7:30 PM:** Encountered a kernel panic issue after the installation.
- **7:45 PM:** Resolved the issue by changing the Proxmox version and reinstalling. Proxmox is now running.

  **Steps Followed:**
  1. Re-downloaded the Proxmox ISO for a different version.
  2. Reinstalled Proxmox refered this:
     - [Video](https://www.youtube.com/watch?v=ATd2fzgLN5g)
     - [Docs](https://pve.proxmox.com/pve-docs/qm.1.html)
     - [Article](https://credibledev.com/import-virtualbox-and-virt-manager-vms-to-proxmox/)
     - [Helpful Reddit Thread](https://www.reddit.com/r/Proxmox/comments/195db77/importing_virtual_box_vm_to_proxmox/)

---

### **Virtual Machine Migration**
- **8:00 PM:** Copied the `.ova` file from the laptop to the Proxmox server.
- **9:00 PM:** Extracted the `.ova` file to obtain the `.vmdk` file.
  ```bash
  tar -xvf Chat.Scipy.In.ova
  ```
- **9:10 PM:** Converted the `.vmdk` file to `.qcow2` format using `qemu-img`:
  ```bash
  qemu-img convert -f vmdk -O qcow2 Chat.Scipy.In.vmdk test.chat.scipy.qcow2
  ```
- **9:20 PM:** Created a test VM using `qm create` can also be done in GUI (recommended):
  ```bash
  qm create 100 --name fossee --memory 4096 --cores 2 --net0 virtio,bridge=vmbr0
  ```
- **9:30 PM:** Imported the `.qcow2` file into the VM:
  ```bash
  qm importdisk 100 test.chat.scipy.qcow2 local-lvm
  ```
- **9:35 PM:** Started the VM:
  ```bash
  qm start 100
  ```
  **Issue:** VM did not boot successfully.

---

### **Troubleshooting**
- **9:40 PM:** Attempted to replace machine types (`q35`, `i440fx`) and firmware (`SeaBIOS`, `OVMF`).
- **9:45 PM:** Found that OVMF required an EFI disk, so created one:
  ```bash
  qm set 100 --efidisk0 local-qcow2:0,format=qcow2,efitype=4m
  ```
- **10:00 PM:** Created a new VM with modified configuration to match the source VM's settings:
  ```bash
  qm create 100 --name fossee --memory 4096 --cores 2 --net0 virtio,bridge=vmbr0
  ```
- **10:15 PM:** Re-imported the `.qcow2` file to the new VM:
  ```bash
  qm importdisk 100 test.chat.scipy.qcow2 local-lvm
  ```
- **10:30 PM:** Exported a test VM from VirtualBox back to an `.ova` and repeated the process.
- **10:40 PM:** Uploaded an ISO file to Proxmox and used it to create a VM:
  ```bash
  qm create 100 --name ISOTestVM --memory 2048 --cores 2 --net0 virtio,bridge=vmbr0 --cdrom /var/lib/vz/template/iso/centos.iso --disk 0,format=raw,size=32G
  ```
  **Result:** Successfully created and booted the VM from the ISO.

- **10:50 PM:** Exported a new `.ova` file from VirtualBox, copied it to Proxmox, and repeated the migration process. This time it worked successfully.

---

## **Documentation**

### **Key Commands Used**
1. **Create a VM**:
   ```bash
   qm create <vmid> --name <vm_name> --memory <RPM_size> --cores <num_cores> --net0 virtio,bridge=vmbr0
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
---

## **Tasks for Tomorrow**
1. Research a proper method for exporting CentOS-based VMs from VirtualBox for migration.
2. Explore alternate ways to migrate VMs from VirtualBox to Proxmox (e.g., `qemu-img` conversion, direct import methods).