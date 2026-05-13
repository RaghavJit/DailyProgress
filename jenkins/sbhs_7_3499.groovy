pipeline {
    agent any

    environment {
        ROOTPASS        = credentials('mysql')
        SITE_DB_USER    = 'sbhs_user_jenkins_3499' 
        SITE_DB_PASSWD  = credentials('sbhs_user_password')
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

                    systemctl --user disable sbhs.service || true
                    systemctl --user stop sbhs.service || true
                '''
            }
        }
        stage('Clone Repo') {
            steps {
                dir('sbhs_repo') {
                    checkout scmGit(
                        branches: [[name: '*/main']], 
                        extensions: [], 
                        userRemoteConfigs: [[url: 'https://github.com/FOSSEE/SBHS-Drupal10']]
                    )
                }

                script {
                    if (params.UPDATE_PUBLIC) {
                        sh '''
                            mkdir -p "$WORKSPACE/../../site_directories/sbhs_public/"
                            mkdir -p "$WORKSPACE/../../site_directories/sbhs_uploads/"
                            
                            if [ -d "sbhs_repo/sites/default/files" ]; then
                                cp -r sbhs_repo/sites/default/files/. "$WORKSPACE/../../site_directories/sbhs_public/"
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
                dir('sbhs_db') {
                    checkout scmGit(
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[ credentialsId: 'sbhs', url: 'https://github.com/RaghavJit/sbhs_db' ]]
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
                                -e "SHOW DATABASES LIKE 'sbhs_db_jenkins_%';")

                            if [ -n "$db_list" ]; then
                                while IFS= read -r db; do
                                    mysql -u root -p"$ROOTPASS" <<EOF
DROP DATABASE IF EXISTS $db;
EOF
                                done <<< "$db_list"
                                mysql -u root -p"$ROOTPASS" -e "FLUSH PRIVILEGES;"
                            fi

                            newdb="sbhs_db_jenkins_${BUILD_NUMBER}"

                            mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE IF NOT EXISTS $newdb;
CREATE USER IF NOT EXISTS '$SITE_DB_USER'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $newdb.* TO '$SITE_DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF

                            if [ -f "sbhs_db/sbhs_10.sql" ]; then
                                sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' \
                                    sbhs_db/sbhs_10.sql > /tmp/sbhs_clean.sql

                                mysql -u "$SITE_DB_USER" -p"$SITE_DB_PASSWD" "$newdb" < /tmp/sbhs_clean.sql
                            fi
                        '''
                    }

                    else {

                        sh '''
                            set -e

                            prod_db="sbhs_db_jenkins_${BUILD_NUMBER}"

                            exists=$(mysql -u root -p"$ROOTPASS" --silent --skip-column-names \
                                -e "SHOW DATABASES LIKE '$prod_db';")

                            if [ -z "$exists" ]; then
                                mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE $prod_db;
CREATE USER IF NOT EXISTS '$SITE_DB_USER'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $prod_db.* TO '$SITE_DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF

                                if [ -f "sbhs_db/sbhs_10.sql" ]; then
                                    sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' \
                                        sbhs_db/sbhs_10.sql > /tmp/sbhs_clean.sql

                                    mysql -u "$SITE_DB_USER" -p"$SITE_DB_PASSWD" "$prod_db" < /tmp/sbhs_clean.sql
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
                        git remote add origin https://github.com/RaghavJit/DailyProgress
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
                sh 'podman image ls --format "{{.Repository}}" | grep "^sbhs" | xargs -r podman image rm'
                sh """
                    podman build \
                        -t sbhs_image:latest \
                        -f "${WORKSPACE}/Dockerfile" \
                        --build-arg ENV_USR="sbhs_user_jenkins_3499" \
                        --build-arg REPO_DIR="sbhs_repo" \
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

                            podman secret rm sbhs_mysql_db || true
                            podman secret rm sbhs_mysql_password || true

                            printf "sbhs_db_jenkins_${BUILD_NUMBER}" | podman secret create sbhs_mysql_db -
                            printf "${SITE_DB_PASSWD}" | podman secret create sbhs_mysql_password -
                        """
                    }

                    else {
                        sh """
                            set -e

                            if ! podman secret inspect sbhs_mysql_db >/dev/null 2>&1; then
                                printf "sbhs_db_production" | podman secret create sbhs_mysql_db -
                            fi

                            if ! podman secret inspect sbhs_mysql_password >/dev/null 2>&1; then
                                printf "${SITE_DB_PASSWD}" | podman secret create sbhs_mysql_password -
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

                    systemctl --user disable sbhs.service || true
                    systemctl --user stop sbhs.service || true

                    mkdir -p ~/.config/containers/systemd


                    cat > ~/.config/containers/systemd/sbhs.container <<EOF
[Unit]
Description=sbhs Persist Container
After=network-online.target
Wants=network-online.target

[Container]
Image=sbhs_image:latest
Pull=never
AddCapability=NET_RAW
ContainerName=sbhs_container
PublishPort=9107:80
Volume=/var/lib/jenkins/site_directories/sbhs_public:/var/www/html/sites/default/files:Z
Volume=/var/lib/jenkins/site_directories/sbhs_uploads:/var/www/html/sbhs_uploads:Z
Network=slirp4netns:allow_host_loopback=true

[Service]
Restart=unless-stopped
TimeoutStartSec=1000

[Install]
WantedBy=default.target
EOF

                    systemctl --user daemon-reload || true

                    mkdir -p ~/.config/systemd/user
                    cp /run/user/$(id -u)/systemd/generator/sbhs.service ~/.config/systemd/user/ || true

                    echo "Adding Podman Secrets to service file"
                    sed -i 's|podman run|podman run --secret sbhs_mysql_db,type=env,target=ENV_DB --secret sbhs_mysql_password,type=env,target=ENV_PSWD|' ~/.config/systemd/user/sbhs.service
                    systemctl --user daemon-reload || true
                    systemctl --user enable sbhs || true
                    systemctl --user start sbhs || true
                    '''
                }
            }
        }
        stage('Mail enable') {
            steps {
                sh '''
                    podman exec sbhs_container apt update
                    podman exec sbhs_container apt install mailutils ssmtp -y
                    
                    podman exec sbhs_container sh -c 'echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf'
                    podman exec sbhs_container sh -c 'echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                    podman exec sbhs_container sh -c 'echo "'${ENV_USR}':'${ENV_USR}'@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                '''
            }
        }
        stage('Set Permissions') {
            steps {
                sh '''
                    podman exec sbhs_container sh -c 'curl -s https://static.fossee.in/IT/drupal_fix_permissions.sh | bash -s -- -u="sbhs_user_jenkins_3499"'
                    podman exec sbhs_container sh -c 'chown -R root:www-data /opt/drupal/sites/default/files'
                    podman exec sbhs_container sh -c 'chown -R root:www-data /opt/drupal/sbhs_uploads'
                    podman exec sbhs_container sh -c 'chmod -R 770 /opt/drupal/sites/default/files'
                    podman exec sbhs_container sh -c 'chmod -R 770 /opt/drupal/sbhs_uploads'
                '''
            }
        }
    }
}

