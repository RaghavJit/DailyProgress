# **Proxmox CLI Documentation**

Proxmox provides a robust Command Line Interface (CLI) for managing virtual machines (VMs), storage, and disks. This guide covers essential commands for VM management, storage management, and disk management.

---

## **1. VM Management Commands**

### **1.1 Create VM**  
Creates a new virtual machine with specified options.  
```bash
qm create <vm-id> [OPTIONS]
```
- `<vm-id>`: Unique ID for the VM.  
- **Options**:
  - `--name <vm-name>`: Set the VM name.
  - `--memory <size>`: Set memory size (in MB).
  - `--net0 <model>,bridge=<bridge>`: Configure network interface.
  - `--cdrom <path>`: Attach an ISO image.
  
**Example**:  
```bash
qm create 100 --name ubuntu-vm --memory 2048 --net0 virtio,bridge=vmbr0 --cdrom local:iso/ubuntu.iso
```

---

### **1.2 Destroy VM**  
Deletes a virtual machine and its associated data.  
```bash
qm destroy <vm-id>
```
**Example**:  
```bash
qm destroy 100
```

---

### **1.3 Start VM**  
Starts a virtual machine.  
```bash
qm start <vm-id>
```
**Example**:  
```bash
qm start 100
```

---

### **1.4 Stop VM**  
Stops a virtual machine. There are two types of stops:  

- **Force Stop** (immediate stop):  
  ```bash
  qm stop <vm-id>
  ```

- **Graceful Shutdown**:  
  ```bash
  qm shutdown <vm-id>
  ```

- **Suspend** (save state to disk and stop):  
  ```bash
  qm suspend <vm-id>
  ```

**Examples**:  
```bash
qm stop 100  
qm shutdown 100  
qm suspend 100
```

---

### **1.5 Change VM Variables**  
VM's configuration can be checked using:
```bash
qm config <vm-id>
```

Modifies a VM’s configuration, such as adding disks or changing CPU settings.  
```bash
qm set <vm-id> [OPTIONS]
```
- **Options**:
  - `--cpu <cpu-type>`: Change CPU type.
  - `--scsi0 <storage>:<disk>`: Attach a disk.
  - `--kvm <on|off>`: Enable/disable KVM.

**Example**:  
```bash
qm set 100 --scsi0 local-lvm:10G --cpu host --kvm on
```

---

### **1.6 List All VMs**  
Displays a list of all virtual machines.  
```bash
qm list
```

---

## **2. Storage Management Commands**

### **2.1 List Storages**  
Lists all available storage pools.  
```bash
pvesm status
```

---

### **2.2 Delete Storage**  
Removes a storage pool.  
```bash
pvesm remove <storage-name>
```
**Example**:  
```bash
pvesm remove local-lvm
```

---

### **2.3 Create Storage**  
Creates a new storage pool.  
```bash
pvesm add <type> <storage-name> [OPTIONS]
```
- **Types**: `lvm`, `lvmthin`, `dir`, `nfs`, `zfs`
- **Options**:  
  - `--vgname <volume-group>`: Volume group for LVM.  
  - `--thinpool <pool-name>`: Thin pool name for LVM thin.  
  - `--path <directory>`: Directory path for storage.  

**Examples**:  
```bash
pvesm add lvm my-lvm --vgname vg_data  
pvesm add dir local-storage --path /mnt/storage
```

---

### **2.4 Storage Types**  
- **LVM**: Logical Volume Manager-based storage.  
- **LVM Thin**: Thin-provisioned LVM storage.  
- **Directory (CD/DVD)**: Stores ISOs or files in a directory.

---

## **3. Disk Management Commands**

### **3.1 Import QCOW2 Disk to Storage**  
Imports a QCOW2 disk file to a specified storage.  
```bash
qm importdisk <vm-id> <path-to-disk.qcow2> <storage-name>
```
**Example**:  
```bash
qm importdisk 100 /path/to/disk.qcow2 local-lvm
```

---

### **3.2 List Disks**  
Lists all physical and virtual disks.  
```bash
lsblk
```
OR  
```bash
pvesm list <storage-name>
```

---

### **3.3 Delete Disks**  
Deletes a disk from storage.  
```bash
pvesm free <disk-path>
```
**Example**:  
```bash
pvesm free local:100/vm-100-disk-0.raw
```

---

## **4. Set Storage/Disk for a VM**

Attaches an existing disk or storage to a VM.  
```bash
qm set <vm-id> --<disk-type><slot> <storage>:<disk>
```
- **Disk Types**: `scsi`, `sata`, `ide`, `virtio`  

**Example**:  
```bash
qm set 100 --scsi0 local-lvm:vm-100-disk-0
```

---

This documentation provides essential commands for managing VMs, storage, and disks in Proxmox. Adjust options as needed based on your infrastructure and use case.