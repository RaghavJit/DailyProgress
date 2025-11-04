# **Migrate Drupal Websites from v7 to v10**

## **Task Overview**

The task involved migrating Drupal websites from version 7 to 10. The objective was to the proper backup of all websites currently running on Drupal7 and extract their contents (Database and site files), and running them in a containerized Drupal10 environment.

---

### **Task Requirements:**
1. **Podman:** Podman was preferred over Docker for security and compliance reasons.
2. **Hosting DB on bare metal:** Database must run on baremetal Podman host, containers should be abel to connect with this DB securely.

---

### **Proxmox Tools Documentation:**
1. [**Podman CLI Documentation**](https://docs.podman.io/en/latest/).
1. [**slirp4netns Documentation**](https://man.archlinux.org/man/slirp4netns.1.en)

---

### **Dockerfile Documentation:**
To understand the functionality of the Dockerfile, please refer to the [**Script Documentation**](./DockerfileDoc.md).

---

### **Prerequisites**

1. **SQL Database:**
The host system must have a **MYSQL** database listening on port 3306 on localhost. The username and password of the database must be known (as it will be passed as build arguments to the Dockerfile).

1. **Slirp4netns:**
Helps the container securely access the host's localhost. 

## **Steps for building**

1. Create database and import dump file. Run the following commands in sql terminal.
```
CREATE DATABASE <database name>;
CREATE USER '<database username>'@'localhost' IDENTIFIED BY '<database user password>';
GRANT ALL PRIVILEGES ON '<database name>'.* TO '<database username>'@'localhost';
FLUSH PRIVILEGES;
```

2. Import .sql dump to the database;
```
sudo mysql -u root -p "<database name>" < <dumpfile path>
```

**The place holders are described [here](./deployDoc.md) make sure the same values are used in [deploy.yml](./deploy.yml) file**

3. Clone the repo to be hosted, and place in the same directory as deploy.yml and Dockerfile
```
.
|
--deploy.yml
|
--Dockerfile
|
--<github cloned repo>
```

3. Build image with podman compose, since this image does not contain any sensitive secrets it can be shared freely and pushed to cloud platforms like DockerHub.
```
podman-compose -f deploy.yml build
```

3. Start a container with newly created image
```
podman-compose -f deploy.yml up -d
```

This must start the site on specified port in most cases


## **Troubleshooting**

### Database not connecting
In some rare case, the host specified in the build command **10.0.0.2** might change, this will prevent the database from connecting and site will give out a message like
```
The website encountered an unexpected error. Try again later.
```

To fix this issue we must obtain the correct IP of host that is visible to container inside slirp4netns virtual network namespace.

Drop inside the shell
```
bash exec -it <container-name> bash
```
Install trouble shooting packages
```
apt install mysql-client iproute2 iputils-ping nmap nano
```
Check the container's IP address 
```
ip a
```
Search the subnet for other devices
```
nmap -sn </24 subnet of the container's ip>
```
Look for open port 3306 on suspected IP
```
nmap -p 3306 10.0.2.8
```
If MySQL is running that is your host's virtual IP!
To test mysql connection 
```
mysql -h <ip you obtained> -u <username> -p <database name>
```

### Podman compose issue
Podman compose is a thin wrapper around a compose provider kindly make sure you have the right compose provider installed, it is recommended to use podman compose as some options in [deploy.yml](./deploy.yml) are podman specific. After removing those options the same file might be used with docker-compose, the reliability of the same is not guaranteed.
```
sudo apt install podman-compose   # Debian/Ubuntu
# or
sudo dnf install podman-compose   # Fedora/RHEL
# and check 
podman-compose --version
```

## **Automation**
To avoid all the labour of performing the above steps for multiple sites you can use the automation scripts available on automation branch. 
