# OVA to Proxmox Automation Script Documentation

A script that automates the process of importing OVA files into Proxmox by extracting the contents, converting disk formats, creating storage, and attaching the storage to a new virtual machine (VM).

---

## Reqirements and limitations
1. Script is written in bash, it may not work as desired, in other shells.
1. [fzf](https://github.com/junegunn/fzf/blob/master/README.md), [awk](https://www.gnu.org/software/gawk/manual/gawk.html), [cut](https://man7.org/linux/man-pages/man1/cut.1.html), [tail](https://man7.org/linux/man-pages/man1/tail.1.html) should be installed.
1. Avoid running multiple instances of script at once.
1. Avoid making changes in web interface while script is running.

---

## Script Overview

The script follows these major steps:

1. **OVA Extraction**  
2. **Disk Conversion to QCOW2 Format**  
3. **Storage Creation or Selection**  
4. **VM Creation**  
5. **Disk Attachment to the VM**  

If any step fails, the script offers cleanup options to remove the extracted files, disk, or VM.  

---

## Usage  

```bash
./script.sh [-v] <file>
```

- `-v`: Enable verbose mode for detailed output.  
- `<file>`: Path to the OVA or QCOW2 file.

Example:  
```bash
./script.sh -v myvm.ova
```

---

## Process Details  

### 1. OVA Extraction  

- **Why**: OVA files contain virtual disk files (usually VMDK) that need to be extracted before use in Proxmox.  
- **Command**:  
   ```bash
   tar -xf <ova-file> -C <target-folder>
   ```
- **Script Reference**: The script extracts the OVA to a user-specified folder using the `extractOVA` function. It prompts the user to confirm overwriting if a VMDK file already exists.

Refer to **VM Management** section: [Extract OVA](#vm-management).

---

### 2. Disk Conversion to QCOW2  

- **Why**: Proxmox uses QCOW2 as its preferred disk format for features like snapshots.  
- **Command**:  
   ```bash
   qemu-img convert -f vmdk -O qcow2 <input-vmdk> <output-qcow2>
   ```
- **Script Reference**: The `convertQCOW` function handles the conversion from VMDK to QCOW2, skipping this step if the input file is already in QCOW format.

Refer to **Disk Management** section: [Convert Disk Format](#disk-management).

---

### 3. Storage Creation or Selection  

- **Why**: Storage is required to hold virtual disks. The script can create a new LVM-Thin storage or allow the user to select an existing storage.  
- **Command to Create Storage**:  
   ```bash
   pvesm add lvmthin <storage-name> --vgname <vg-name> --thinpool <thinpool-name> --content images
   ```
- **Script Reference**: The `createStorage` function interacts with the user to either create a new storage or select an existing one using `fzf`.

Refer to **Storage Management** section: [Create Storage](#storage-management).

---

### 4. VM Creation  

- **Why**: After obtaining the QCOW2 file, a VM must be created in Proxmox to utilize the disk.  
- **Command**:  
   ```bash
   qm create <vm-id> --name <vm-name> --memory <memory-size> --cores <cpu-cores> --net0 virtio,bridge=vmbr0 --scsihw virtio-scsi-pci --boot order=scsi0
   ```
- **UEFI vs SeaBIOS**: The script defaults to SeaBIOS for compatibility, though UEFI can be specified by adjusting VM options.

Refer to **VM Management** section: [Create VM](#vm-management).

---

### 5. Attach Disk to VM  

- **Why**: The disk needs to be attached to the VM after the QCOW2 conversion.  
- **Command**:  
   ```bash
   qm set <vm-id> --scsi0 <storage>:<disk>,size=<size>
   ```
- **Script Reference**: The `attachStorage` function lists available disks from the storage and attaches the selected disk to the VM.

Refer to **VM Management** section: [Attach Disk](#vm-management).

---

## Cleanup and Error Handling  

The script offers cleanup options in case of failures, including deleting the extracted files, disks, and VMs. The `handleFailure` function prompts the user to confirm these actions.

Refer to **Disk Management** section: [Delete Disk](#disk-management).

---

## Notes  

- **Verbose Mode**: Use `-v` to enable detailed logging for debugging purposes.  
- **Error Handling**: The script gracefully handles errors and provides options to undo changes.