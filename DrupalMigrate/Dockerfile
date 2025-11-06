FROM docker.io/drupal:10.0-apache

ARG REPO_DIR \
    ENV_USR \
    ENV_HOST 
ENV DEBIAN_FRONTEND=noninteractive \
    ENV_USR=${ENV_USR} \
    ENV_HOST=${ENV_HOST}

RUN apt update && \
    apt install -y git unzip && \ 
    echo "ServerName 10.0.2.2" >> /etc/apache2/apache2.conf && \
    echo "User www-data" >> /etc/apache2/apache2.conf && \
    echo "Group www-data" >> /etc/apache2/apache2.conf && \
    adduser --disabled-password --gecos "" ${ENV_USR} && \
    unlink /var/www/html && \
    mkdir -p /var/www/html 

COPY ${REPO_DIR} /var/www/html

WORKDIR /var/www/html

RUN composer update && \
    curl -s https://static.fossee.in/IT/drupal_fix_permissions.sh | bash -s -- -u=${ENV_USR}

EXPOSE 80

ENTRYPOINT ["apachectl"]
CMD ["-DFOREGROUND"]

