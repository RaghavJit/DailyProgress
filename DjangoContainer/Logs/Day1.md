## **Day 1**

### **Testing in Virtual Machine**
- **8:00 PM**: Logged in.  
- **8:10 PM**: Created a virtual machine using the AlmaLinux 8 ISO image.  
- **8:20 PM**: Cloned the project repository.  
- **8:30 PM**: Attempted to install dependencies (failed).  
- **8:40 PM**: `pip install` failed due to the missing `mysql-devel` package.  
- **9:00 PM**: Installed all missing dependencies.  
- **9:10 PM**: `runserver` command failed due to the absence of `config.py`.  
- **9:15 PM**: Wrote a prototype `config.py`.  
- **9:20 PM**: Created an SQLite3 database and updated `config.py` to reflect the changes.  
- **9:35 PM**: Edited `settings.py` to use the SQLite3 database.  
- **9:40 PM**: Syntax error encountered.  
- **10:00 PM**: Traced the syntax error to an incorrect version of dependencies.  
- **10:10 PM**: Reinstalled dependencies.  
- **10:30 PM**: Made changes to `manage.py`.  
- **10:40 PM**: Successfully ran the application on the virtual machine.  
- **11:00 PM**: Restored the repository and deleted the virtual environment.  
- **11:20 PM**: Downgraded `pip` version.  
- **11:40 PM**: Resolved version conflict issues.  
- **11:45 PM**: Got the application running with Python 3.6 and `pip` 0.9.  


