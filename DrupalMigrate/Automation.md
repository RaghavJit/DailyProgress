## **Automation**
To avoid all the labour of performing the above steps for multiple sites [this](./init_sites) bash script can be used. 

## **Prerequisites**
1. Podman
2. slirp4netns (generally preinstalled with Podman)
3. Bash Shell
4. MySQL server on Host Machine with root user
5. Linux user with root privileges
6. jq

## **Directory structure and files for deployment**
```
|
--init_sites 
--Dockerfile
--sites.json
--SQLDumps
  |
  --site1.sql
  --site2.sql
```

1. [init_sites](./init_sites): Bash script that handles deployment.
2. Dockerfile: [Drupal 10 Dockerfile](./Dockerfile), [Drupal 11 Dockerfile](./Dockerfile.11)
3. [sites.json](./sites.json): This json file contains the configuration for site that needs to be deployed
4. SQLDumps: This folder can be placed anywhere, keeping them in the same folder keeps everything organized and keeps the paths short.

### Contents of sites.json
```
[
    {
        "SITE_NAME": "arduino",
        "PORT": 8080,
        "REPO": "github.com",
    },
    {
        "SITE_NAME": "freecad",
        "PORT": 8081,
        "REPO": "github.com",
    }
]
```
1. SITE_NAME: Name of the site to be deployed, this field must be unique
2. PORT: The port on host machine to which container port 80 will bind. Make sure the port is free before deployment.
3. REPO: Github repo link to the code for Drupal site.

## **Usage**
1. Make sure all the requriments mentioned above are installed on the host machine where this script is run.
1. User should only write 'SITE_NAME', 'PORT', 'REPO' are fields sites.json, all other fields will be managed by init_sites script, once the deployment has take place these fields must never be changed manually.
1. Make sure the above mentioned directory structure is followed.

### **Deploying**
To start deployment run the bash script:
```
./init_sites
```

Script has two phases, in first phase the site config is saved to sites.json and .sql file gets imported to MYSQL Server. In the second phase image building and deployment takes place

```
--- Phase 1
Input Validation
Updating sites.json with configuration
Importing .sql file

--- Phase 2
Building Image
Running container
Making systemD server
```

### Phase 1
This will prompt for site configuration as follows
```
Enter MySQL root password: 
!! Kindly use lowercase for naming and don't include white spaces !!
Enter name of the site: freecad
Enter name of site database: freecad_db
Add date tag to database name? [default: _2025_09_17]: 
Enter database username you wish to use: freecad_user
Enter path to SQL dumpfile for site: ./dumps/freecad_10.sql
```

1. MySQL Password: Password of a user that can connect to mysql server and create db, user and grant privileges.
2. Name of Site: This tell the script which site to deploy, this name should be same as inside the sites.json (SITE_NAME)
3. Name of site DB: This is the name of site db where script will import the dump file.
4. Tag: This helps keep track of imports, this can be modified to users liking.
5. DB username: Name of DB user which will manage the imported database. If user already exists, script will prompt you to enter the password for that user, otherwise a user will be created with a new password (Password will be updated in sites.json file)

```
Summary of entered information:
Site Name        : freecad
Database Name    : freecad_db_2025_09_17
Database Username: freecad_user
SQL Dump File    : ./dumps/freecad_10.sql
Image Name       : freecad_image
Container Name   : freecad_container
Service Name     : freecad_persist
``` 
If there already exists a volume script will mount it automatically

```
Persistent volume 'freecad_volume' already exists.
```

### Phase 2

Script will ask for confirmation before building the image.
```
SITE_NAME: freecad
Building Podman image with:
Site Image Name  : freecad_image
Database Name    : freecad_db_2025_09_17
Database Username: freecad_user
Repository       : https://github.com/nikitasabale96/Freecad_Docker

Proceed with image build? (y/N): 
```

**If for any reason you abort the script the script will run handleFailure function this will delete all the progress made by the script to give you a blank slate for next deployment.**

```
!!! Operation Failed !!!
Error occurred. Check input and try again.
Would you like to undo the changes made by script? (y/N): 
```

This script runs such that, next steps are taken when all previous steps were successful.**handleFailure** will automatically execute incase the script fails to perform the deployment successfully. Don't abort the script with ^C.l