# Dockerfile Docs

Use the appropriate drupal base image for the site.
```
FROM docker.io/drupal:10.0-apache
```

Args and Env variables used during site building. Make sure dont add any sensative information like passwords and secrets in this section.
```
ARG REPO_DIR \
    ENV_USR \
    ENV_HOST 
ENV DEBIAN_FRONTEND=noninteractive \
    ENV_USR=${ENV_USR} \
    ENV_HOST=${ENV_HOST}
```

Both git and unzip are dependencies for composer to work properly, don't remove this install command in attempt to make the image 'leaner'.

The user is also added in this step, this is the same user which will access the database, have ownership of most of the files and send mail functionality.
```
RUN apt update && \
    apt install -y git unzip && \
    adduser --disabled-password --gecos "" ${ENV_USR} && \
    unlink /var/www/html
```

Copy the repo to ``/opt/drupal``. Please make sure to don't make major changes to direcotry structure of the repo, as it may break this step and also CI/CD workflows.
If drupal mendates change in direcotry strucgture notify the SysAd/DevOps.
```
COPY ${REPO_DIR}/ .
```

In the container we softlink the ``/opt/drupal`` path to web root, dont copy the contents of repo directly to web root
```
RUN ln -s /opt/drupal /var/www/html && \
    composer install --no-cache --no-dev --no-interaction --no-progress
```

Expose port 80 to host, this port will be mapped to *site-port* on host.
```
EXPOSE 80
```