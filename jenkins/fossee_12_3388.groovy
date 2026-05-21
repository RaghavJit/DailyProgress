pipeline {
    agent any

    environment {
        ROOTPASS        = credentials('mysql')
        SITE_DB_USER    = 'fossee_user_jenkins_3388' 
        SITE_DB_PASSWD  = credentials('fossee_user_password')
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

                    systemctl --user disable fossee.service || true
                    systemctl --user stop fossee.service || true
                '''
            }
        }
        stage('Clone Repo') {
            steps {
                dir('fossee_repo') {
                    checkout scmGit(
                        branches: [[name: '*/main']], 
                        extensions: [], 
                        userRemoteConfigs: [[url: 'https://github.com/fossee/fossee_website_docker']]
                    )
                }

                script {
                    if (params.UPDATE_PUBLIC) {
                        sh '''
                            mkdir -p "$WORKSPACE/../../site_directories/fossee_public/"
                            mkdir -p "$WORKSPACE/../../site_directories/fossee_uploads/"
                            
                            if [ -d "fossee_repo/sites/default/files" ]; then
                                cp -r fossee_repo/sites/default/files/. "$WORKSPACE/../../site_directories/fossee_public/"
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
                dir('fossee_db') {
                    checkout scmGit(
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[ credentialsId: 'jenkins_token', url: 'https://github.com/FOSSEE-DevOps/fossee_db' ]]
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
                                -e "SHOW DATABASES LIKE 'fossee_db_jenkins_%';")

                            if [ -n "$db_list" ]; then
                                while IFS= read -r db; do
                                    mysql -u root -p"$ROOTPASS" <<EOF
DROP DATABASE IF EXISTS $db;
EOF
                                done <<< "$db_list"
                                mysql -u root -p"$ROOTPASS" -e "FLUSH PRIVILEGES;"
                            fi

                            newdb="fossee_db_jenkins_${BUILD_NUMBER}"

                            mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE IF NOT EXISTS $newdb;
CREATE USER IF NOT EXISTS '$SITE_DB_USER'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $newdb.* TO '$SITE_DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF

                            if [ -f "fossee_db/fossee_11.sql" ]; then
                                sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' \
                                    fossee_db/fossee_11.sql > /tmp/fossee_clean.sql

                                mysql -u "$SITE_DB_USER" -p"$SITE_DB_PASSWD" "$newdb" < /tmp/fossee_clean.sql
                            fi
                        '''
                    }

                    else {

                        sh '''
                            set -e

                            prod_db="fossee_db_jenkins_${BUILD_NUMBER}"

                            exists=$(mysql -u root -p"$ROOTPASS" --silent --skip-column-names \
                                -e "SHOW DATABASES LIKE '$prod_db';")

                            if [ -z "$exists" ]; then
                                mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE $prod_db;
CREATE USER IF NOT EXISTS '$SITE_DB_USER'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $prod_db.* TO '$SITE_DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF

                                if [ -f "fossee_db/fossee_11.sql" ]; then
                                    sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' \
                                        fossee_db/fossee_11.sql > /tmp/fossee_clean.sql

                                    mysql -u "$SITE_DB_USER" -p"$SITE_DB_PASSWD" "$prod_db" < /tmp/fossee_clean.sql
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
                        echo "DrupalMigrate/Dockerfile.11" > .git/info/sparse-checkout
                        git pull origin automated --depth=1
                        cp DrupalMigrate/Dockerfile.11 "${WORKSPACE}/Dockerfile"
                    """
                }
            }
        }
        stage ('Build Image') {
            steps {
                sh 'podman image ls --format "{{.Repository}}" | grep "^fossee" | xargs -r podman image rm'
                sh """
                    podman build \
                        -t fossee_image:latest \
                        -f "${WORKSPACE}/Dockerfile" \
                        --build-arg ENV_USR="fossee_user_jenkins_3388" \
                        --build-arg REPO_DIR="fossee_repo" \
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

                            podman secret rm fossee_mysql_db || true
                            podman secret rm fossee_mysql_password || true

                            printf "fossee_db_jenkins_${BUILD_NUMBER}" | podman secret create fossee_mysql_db -
                            printf "${SITE_DB_PASSWD}" | podman secret create fossee_mysql_password -
                        """
                    }

                    else {
                        sh """
                            set -e

                            if ! podman secret inspect fossee_mysql_db >/dev/null 2>&1; then
                                printf "fossee_db_production" | podman secret create fossee_mysql_db -
                            fi

                            if ! podman secret inspect fossee_mysql_password >/dev/null 2>&1; then
                                printf "${SITE_DB_PASSWD}" | podman secret create fossee_mysql_password -
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

                    systemctl --user disable fossee.service || true
                    systemctl --user stop fossee.service || true

                    mkdir -p ~/.config/containers/systemd


                    cat > ~/.config/containers/systemd/fossee.container <<EOF
[Unit]
Description=fossee Persist Container
After=network-online.target
Wants=network-online.target

[Container]
Image=fossee_image:latest
Pull=never
AddCapability=NET_RAW
ContainerName=fossee_container
PublishPort=9112:80
Volume=/var/lib/jenkins/site_directories/fossee_public:/var/www/html/sites/default/files:Z
Volume=/var/lib/jenkins/site_directories/fossee_uploads:/var/www/html/fossee_uploads:Z
Network=slirp4netns:allow_host_loopback=true

[Service]
Restart=unless-stopped
TimeoutStartSec=1000

[Install]
WantedBy=default.target
EOF

                    systemctl --user daemon-reload || true

                    mkdir -p ~/.config/systemd/user
                    cp /run/user/$(id -u)/systemd/generator/fossee.service ~/.config/systemd/user/ || true

                    echo "Adding Podman Secrets to service file"
                    sed -i 's|podman run|podman run --secret fossee_mysql_db,type=env,target=ENV_DB --secret fossee_mysql_password,type=env,target=ENV_PSWD|' ~/.config/systemd/user/fossee.service
                    systemctl --user daemon-reload || true
                    systemctl --user enable fossee || true
                    systemctl --user start fossee || true
                    '''
                }
            }
        }
        stage('Mail enable') {
            steps {
                sh '''
                    podman exec fossee_container apt update
                    podman exec fossee_container apt install mailutils ssmtp -y
                    
                    podman exec fossee_container sh -c 'echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf'
                    podman exec fossee_container sh -c 'echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                    podman exec fossee_container sh -c 'echo "'${ENV_USR}':'${ENV_USR}'@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                '''
            }
        }
        stage('Set Permissions') {
            steps {
                sh '''
                    podman exec fossee_container sh -c 'curl -s https://static.fossee.in/IT/drupal_fix_permissions.sh | bash -s -- -u="fossee_user_jenkins_3388"'
                    podman exec fossee_container sh -c 'chown -R root:www-data /opt/drupal/sites/default/files'
                    podman exec fossee_container sh -c 'chown -R root:www-data /opt/drupal/fossee_uploads'
                    podman exec fossee_container sh -c 'chmod -R 770 /opt/drupal/sites/default/files'
                    podman exec fossee_container sh -c 'chmod -R 770 /opt/drupal/fossee_uploads'
                '''
            }
        }
    }
}

