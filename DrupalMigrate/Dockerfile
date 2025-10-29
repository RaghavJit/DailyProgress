FROM docker.io/drupal:10.0-apache

ARG REPO_DIR \
    ENV_DB \
    ENV_USR \
    ENV_PSWD \
    ENV_SSO_ID \
    ENV_SSO_SECRET \
    ENV_HOST 
ENV DEBIAN_FRONTEND=noninteractive \
    ENV_DB=${ENV_DB} \
    ENV_USR=${ENV_USR} \
    ENV_PSWD=${ENV_PSWD} \
    ENV_HOST=${ENV_HOST}

RUN apt update && \
    apt install -y mailutils ssmtp git && \ 
    echo "ServerName 10.0.2.2" >> /etc/apache2/apache2.conf && \
    echo "User www-data" >> /etc/apache2/apache2.conf && \
    echo "Group www-data" >> /etc/apache2/apache2.conf && \
    adduser --disabled-password --gecos "" ${ENV_USR} && \
    echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases && \
    echo "${ENV_USR}:${ENV_USR}@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases && \
    unlink /var/www/html && \
    mkdir -p /var/www/html 

COPY ${REPO_DIR} /var/www/html

WORKDIR /var/www/html

RUN composer update && \
    curl -s https://rawcdn.githack.com/Metadrop/drupal-fix-permissions-script/eb12afc0bfe55aa7065f3af6be9713434c60c214/drupal_fix_permissions.sh | bash -s -- -u=${ENV_USR} && \
    curl -s https://rawcdn.githack.com/kjswaruph/fossee-daily-progress/41f58629de4036573bc50d8c27f91ff530de9346/seperate-vm/drupal | bash -s -- -c=${ENV_SSO_ID} -s=${ENV_SSO_SECRET}

EXPOSE 80

ENTRYPOINT ["apachectl"]
CMD ["-DFOREGROUND"]

