# List of issue faced in containerization of Drupal sites
These are the issues I faced during containerization of following Drupal sites and how I overcame them. All these issues have been resolved, however we will not be using some of these solutions in Production.

## Rootless contianer not network
### **Problem**
We are using Podman for containerization, podman by default uses Rootless containers. Unlike Docker, Podman does not have a daemon that starts/stops the containers. The container processes are can be started by a non root user. <br>
Since the confiners are rootless they dent have the access to the Host Machine network. We need the ability to connect to host on port 3306 to connect with MySQL database present on baremetal.

### **Demonstration:**
Create a rootless container with Podman (taking ubuntu/latest for Demonstration purposes)
```
podman pull ubuntu:latest
podman run -it ubuntu:latest
```
Inside container run 
```
apt update 
apt install iproute2 iputils-ping curl
ip a
```
The container does not have the ability to 
1. Ping host because there is no route.
2. Does not have a routable IP address.
3. Any attempt to curl on a host port results in failure.

### **Solution:**
This can be solved by using a network capability and slirp4netns. <br>
We need to have [slirp4netns](https://man.archlinux.org/man/slirp4netns.1.en) installed on our system and use it as default network mode.
We need to add the following flags to the podman run command
```
--cap-add=NET_RAW --network=slirp4netns:allow_host_loopback=true
```
This allows the container to access the localhost of the host machine. The container gets it's own network namespace. After using the solution we can see there is a private address 10.0.X.X with gateway 10.0.2.2.

### **Resources:**
1. [podman-slirp4netns docs](https://docs.podman.io/en/v4.9.0/markdown/podman-network.1.html)
1. [slirp4netns](https://man.archlinux.org/man/slirp4netns.1.en)
1. [stackoverflow](https://stackoverflow.com/questions/74579092/how-do-i-configure-rootless-containers-so-that-they-can-reach-the-host)
1. [Question I asked on reddit](https://www.reddit.com/r/podman/comments/1lqip4w/comment/n19x2k7/?context=3)

## Apache Misconfiguration
### **Problem:**
Since we are using the above mentioned settings for containers, we need to configure apache to host on the IP 10.0.X.X. All other details are trivial.

### **Demonstration:**
Start a container with following command
```
podman run -it \
  --name testing \
  --cap-add=NET_RAW \
  --network=slirp4netns \
  -p 8080:80 \
  docker.io/drupal:latest

```
You will get the following logs on starting the container
```
AH00558: apache2: Could not reliably determine the server's fully qualified domain name, using 10.0.2.100. Set the 'ServerName' directive globally to suppress this message
AH00558: apache2: Could not reliably determine the server's fully qualified domain name, using 10.0.2.100. Set the 'ServerName' directive globally to suppress this message
[Thu Nov 06 06:35:44.661886 2025] [mpm_prefork:notice] [pid 1] AH00163: Apache/2.4.57 (Debian) PHP/8.2.14 configured -- resuming normal operations
[Thu Nov 06 06:35:44.661958 2025] [core:notice] [pid 1] AH00094: Command line: 'apache2 -D FOREGROUND'
```
### **Solution:**
We can configure apache by running the following commands
```
echo "ServerName 10.0.2.2" >> /etc/apache2/apache2.conf && \
echo "User www-data" >> /etc/apache2/apache2.conf && \
echo "Group www-data" >> /etc/apache2/apache2.conf && \
```

Since the apache2 configuration was changed we need to restart it so I added following at the end of Dockerfile. This makes sure the above changes take effect.
```
ENTRYPOINT ["apachectl"]
CMD ["-DFOREGROUND"]
```

### **Resources:**
Nothing here

## Public folder permission issue and other permission based security lapses
## **Problem:**
Webteam works on the codefiles on their device, when attempting to clone their code and use it to run containers some code files, folder and media files did not have appropriate permission for the site to function properly. <br>
Webteam requested a podman volume be created and mounted on *'/var/www/html/sites/default/files'*, due to improper file permissions some of the features requiring public directly were not working.

### **Demonstration:**
A container started without permission fix would run as usual but somewhere on homepage following error message can be seen, the error message below if from arudino site. This also prevents users from uploading files to the site. 
```
User warning: mkdir(): Permission Denied in Drupal\Component\PhpStorage\FileStorage->createDirectory() (line 123 of core/lib/Drupal/Component/PhpStorage/FileStorage.php).
User warning: mkdir(): Permission Denied in Drupal\Component\PhpStorage\FileStorage->createDirectory() (line 125 of core/lib/Drupal/Component/PhpStorage/FileStorage.php).
User warning: mkdir(): Permission Denied in Drupal\Component\PhpStorage\FileStorage->createDirectory() (line 125 of core/lib/Drupal/Component/PhpStorage/FileStorage.php).
User warning: mkdir(): Permission Denied in Drupal\Component\PhpStorage\FileStorage->createDirectory() (line 125 of core/lib/Drupal/Component/PhpStorage/FileStorage.php).
User warning: mkdir(): Permission Denied in Drupal\Component\PhpStorage\FileStorage->createDirectory() (line 125 of core/lib/Drupal/Component/PhpStorage/FileStorage.php).
```

Following are some problematic permissions in a site here css and js does not load properly. This issue was especially more prominent in **gis** website.
```
d----w---- 1 root root   4096 Nov  4 02:47 css
dr-xrwxr-x 1 root root   4096 Nov  4 02:47 image
drwxrwxr-x 4 root root   4096 Nov  4 02:47 includes
d----w---- 1 root root   4096 Nov  4 02:47 js

```

### **Solution:**
Following were the recommended file permissions 
|                     |  Symbolic notation |  Numeric notation | ls notation  |
|-------------------- |------------------- |------------------ |------------- |
| **Code folders**    |    `u=rwx,g=rx,o=` |            `0750` |  `rwxr-x---` |
| **Code files**      |      `u=rw,g=r,o=` |            `0640` |  `rw-r-----` |
| **Content folders** |   `u=rwx,g=rwx,o=` |            `0770` |  `rwxrwx---` |
| **Content files**   |         `ug=rw,o=` |            `0660` |  `rw-rw----` |

After implementing these manually the errors were gone, this was however a very laborious task, therefore we decided to use the bash script (link below). <br> 
For further security, I forked the repository after carefully reviewing the script used it to perform the permission fixes. [Link to my fork](https://github.com/RaghavJit/drupal-fix-permissions-script/blob/main/drupal_fix_permissions.sh)
We also hosted this script on FOSSEE server so it can be directly cloned from there. [Link]()

### **Resources:**
1. [Drupal Article](https://www.drupal.org/docs/administering-a-drupal-site/security-in-drupal/securing-file-permissions-and-ownership)
2. [Script obtained from above article](https://github.com/Metadrop/drupal-fix-permissions-script)

## 502 issue on containers 
## **Problem:**
Drupal sites appear to throw a 503 error randomly which goes away after a refresh. This problem has not been fully resolved on any site, however below are some hypothesis, logs and attempts on fixing the problem that I tried.

Refer to this file [bug_report](./logs/bug_report.md)

<br>
<br>
<br>
<br>

**THE ISSUES BELOW ARE NOT DIRECTLY CONCERNED WITH FUNCTIONALITY OF DRUPAL SITES, PLEASE READ THE BELOW PARAGRAPH CAREFULLY BEFORE GOING AHEAD**
The below mentioned issues are not caused due to misconfiguration or bugs, these are extra features without which the sites **can** function properly. However, having these features makes automation easier and faster, reduces chance of human error and increases up time. You may choose to totally ignore the content below. These features are not implemented anywhere, and is more of my personal tomfoolery.

## Containers don't auto restart on server reboot  
## **Problem:**
We wish the continers to restart in case they crash or server restarts.

### **Demonstration:**
Nothing to Demonstrate.

### **Solution:**
Using Podman-Systemd-Generate a systemd file can be generated that will automatically restart servers as we want.
After building the image instead of directly running the container we write a .continer file on path ~/.config/continer/systemd. The continer run options can be written to the file like this:
```
[Unit]
Description=${SITE_NAME} Persist Container
After=network-online.target
Wants=network-online.target

[Container]
Image=${SITE_IMAGE}:latest
Pull=never
AddCapability=NET_RAW
ContainerName=${SITE_CONTAINER}
PublishPort=${PORT}:80
Volume=${SITE_VOLUME}:/var/www/html/sites/default/files:Z
Network=slirp4netns:allow_host_loopback=true

[Service]
Restart=unless-stopped
TimeoutStartSec=1000

[Install]
WantedBy=default.target
```

running the following command automatcially creates a systemd unit file on path `/run/user/$(id -u)/systemd/generator/`
```
systemd --user daemon-reload 
```
This can be copied to user's systemd unit files location `~/.config/systemd/user/.`

Since there is not option to add --secret to .container file secret flags had to be manually added to the final systemd unit file.

### **Resources:**
1. [Podman Systemd](https://docs.podman.io/en/latest/markdown/podman-generate-systemd.1.html)
1. [Podman Secrets](https://docs.podman.io/en/latest/markdown/podman-secret-create.1.html)

## SSMTP configuration not properly causing the email feature to not work properly 
## **Problem:**
### **Demonstration:**
### **Solution:**
### **Resources:**

## SSO configuration task  
## **Problem:**
### **Demonstration:**
### **Solution:**
### **Resources:**
