# **Day 9 - Proxmox Installation and VM Migration**  

**Date:** 02.12.2024  

---  

## **Log**  

### **Setup and Script Enhancements**  
- **8:00 PM:** Logged in and set up the environment.  
- **8:15 PM:** Reviewed concepts of volume groups, LVM, and data pools.  
- **8:20 PM:** Added fail-safe mechanisms to the scripts to handle errors gracefully.  

### **Testing and Debugging**  
- **9:30 PM:** Implemented storage selection and safe input handling; initiated script testing.  
- **9:40 PM:** Test failed due to a typo in the variable name.  
- **9:55 PM:** Test failed again; identified and corrected a logic error in the condition check.  

### **Feature Development**  
- **10:00 PM:** Added a feature to allow disk selection during VM creation.  
- **10:30 PM:** Test failed; found an extra space in the command, causing the VM not to boot.  
- **10:45 PM:** Fixed the command and successfully booted the VM.  

### **Final Enhancements**  
- **11:45 PM:** Introduced safe input options with confirmation prompts. Added a `handle_failure` function to manage unexpected errors efficiently.  