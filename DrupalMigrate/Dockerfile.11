FROM docker.io/drupal:11.0-apache

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
    composer install --no-cache --no-dev --no-interaction --no-progress

EXPOSE 80
