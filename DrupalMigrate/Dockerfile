FROM docker.io/drupal:10.0-apache

ARG REPO_DIR \
    ENV_HOST 
ENV DEBIAN_FRONTEND=noninteractive \
    ENV_HOST=${ENV_HOST}

RUN apt update && \
    apt install -y mailutils ssmtp git unzip && \ 
    echo "ServerName 10.0.2.2" >> /etc/apache2/apache2.conf && \
    echo "User www-data" >> /etc/apache2/apache2.conf && \
    echo "Group www-data" >> /etc/apache2/apache2.conf && \
    unlink /var/www/html && \
    mkdir -p /var/www/html 

COPY ${REPO_DIR} /var/www/html

WORKDIR /var/www/html

RUN composer update 

EXPOSE 80

ENTRYPOINT ["apachectl"]
CMD ["-DFOREGROUND"]

