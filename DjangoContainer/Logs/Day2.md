## **Day 2**

### **Podman and Container Setup**
- **8:00 PM**: Logged in.  
- **8:10 PM**: Installed Podman.  
- **8:20 PM**: Downloaded the AlmaLinux image.  
- **8:30 PM**: Ran the image as a container.  
- **8:40 PM**: Cloned the repository into the container.  
- **8:50 PM**: Installed dependencies: `python3-devel`, `pkgconfig`, `mysql-devel`.  
- **8:55 PM**: `mysql-devel` package not found in DNF.  
- **9:00 PM**: Installed `mariadb-devel` instead.  
- **9:10 PM**: `pip install` failed due to the missing `gcc` package.  
- **9:15 PM**: Installed `gcc` and re-ran `pip install`.  
- **9:20 PM**: `pip install` failed again due to greenlet version incompatibility with Python version.  
- **9:30 PM**: Encountered a `pip` version incompatibility error.  
- **9:40 PM**: Downgraded `pip` using `python3 -m pip install pip==0.9.6`.  
- **9:50 PM**: Greenlet version issue remained unresolved.  
- **10:00 PM**: Attempted to remove the Python version but could not as it is an essential package.  
- **10:05 PM**: Tried installing Python 3.6, but the package was unavailable.  
- **10:15 PM**: Installed Pyenv to manage Python versions.  
- **10:25 PM**: Pyenv installed, but switching to a lower Python version failed.  
- **10:30 PM**: Downloaded dependencies to build Python from source.  
- **10:45 PM**: Determined it was not feasible to switch Python versions on the image.  
- **10:50 PM**: Switched to another Docker image from Quay.io.  
- **11:30 PM**: Repeated the setup process; unresolved `mysql-devel` dependency issues.  
- **11:45 PM**: Discovered `kernel-headers` package was missing.  
- **11:50 PM**: Installed the missing packages.  
- **11:55 PM**: Repeated the process and successfully ran the Docker container.  

