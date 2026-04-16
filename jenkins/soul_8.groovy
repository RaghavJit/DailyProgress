pipeline {
    agent any

    environment {
        ROOTPASS        = credentials('mysql')
        SITE_DB_PASSWD  = credentials('soul_user_password')
    }

    stages {

        stage ('Stop site') {
            steps {
                sh ''' 
                    export XDG_RUNTIME_DIR="/run/user/$(id -u)"
                    export DBUS_SESSION_BUS_ADDRESS="unix:path=\${XDG_RUNTIME_DIR}/bus"

                    systemctl --user disable soul.service || true
                    systemctl --user stop soul.service || true
                '''
            }
        }

        stage ('Clone Repo') {
            steps {
                dir('soul_repo') {
                    checkout scmGit(
                        branches: [[name: '*/main']], 
                        extensions: [], 
                        userRemoteConfigs: [[
                            url: 'https://github.com/FOSSEE/soul_d10'
                            ]]
                        )
                    }
             }
        }

        stage ('Clone DB') {
            steps {
                dir('soul_db') {
                    checkout scmGit(
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            credentialsId: 'soul',
                            url: 'https://github.com/RaghavJit/soul_db'
                        ]]
                    )
                }
            }
        }

        stage ('Create new MySQL DB') {
            steps {
                script {
                    sh "git config --global --add safe.directory ${WORKSPACE}/soul_repo"
                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/soul_repo log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()
                    
                    if (!commitMsg.toLowerCase().contains("build clean")) {
                        echo "Reusing latest DB"
                    }
                    else {
                        sh '''
                            set -e

                            echo $ROOTPASS
                            echo $SITE_DB_PASSWD

                            db_list=$(mysql -u root -p"$ROOTPASS" --silent --skip-column-names \
                                -e "SHOW DATABASES LIKE 'soul_db_jenkins_%';")

                            if [ -n "$db_list" ]; then
                                while IFS= read -r db; do
                                    suffix="${db#soul_db_jenkins_}"
                                    user="soul_user_jenkins_$suffix"

                                    mysql -u root -p"$ROOTPASS" <<EOF
DROP DATABASE IF EXISTS $db;
DROP USER IF EXISTS '$user'@'localhost';
EOF
                                done <<< "$db_list"

                                mysql -u root -p"$ROOTPASS" -e "FLUSH PRIVILEGES;"
                            fi

                            newdb="soul_db_jenkins_${BUILD_NUMBER}"
                            newuser="soul_user_jenkins_8210"

                            mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE IF NOT EXISTS $newdb;
CREATE USER IF NOT EXISTS '$newuser'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $newdb.* TO '$newuser'@'localhost';
FLUSH PRIVILEGES;
EOF

                            if [ -f "soul_db/soul_10.sql" ]; then
                                rm -rf /tmp/soul_clean.sql
                                sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' soul_db/soul_10.sql > /tmp/soul_clean.sql
                                mysql -u "$newuser" -p"$SITE_DB_PASSWD" "$newdb" < /tmp/soul_clean.sql

                            else 
                                echo 'No file soul_10.sql found'
                            fi
                        '''
                    }
                }
            }
        }

        stage('Mount Volume') { 
            steps { 
                script { 
                    sh "git config --global --add safe.directory ${WORKSPACE}/soul_repo"
                    
                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/soul_repo log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    if (!commitMsg.toLowerCase().contains("build clean")) {
                        echo "Reusing old uploads folder (osdag_uploads and osdag_public)"
                    } 
                    else {
                        echo "Recreating Podman Mount directory"
                        sh '''
                            mv /var/lib/jenkins/site_directories/soul_uploads /var/lib/jenkins/site_directories/soul_uploads.${BUILD_NUMBER}
                            mv /var/lib/jenkins/site_directories/soul_public /var/lib/jenkins/site_directories/soul_public.${BUILD_NUMBER}
                            mkdir -p /var/lib/jenkins/site_directories/soul_uploads
                            mkdir -p /var/lib/jenkins/site_directories/soul_public
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
                sh 'podman image ls --format "{{.Repository}}" | grep "^soul" | xargs -r podman image rm'
                sh """
                    podman build \
                        -t soul_image:latest \
                        -f "${WORKSPACE}/Dockerfile" \
                        --build-arg ENV_USR="soul_user_jenkins_8210" \
                        --build-arg REPO_DIR="soul_repo" \
                        --build-arg ENV_HOST="10.0.2.2" \
                        "${WORKSPACE}"
                """
            }
        }

        stage('Podman Secrets') {
            steps {
                script {
                    sh "git config --global --add safe.directory ${WORKSPACE}/soul_repo"

                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/soul_repo log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    if (!commitMsg.toLowerCase().contains("build clean")) {
                        echo "Reusing latest secrets"
                    }
                    else {
                        sh """
                            set -e

                            podman secret rm soul_mysql_db || true
                            podman secret rm soul_mysql_password || true

                            printf "soul_db_jenkins_${BUILD_NUMBER}" | podman secret create soul_mysql_db -
                            printf "${SITE_DB_PASSWD}" | podman secret create soul_mysql_password -
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

                    systemctl --user disable soul.service || true
                    systemctl --user stop soul.service || true

                    mkdir -p ~/.config/containers/systemd


                    cat > ~/.config/containers/systemd/soul.container <<EOF
[Unit]
Description=soul Persist Container
After=network-online.target
Wants=network-online.target

[Container]
Image=soul_image:latest
Pull=never
AddCapability=NET_RAW
ContainerName=soul_container
PublishPort=9108:80
Volume=/var/lib/jenkins/site_directories/soul_public:/var/www/html/sites/default/files:Z
Volume=/var/lib/jenkins/site_directories/soul_uploads:/var/www/html/soul_uploads:Z
Network=slirp4netns:allow_host_loopback=true

[Service]
Restart=unless-stopped
TimeoutStartSec=1000

[Install]
WantedBy=default.target
EOF

                    systemctl --user daemon-reload || true

                    mkdir -p ~/.config/systemd/user
                    cp /run/user/$(id -u)/systemd/generator/soul.service ~/.config/systemd/user/ || true

                    echo "Adding Podman Secrets to service file"
                    sed -i 's|podman run|podman run --secret soul_mysql_db,type=env,target=ENV_DB --secret soul_mysql_password,type=env,target=ENV_PSWD|' ~/.config/systemd/user/soul.service
                    systemctl --user daemon-reload || true
                    systemctl --user enable soul || true
                    systemctl --user start soul || true
                    '''
                }
            }
        }

        stage('Mail enable') {
            steps {
                sh '''
                    podman exec soul_container apt update
                    podman exec soul_container apt install mailutils ssmtp -y
                    
                    podman exec soul_container sh -c 'echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf'
                    podman exec soul_container sh -c 'echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                    podman exec soul_container sh -c 'echo "'${ENV_USR}':'${ENV_USR}'@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                '''
            }
        }
    }
}

