FROM docker.io/drupal:10.0-apache

ARG REPO_DIR \
    ENV_USR \
    ENV_HOST 
ENV DEBIAN_FRONTEND=noninteractive \
    ENV_USR=${ENV_USR} \
    ENV_HOST=${ENV_HOST}

RUN apt update && \
    apt install -y git unzip && \ 
    adduser --disabled-password --gecos "" ${ENV_USR} && \
    unlink /var/www/html 

COPY ${REPO_DIR}/ .

RUN ln -s /opt/drupal /var/www/html && \
    composer update --no-cache --no-dev --no-interaction --no-progress && \
    curl -s https://static.fossee.in/IT/drupal_fix_permissions.sh | bash -s -- -u=${ENV_USR}

EXPOSE 80
