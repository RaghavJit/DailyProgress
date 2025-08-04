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
To understand the functionality of the Dockerfile, please refer to the [**Script Documentation**](./Script.md).

---

### **Prerequisites**

1. **SQL Database:**
The host system must have a **MYSQL** database listening on port 3306 on localhost. The username and password of the database must be known (as it will be passed as build arguments to the Dockerfile).

1. **Slirp4netns:**
Helps the container securely access the host's localhost. 

## **Steps for building**

Create database and import dump file (make sure all the sql dumpfiles and sites.csv is present in the same directory.)
```
./database.sh
```

Build the docker image with Dockerfile, 
```
podman build -t <name-of-your-image> . \
  --build-arg REPO=<repository-to-be-containerized> \
  --build-arg PORT=<port-to-host-the-site> \
  --build-arg ENV_DB=<database-for-site> \
  --build-arg ENV_USR=<username-for-database> \
  --build-arg ENV_PSWD=<password-for-database-for-the-username> \
  --build-arg ENV_HOST=10.0.2.2
```

Start a container with newly created image
```
podman run -dit \
  --network=slirp4netns:allow_host_loopback=true \
  --cap-add=NET_RAW \
  -p <port-number-to-run-the-site>:80 \
  --name=<name-of-your-container> \
  localhost/<name-of-your-image>:latest
```

This must start teh site on specified port in most cases


## **Troubleshooting**
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


## **Automation**
To avoid all the labour of performing the above steps for multiple sites [this](./init_sites) bash script can be employed. 

**Usage:**
1. Make sure jq, Podman and MySQL server are installed on the host machine where this script is run.
1. This script uses jq for reading sites.json, make sure sites.json exists in the same directory and has 'SITE_NAME', 'PORT', 'REPO' are fields are present in the site object.
1. [Dockerfile](./Dockerfile) should be present in the same directory.
```
./init_sites
```

The script is failsafe, it deletes/undoes errornous configuration if script fails. It performs input validation, file check and provides verbose output by default.