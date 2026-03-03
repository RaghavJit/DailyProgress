pipeline {
    agent any

    environment {
        ROOTPASS        = credentials('mysql')
        SITE_DB_PASSWD  = credentials('freecad_user_password')
    }

    stages {

        stage ('Stop site') {
            steps {
                sh ''' 
                    export XDG_RUNTIME_DIR="/run/user/$(id -u)"
                    export DBUS_SESSION_BUS_ADDRESS="unix:path=\${XDG_RUNTIME_DIR}/bus"

                    systemctl --user disable freecad.service || true
                    systemctl --user stop freecad.service || true
                '''
            }
        }

        stage ('Clone Repo') {
            steps {
                dir('freecad_repo') {
                    checkout scmGit(
                        branches: [[name: '*/master']], 
                        extensions: [], 
                        userRemoteConfigs: [[
                            url: 'https://github.com/nikitasabale96/Freecad_Docker'
                            ]]
                        )
                    }
             }
        }

        stage ('Clone DB') {
            steps {
                dir('freecad_db') {
                    checkout scmGit(
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            credentialsId: 'freecad',
                            url: 'https://github.com/RaghavJit/freecad_db'
                        ]]
                    )
                }
            }
        }

        stage ('Create new MySQL DB') {
            steps {
                script {
                    sh "git config --global --add safe.directory ${WORKSPACE}/freecad_repo"
                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/freecad_repo log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()
                    
                    if (commitMsg.toLowerCase().contains("build live")) {
                        echo "Reusing old DB"
                    }
                    else {
                        sh '''
                            set -e

                            echo $ROOTPASS
                            echo $SITE_DB_PASSWD

                            db_list=$(mysql -u root -p"$ROOTPASS" --silent --skip-column-names \
                                -e "SHOW DATABASES LIKE 'freecad_db_jenkins_%';")

                            if [ -n "$db_list" ]; then
                                while IFS= read -r db; do
                                    suffix="${db#freecad_db_jenkins_}"
                                    user="freecad_user_jenkins_$suffix"

                                    mysql -u root -p"$ROOTPASS" <<EOF
DROP DATABASE IF EXISTS $db;
DROP USER IF EXISTS '$user'@'localhost';
EOF
                                done <<< "$db_list"

                                mysql -u root -p"$ROOTPASS" -e "FLUSH PRIVILEGES;"
                            fi

                            newdb="freecad_db_jenkins_${BUILD_NUMBER}"
                            newuser="freecad_user_jenkins_5224"

                            mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE IF NOT EXISTS $newdb;
CREATE USER IF NOT EXISTS '$newuser'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $newdb.* TO '$newuser'@'localhost';
FLUSH PRIVILEGES;
EOF

                            if [ -f "freecad_db/freecad_10.sql" ]; then
                                rm -rf /tmp/freecad_clean.sql
                                sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' freecad_db/freecad_10.sql > /tmp/freecad_clean.sql
                                mysql -u "$newuser" -p"$SITE_DB_PASSWD" "$newdb" < /tmp/freecad_clean.sql

                            else 
                                echo 'No file freecad_10.sql found'
                            fi
                        '''
                    }
                }
            }
        }

        stage('Mount Volume') { 
            steps { 
                script { 
                    sh "git config --global --add safe.directory ${WORKSPACE}/freecad_repo"
                    
                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/freecad_repo log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    if (commitMsg.toLowerCase().contains("build live")) {
                        echo "Reusing old volume (freecad_vol)"
                    } 
                    else {
                        echo "Recreating Podman Volume: freecad_vol"
                        sh '''
                            if podman volume inspect freecad_vol >/dev/null 2>&1; then
                                podman volume rm -f freecad_vol
                            fi

                            podman volume create freecad_vol
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
                sh 'podman image ls --format "{{.Repository}}" | grep "^freecad" | xargs -r podman image rm'
                sh """
                    podman build \
                        -t freecad_image:latest \
                        -f "${WORKSPACE}/Dockerfile" \
                        --build-arg ENV_USR="freecad_user_jenkins_5224" \
                        --build-arg REPO_DIR="freecad_repo" \
                        --build-arg ENV_HOST="10.0.2.2" \
                        "${WORKSPACE}"
                """
            }
        }

        stage('Podman Secrets') {
            steps {
                script {
                    sh "git config --global --add safe.directory ${WORKSPACE}/freecad_repo"

                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/freecad_repo log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    if (commitMsg.toLowerCase().contains("build live")) {
                        echo "Reusing old secrets"
                    }
                    else {
                        sh """
                            set -e

                            podman secret rm freecad_mysql_db || true
                            podman secret rm freecad_mysql_password || true

                            printf "freecad_db_jenkins_${BUILD_NUMBER}" | podman secret create freecad_mysql_db -
                            printf "${SITE_DB_PASSWD}" | podman secret create freecad_mysql_password -
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

                    systemctl --user disable freecad.service || true
                    systemctl --user stop freecad.service || true

                    mkdir -p ~/.config/containers/systemd


                    cat > ~/.config/containers/systemd/freecad.container <<EOF
[Unit]
Description=freecad Persist Container
After=network-online.target
Wants=network-online.target

[Container]
Image=freecad_image:latest
Pull=never
AddCapability=NET_RAW
ContainerName=freecad_container
PublishPort=9103:80
Volume=freecad_vol:/var/www/html/sites/default/files:Z
Network=slirp4netns:allow_host_loopback=true

[Service]
Restart=unless-stopped
TimeoutStartSec=1000

[Install]
WantedBy=default.target
EOF

                    systemctl --user daemon-reload || true

                    mkdir -p ~/.config/systemd/user
                    cp /run/user/$(id -u)/systemd/generator/freecad.service ~/.config/systemd/user/ || true

                    echo "Adding Podman Secrets to service file"
                    sed -i 's|podman run|podman run --secret freecad_mysql_db,type=env,target=ENV_DB --secret freecad_mysql_password,type=env,target=ENV_PSWD|' ~/.config/systemd/user/freecad.service
                    systemctl --user daemon-reload || true
                    systemctl --user enable freecad || true
                    systemctl --user start freecad || true
                    '''
                }
            }
        }

        stage('Mail enable') {
            steps {
                sh '''
                    podman exec freecad_container apt update
                    podman exec freecad_container apt install mailutils ssmtp -y
                    
                    podman exec freecad_container sh -c 'echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf'
                    podman exec freecad_container sh -c 'echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                    podman exec freecad_container sh -c 'echo "'${ENV_USR}':'${ENV_USR}'@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                '''
            }
        }
    }
}
