FROM docker.io/library/ubuntu:latest

ARG REPO \
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
    apt install -y git apache2 \ 
    php8.1 php8.1-cli php8.1-common php8.1-imap php8.1-redis php8.1-snmp \
    php8.1-xml php8.1-zip php8.1-mbstring php8.1-curl libapache2-mod-php php8.1-mysql php8.1-gd && \
    echo "ServerName 10.0.2.2" >> /etc/apache2/apache2.conf && \
    echo "User www-data" >> /etc/apache2/apache2.conf && \
    echo "Group www-data" >> /etc/apache2/apache2.conf && \
    sed -i "$(grep -n '^</Directory>$' /etc/apache2/apache2.conf | tail -n1 | cut -d: -f1)a\\\\<Directory /var/www/html/>\n\tOptions Indexes FollowSymLinks\n\tAllowOverride all\n\tRequire all granted\n</Directory>" /etc/apache2/apache2.conf && \
    rm -rf /var/www/html/* && \
    git clone ${REPO} /var/www/html

WORKDIR /var/www/html 

RUN php -r "copy('https://getcomposer.org/installer', 'composer-setup.php');" && \
    php -r 'if (hash_file("sha384", "composer-setup.php") === "dac665fdc30fdd8ec78b38b9800061b4150413ff2e3b6f88543c636f7cd84f6db9189d43a81e5503cda447da73c7e5b6") { echo "Installer verified\n"; } else { echo "Installer corrupt\n"; unlink("composer-setup.php"); exit(1); }' && \
    php composer-setup.php && \
    php -r "unlink('composer-setup.php');" && \
    mv ./composer.phar /usr/local/bin/composer && \
    chmod +x /usr/local/bin/composer && \
    apt remove --purge -y php8.4* && \
    phpenmod curl && \ 
    phpenmod gd && \
    /usr/local/bin/composer update

EXPOSE 80

CMD ["bash", "-c", "apachectl start && bash"]