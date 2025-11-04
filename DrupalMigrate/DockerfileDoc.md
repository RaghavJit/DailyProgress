# Dockerfile

## Base image
We use official drupal image to avoid conflits and supported offered by the maintienrs.

## Build Args
These are variables that are supplied to podman when the image is being build from Dockerfile. 
1. **ENV_HOST**: is the ip address of host visible from inside the continer. Since we are using slirp4netns, the IP address assinged to the container is virtually managed by podman. Unlike docker slirp4netns does not create a docker-bridge on host, instead each container gets its own network namespace, defautl value is 10.0.2.2.
2. **ENV_USR**: This is a unix user in container that is used to manage multiple things
    - This user owns all the code files.
    - Manages ssmtp connection/configuration.
    - User with same name will be added later as drupal user for future features like SSO integration.

## ENV 
The environment values liek password of database and database name are supplied using deploy.yml. This was done so the image can be shared without the worry of secrets leak. Refer to [deployDocs](./deployDoc.md) for more information.

## Package installs
The drupal files need some packages to funciton properly those are downloaded nad instlled using apt.

## Apache configuration
This apache configuration is mandatory, the container will not work without it unless docker is used instead of podman. Since we are using podman slirp4netns, container gets a virtual IP as described above, apache configuration has to be updated to use the correct ServerName.  

## SSMTP configuration
SSMTP configuration is needed for the emailing features to work properly, this assumes there is a SMTP agent correctly configured on the host already nad uses it as mail relay.

## Remote Script   
This remote script has been thoroly inspected before including here, it is hosted on a CND and not directly cloned from any github repo. This script fixes the drupal file permissions and ownerships to drupal recommneded format. This is where the ENV_USR is used.

## Some useful commands

| Task                    | Command                                |
| ----------------------- | -------------------------------------- |
| Build images            | `podman compose -f deploy.yml build`   |
| Start containers        | `podman compose -f deploy.yml up -d`   |
| View logs (follow)      | `podman compose -f deploy.yml logs -f` |
| Stop containers         | `podman compose -f deploy.yml down`    |
| List running containers | `podman ps`                            |

