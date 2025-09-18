FROM docker.io/library/ubuntu:latest

ARG REPO \
    BRANCH \
    ENV_DB \
    ENV_USR \
    ENV_PSWD \
    ENV_HOST 
ENV DEBIAN_FRONTEND=noninteractive \
    ENV_DB=${ENV_DB} \
    ENV_USR=${ENV_USR} \
    ENV_PSWD=${ENV_PSWD} \
    ENV_HOST=${ENV_HOST}

RUN apt update && \
    apt install -y software-properties-common && \
    add-apt-repository ppa:ondrej/php -y && \
    apt update && \
    apt install -y unzip curl mailutils ssmtp git apache2 \
    php8.1 php8.1-cli php8.1-common php8.1-imap php8.1-redis php8.1-snmp \
    php8.1-xml php8.1-zip php8.1-mbstring php8.1-curl libapache2-mod-php php8.1-mysql php8.1-gd && \
    echo "ServerName 10.0.2.2" >> /etc/apache2/apache2.conf && \
    echo "User www-data" >> /etc/apache2/apache2.conf && \
    echo "Group www-data" >> /etc/apache2/apache2.conf && \
    sed -i "$(grep -n '^</Directory>$' /etc/apache2/apache2.conf | tail -n1 | cut -d: -f1)a\\\\<Directory /var/www/html/>\n\tOptions Indexes FollowSymLinks\n\tAllowOverride all\n\tRequire all granted\n</Directory>" /etc/apache2/apache2.conf && \
    echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf && \
    adduser --disabled-password --gecos "" ${ENV_USR} && \
    echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases && \
    echo "${ENV_USR}:${ENV_USR}@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases && \
    rm -rf /var/www/html/* && \
    git clone --branch ${BRANCH} ${REPO} /var/www/html

WORKDIR /var/www/html 

RUN php -r "copy('https://getcomposer.org/installer', 'composer-setup.php');" && \
    php -r 'if (hash_file("sha384", "composer-setup.php") === "ed0feb545ba87161262f2d45a633e34f591ebb3381f2e0063c345ebea4d228dd0043083717770234ec00c5a9f9593792") { echo "Installer verified\n"; } else { echo "Installer corrupt\n"; unlink("composer-setup.php"); exit(1); }' && \
    php composer-setup.php && \
    php -r "unlink('composer-setup.php');" && \
    mv ./composer.phar /usr/local/bin/composer && \
    chmod +x /usr/local/bin/composer && \
    apt remove --purge -y php8.4* && \
    phpenmod curl && \
    phpenmod gd && \
    /usr/local/bin/composer update && \
    curl -s https://raw.githubusercontent.com/Metadrop/drupal-fix-permissions-script/refs/heads/main/drupal_fix_permissions.sh | bash -s -- -u=${ENV_USR}

EXPOSE 80

ENTRYPOINT ["apachectl"]
CMD ["-DFOREGROUND"]
