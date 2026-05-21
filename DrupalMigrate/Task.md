# **Migrate Drupal Websites from v7 to v10**

## **Task Overview**

This task involved migrating multiple Drupal 7 websites to Drupal 10 (or 11).
The objective was to take proper backups of all existing Drupal 7 sites, extract their contents (database and site files), and run them inside a containerized Drupal 10 environment.

## **Additional Information**

### How to read this documentation
(Only for SysAds) sections are relevant only for system administrators. Developers may safely skip these unless they choose to explore them.

Text in the format <some-text> must be replaced with the appropriate value before executing a command.

Example:
```
rm -r <folder-path>
```
becomes
```
rm -r ~/path/to/folder
```
Text written in italics is a placeholder and should be substituted when reading.

Example:<br>
“grant privileges on *database* to *db-user*”<br>
becomes<br>
“grant privileges on gis-db-11 to gis-user”.

---

### **Task Requirements:**
1. **Podman:** Podman was preferred over Docker for security and compliance reasons.
2. **Hosting DB on bare metal:** Database must run on baremetal Podman host, containers should be abel to connect with this DB securely.
3. **Jenkins installation:** Jenkins to perform CI/CD (Only for SysAds)

---

### **Tools Documentation:**
1. [**Podman CLI Documentation**](https://docs.podman.io/en/latest/).
1. [**slirp4netns Documentation**](https://man.archlinux.org/man/slirp4netns.1.en)
1. [**Jenkins**](https://www.jenkins.io/doc/) (Only for SysAds)
1. [**Jenkins CI/CD**](./docs/Docs_Jenkins.md)
1. [Dockerfile Docs](./docs/Docs_Dockerfile.md)
1. [Permissions and seLinux Docs](./docs/Docs_Folder_Permission.md)
1. [Database SSL issue](./docs/Docs_Database_SSL.md)
1. [Mail Configuration](./docs/Docs_MailConfig.md)
---

### **Prerequisites**

1. **SQL Database:**
The host system must have a **MYSQL** database listening on port 3306 on localhost. The username and password of the database must be known (as it will be passed as build arguments to the Dockerfile).

1. **Slirp4netns:**
Helps the container securely access the host's localhost. 

1. **Jenkins**
Helps automate deployment with a CI/CD pipeline. (Only for SysAds)

## **Building steps for developers**

### **Keywords and meaning**
1. **db-dump**: MySQL ``.dump`` file of the site database.
1. **database**: Name of the database to which *db-dump* is improted. 
1. **db-user**: User which will access the contents of *database* and has all privileges.
1. **site-db-pass**: Password used by *db-user* to access *database*.
1. **image-name**: Name of image created for the site with podman build command. 
1. **container-name**: Name of image created for the site with podman build command. 
1. **site-port**: Port of host machine to which the port of container is mapped.

---

### **Steps for building**

1. Create database and import dump file.

```
mysql -u root -p
CREATE DATABASE IF NOT EXISTS <database>;
CREATE USER IF NOT EXISTS '<db-user>'@'localhost' IDENTIFIED BY '<site-db-pass>';
GRANT ALL PRIVILEGES ON <database>.* TO '<db-user>'@'localhost';
FLUSH PRIVILEGES;
```

Sometimes the dumpfile creates it's own database when imported, and does not get imported to our own *database*, this is not ideal. As the developer will have to read the dump file to find out the database name to be able to use it as env variable in later steps, instead we want to import *db-dump* to our own *database*, not the one created and used in dump file. (replace <db-dump> with dumpfile path.)
```
sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' \
    <db-dump> > <db-dump>
```

Import the *db-dump* to *database*
```
mysql -u "<db-user>" -p"<site-db-pass>" "<database>" < <db-dump>
```

2. Build the docker image with Dockerfile, there are two dockerfiles [Dockerfile](./files/Docker/10/Dockerfile) for v10 and [Dockerfile.11](./files/Docker/11/Dockerfile) for v11. To build the image run the following commadn in *site-repo*, the Dockerfile should be present in the same directory.

```
podman build \
    -t <image-name>:latest \
    -f "Dockerfile" \
    --build-arg ENV_USR="<db-user>" \
    --build-arg REPO_DIR="<site-repo>" \
    --build-arg ENV_HOST="10.0.2.2" \
    .
```

3. Start a container with newly created image
```
podman run -d \
  --name <container-name> \ 
  -e ENV_HOST=10.0.2.2 \
  -e ENV_USR=<db-user> \
  -e ENV_PSWD='<site-db-pass>' \
  -e ENV_DB=<database> \
  -p <site-port>:80 \
  --cap-add=NET_RAW \ --network=slirp4netns:allow_host_loopback=true \
  -v /path1/on/host:/var/www/html/sites/default/files:/path:Z
  -v /path2/on/host:/var/www/html/somefolder:/somepath:Z
  <image-name>
```
Sometimes we may commit containers and push them on a repository, this may lead to secrets leak. The image created by podman commit may contain environment variables, don't share this image in public domain. If commit has to be done, reset the secrets.

This must start the site on specified port in most cases. Alternativly we can use pasta instead of slipr4netns.


## **Troubleshooting**
Error on building

```
The website encountered an unexpected error. Try again later.
```
This error is almost always an indication of problem with Database, following could be the reasons:
1. In some rare case (almost impossible), the host specified in the build command **10.0.0.2** might change, this will prevent the database from connecting and site.
2. Empty database or corrupt database.
3. Hidden white spaces in passwords or usernames needed for connecting to database
4. SQL server demanding TLS certs from client container.
5. Drush not using the --skip-ssl option when connecting to SQL server. Refer [database ssl issue](./docs/Docs_Database_SSL.md),
6. SQL server not running or running on a port other than 3306. [database ssl issue](./docs/Docs_Database_SSL.md),

**General steps for troublshooting**
1. Check if database is correctly imported or not. On the host machine check the mysql connection
```
mysql -u <db-user> -p
```
2. Check if username and password environment variables in the container are correct. Use this command to list only the env vars starting with ENV, look for white space or next line characters here.
```
env | grep ENV
```

3. Go to setting.php file and check if the environment variables are being used, sometimes developers may change the env vars to hardcoded values.

4. Issue with connection (very less likly to happen).
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
To test mysql connection, mariadb client by default demands TLS certs to avoid this we can use ``--skip-ssl`` flag.
```
mysql -h <ip you obtained> -u <db-user> -p <database>
```

If you fail to connect there might be issue in your firewall, slirp4netns might not be installed.

### Commmon mistakes
1. **Port already busy**: If the *site-port* specified in the run command is already being used by another contaier, you might get port already busy error. Either stop the process/container using that port or use a different port.
1. **Bad Composer file**: If the podman build stops at last stage it is generally due to issues with packages/dependencies or corrupted composer files.
1. **seLinux lables**: Folders are mounted with :Z flag this already takes care of seLinux 'level', if the issue still persists, selinux can be ``setenforce 0`` on development machines. (Does not imply if you don't have seLinux installed.) Refer [Correct seLinux labeling for mounted folders](./docs/Docs_Folder_Permission.md)
1. **Folder permissions**: Make sure the www-data or web user has read/write permissions to folders where files may get downloaded or uploaded from. If on development environmnet 770 permissions can be used for root:www-data user and group. Refer [Folder permissions](./docs/Docs_Folder_Permission.md)
1. **Mail**: If you have postfix installed and properly configured on the development system, use the following commands in the container to enable mail sending.
```
apt update && apt install mailutils ssmtp

echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf
echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases
echo "${ENV_USR}:${ENV_USR}@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases
```

Kindly ensure that the mail user in drupal settings and /etc/ssmtp config files match. If the users don't match mail sending will not work. Refer [Mail configuration guide](./docs/Docs_MailConfig.md)
