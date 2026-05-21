pipeline {
    agent any

    environment {
        ROOTPASS        = credentials('mysql')
        SITE_DB_USER    = 'koha_user_jenkins_6783' 
        SITE_DB_PASSWD  = credentials('koha_user_password')
    }

    parameters {
        booleanParam(
            name: 'UPDATE_PUBLIC',
            defaultValue: true,
            description: 'Select when new content is added in site/default/files directory of GitHub repository.<br>When true this will copy the contents to the container.'
        )

        choice(
            name: 'ENVIRONMENT',
            choices: ['DEVELOPMENT', 'PRODUCTION'],
            description: 'PRODUCTION: Reuse existing database and mounted folders<br>DEVELOPMENT: Create fresh database and mount empty folders'
        )
    }

    stages {


        stage ('Stop site') {
            steps {
                sh ''' 
                    export XDG_RUNTIME_DIR="/run/user/$(id -u)"
                    export DBUS_SESSION_BUS_ADDRESS="unix:path=\${XDG_RUNTIME_DIR}/bus"

                    systemctl --user disable koha.service || true
                    systemctl --user stop koha.service || true
                '''
            }
        }
        stage('Clone Repo') {
            steps {
                dir('koha_repo') {
                    checkout scmGit(
                        branches: [[name: '*/main']], 
                        extensions: [], 
                        userRemoteConfigs: [[url: 'https://github.com/FOSSEE/koha-2025']]
                    )
                }

                script {
                    if (params.UPDATE_PUBLIC) {
                        sh '''
                            mkdir -p "$WORKSPACE/../../site_directories/koha_public/"
                            mkdir -p "$WORKSPACE/../../site_directories/koha_uploads/"
                            
                            if [ -d "koha_repo/sites/default/files" ]; then
                                cp -r koha_repo/sites/default/files/. "$WORKSPACE/../../site_directories/koha_public/"
                            else
                                echo "Source directory does not exist"
                            fi
                        '''
                    } 
                    else {
                        echo "Content from site repo will not be copied to public dir of site"
                    }
                }
            }
        }
        stage ('Clone DB') {
            steps {
                dir('koha_db') {
                    checkout scmGit(
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[ credentialsId: 'jenkins_token', url: 'https://github.com/FOSSEE-DevOps/koha_db' ]]
                    )
                }
            }
        }
        stage('Create new MySQL DB') {
            steps {
                script {

                    if (params.ENVIRONMENT == "DEVELOPMENT") {

                        sh '''
                            set -e

                            db_list=$(mysql -u root -p"$ROOTPASS" --silent --skip-column-names \
                                -e "SHOW DATABASES LIKE 'koha_db_jenkins_%';")

                            if [ -n "$db_list" ]; then
                                while IFS= read -r db; do
                                    mysql -u root -p"$ROOTPASS" <<EOF
DROP DATABASE IF EXISTS $db;
EOF
                                done <<< "$db_list"
                                mysql -u root -p"$ROOTPASS" -e "FLUSH PRIVILEGES;"
                            fi

                            newdb="koha_db_jenkins_${BUILD_NUMBER}"

                            mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE IF NOT EXISTS $newdb;
CREATE USER IF NOT EXISTS '$SITE_DB_USER'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $newdb.* TO '$SITE_DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF

                            if [ -f "koha_db/koha_10.sql" ]; then
                                sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' \
                                    koha_db/koha_10.sql > /tmp/koha_clean.sql

                                mysql -u "$SITE_DB_USER" -p"$SITE_DB_PASSWD" "$newdb" < /tmp/koha_clean.sql
                            fi
                        '''
                    }

                    else {

                        sh '''
                            set -e

                            prod_db="koha_db_jenkins_${BUILD_NUMBER}"

                            exists=$(mysql -u root -p"$ROOTPASS" --silent --skip-column-names \
                                -e "SHOW DATABASES LIKE '$prod_db';")

                            if [ -z "$exists" ]; then
                                mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE $prod_db;
CREATE USER IF NOT EXISTS '$SITE_DB_USER'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $prod_db.* TO '$SITE_DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF

                                if [ -f "koha_db/koha_10.sql" ]; then
                                    sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' \
                                        koha_db/koha_10.sql > /tmp/koha_clean.sql

                                    mysql -u "$SITE_DB_USER" -p"$SITE_DB_PASSWD" "$prod_db" < /tmp/koha_clean.sql
                                fi
                            fi
                        '''
                    }

                }
            }
        }
        stage ('Fetch Dockerfile') {
            steps {
                dir('dockerfile_only') {
                    sh """
                        rm -rf .git
                        git init
                        git remote add origin https://github.com/FOSSEE-DevOps/DailyProgress
                        git config core.sparseCheckout true
                        echo "DrupalMigrate/Dockerfile" > .git/info/sparse-checkout
                        git pull origin automated --depth=1
                        cp DrupalMigrate/Dockerfile "${WORKSPACE}/Dockerfile"
                    """
                }
            }
        }
        stage ('Build Image') {
            steps {
                sh 'podman image ls --format "{{.Repository}}" | grep "^koha" | xargs -r podman image rm'
                sh """
                    podman build \
                        -t koha_image:latest \
                        -f "${WORKSPACE}/Dockerfile" \
                        --build-arg ENV_USR="koha_user_jenkins_6783" \
                        --build-arg REPO_DIR="koha_repo" \
                        --build-arg ENV_HOST="10.0.2.2" \
                        "${WORKSPACE}"
                """
            }
        }
        stage('Podman Secrets') {
            steps {
                script {

                    if (params.ENVIRONMENT == "DEVELOPMENT") {
                        sh """
                            set -e

                            podman secret rm koha_mysql_db || true
                            podman secret rm koha_mysql_password || true

                            printf "koha_db_jenkins_${BUILD_NUMBER}" | podman secret create koha_mysql_db -
                            printf "${SITE_DB_PASSWD}" | podman secret create koha_mysql_password -
                        """
                    }

                    else {
                        sh """
                            set -e

                            if ! podman secret inspect koha_mysql_db >/dev/null 2>&1; then
                                printf "koha_db_production" | podman secret create koha_mysql_db -
                            fi

                            if ! podman secret inspect koha_mysql_password >/dev/null 2>&1; then
                                printf "${SITE_DB_PASSWD}" | podman secret create koha_mysql_password -
                            fi
                        """
                    }
                }
            }
        }
        stage ('Podman SystemD generator') {
            steps {
                script {
                    sh '''
                    set -e

                    echo "Exporing XDG and DBUS vars"
                    export XDG_RUNTIME_DIR="/run/user/$(id -u)"
                    export DBUS_SESSION_BUS_ADDRESS="unix:path=\${XDG_RUNTIME_DIR}/bus"

                    systemctl --user disable koha.service || true
                    systemctl --user stop koha.service || true

                    mkdir -p ~/.config/containers/systemd


                    cat > ~/.config/containers/systemd/koha.container <<EOF
[Unit]
Description=koha Persist Container
After=network-online.target
Wants=network-online.target

[Container]
Image=koha_image:latest
Pull=never
AddCapability=NET_RAW
ContainerName=koha_container
PublishPort=9105:80
Volume=/var/lib/jenkins/site_directories/koha_public:/var/www/html/sites/default/files:Z
Volume=/var/lib/jenkins/site_directories/koha_uploads:/var/www/html/koha_uploads:Z
Network=slirp4netns:allow_host_loopback=true

[Service]
Restart=unless-stopped
TimeoutStartSec=1000

[Install]
WantedBy=default.target
EOF

                    systemctl --user daemon-reload || true

                    mkdir -p ~/.config/systemd/user
                    cp /run/user/$(id -u)/systemd/generator/koha.service ~/.config/systemd/user/ || true

                    echo "Adding Podman Secrets to service file"
                    sed -i 's|podman run|podman run --secret koha_mysql_db,type=env,target=ENV_DB --secret koha_mysql_password,type=env,target=ENV_PSWD|' ~/.config/systemd/user/koha.service
                    systemctl --user daemon-reload || true
                    systemctl --user enable koha || true
                    systemctl --user start koha || true
                    '''
                }
            }
        }
        stage('Mail enable') {
            steps {
                sh '''
                    podman exec koha_container apt update
                    podman exec koha_container apt install mailutils ssmtp -y
                    
                    podman exec koha_container sh -c 'echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf'
                    podman exec koha_container sh -c 'echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                    podman exec koha_container sh -c 'echo "'${ENV_USR}':'${ENV_USR}'@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                '''
            }
        }
        stage('Set Permissions') {
            steps {
                sh '''
                    podman exec koha_container sh -c 'curl -s https://static.fossee.in/IT/drupal_fix_permissions.sh | bash -s -- -u="koha_user_jenkins_6783"'
                    podman exec koha_container sh -c 'chown -R root:www-data /opt/drupal/sites/default/files'
                    podman exec koha_container sh -c 'chown -R root:www-data /opt/drupal/koha_uploads'
                    podman exec koha_container sh -c 'chmod -R 770 /opt/drupal/sites/default/files'
                    podman exec koha_container sh -c 'chmod -R 770 /opt/drupal/koha_uploads'
                '''
            }
        }
    }
}

