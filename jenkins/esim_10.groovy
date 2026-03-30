pipeline {
    agent any

    environment {
        ROOTPASS        = credentials('mysql')
        SITE_DB_PASSWD  = credentials('esim_user_password')
    }

    stages {

        stage ('Stop site') {
            steps {
                sh ''' 
                    export XDG_RUNTIME_DIR="/run/user/$(id -u)"
                    export DBUS_SESSION_BUS_ADDRESS="unix:path=\${XDG_RUNTIME_DIR}/bus"

                    systemctl --user disable esim.service || true
                    systemctl --user stop esim.service || true
                '''
            }
        }

        stage ('Clone Repo') {
            steps {
                dir('esim_repo') {
                    checkout scmGit(
                        branches: [[name: '*/main']], 
                        extensions: [], 
                        userRemoteConfigs: [[
                            url: 'https://github.com/nikitasabale96/eSim_docker'
                            ]]
                        )
                    }
             }
        }

        stage ('Clone DB') {
            steps {
                dir('esim_db') {
                    checkout scmGit(
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            credentialsId: 'esim',
                            url: 'https://github.com/RaghavJit/esim_db'
                        ]]
                    )
                }
            }
        }

        stage ('Create new MySQL DB') {
            steps {
                script {
                    sh "git config --global --add safe.directory ${WORKSPACE}/esim_repo"
                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/esim_repo log -1 --pretty=%B",
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
                                -e "SHOW DATABASES LIKE 'esim_db_jenkins_%';")

                            if [ -n "$db_list" ]; then
                                while IFS= read -r db; do
                                    suffix="${db#esim_db_jenkins_}"
                                    user="esim_user_jenkins_$suffix"

                                    mysql -u root -p"$ROOTPASS" <<EOF
DROP DATABASE IF EXISTS $db;
DROP USER IF EXISTS '$user'@'localhost';
EOF
                                done <<< "$db_list"

                                mysql -u root -p"$ROOTPASS" -e "FLUSH PRIVILEGES;"
                            fi

                            newdb="esim_db_jenkins_${BUILD_NUMBER}"
                            newuser="esim_user_jenkins_6630"

                            mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE IF NOT EXISTS $newdb;
CREATE USER IF NOT EXISTS '$newuser'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $newdb.* TO '$newuser'@'localhost';
FLUSH PRIVILEGES;
EOF

                            if [ -f "esim_db/esim_10.sql" ]; then
                                rm -rf /tmp/esim_clean.sql
                                sed -e '/^CREATE DATABASE/d' -e '/^USE[[:space:]]/d' esim_db/esim_10.sql > /tmp/esim_clean.sql
                                mysql -u "$newuser" -p"$SITE_DB_PASSWD" "$newdb" < /tmp/esim_clean.sql

                            else 
                                echo 'No file esim_10.sql found'
                            fi
                        '''
                    }
                }
            }
        }

        stage('Mount Volume') { 
            steps { 
                script { 
                    sh "git config --global --add safe.directory ${WORKSPACE}/esim_repo"
                    
                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/esim_repo log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    if (commitMsg.toLowerCase().contains("build live")) {
                        echo "Reusing old volume (esim_vol)"
                    } 
                    else {
                        echo "Recreating Podman Volume: esim_vol"
                        sh '''
                            if podman volume inspect esim_vol >/dev/null 2>&1; then
                                podman volume rm -f esim_vol
                            fi

                            podman volume create esim_vol
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
                sh 'podman image ls --format "{{.Repository}}" | grep "^esim" | xargs -r podman image rm'
                sh """
                    podman build \
                        -t esim_image:latest \
                        -f "${WORKSPACE}/Dockerfile" \
                        --build-arg ENV_USR="esim_user_jenkins_6630" \
                        --build-arg REPO_DIR="esim_repo" \
                        --build-arg ENV_HOST="10.0.2.2" \
                        "${WORKSPACE}"
                """
            }
        }

        stage('Podman Secrets') {
            steps {
                script {
                    sh "git config --global --add safe.directory ${WORKSPACE}/esim_repo"

                    def commitMsg = sh(
                        script: "git -C ${WORKSPACE}/esim_repo log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    if (commitMsg.toLowerCase().contains("build live")) {
                        echo "Reusing old secrets"
                    }
                    else {
                        sh """
                            set -e

                            podman secret rm esim_mysql_db || true
                            podman secret rm esim_mysql_password || true

                            printf "esim_db_jenkins_${BUILD_NUMBER}" | podman secret create esim_mysql_db -
                            printf "${SITE_DB_PASSWD}" | podman secret create esim_mysql_password -
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

                    systemctl --user disable esim.service || true
                    systemctl --user stop esim.service || true

                    mkdir -p ~/.config/containers/systemd


                    cat > ~/.config/containers/systemd/esim.container <<EOF
[Unit]
Description=esim Persist Container
After=network-online.target
Wants=network-online.target

[Container]
Image=esim_image:latest
Pull=never
AddCapability=NET_RAW
ContainerName=esim_container
PublishPort=9110:80
Volume=esim_vol:/var/www/html/sites/default/files:Z
Network=slirp4netns:allow_host_loopback=true

[Service]
Restart=unless-stopped
TimeoutStartSec=1000

[Install]
WantedBy=default.target
EOF

                    systemctl --user daemon-reload || true

                    mkdir -p ~/.config/systemd/user
                    cp /run/user/$(id -u)/systemd/generator/esim.service ~/.config/systemd/user/ || true

                    echo "Adding Podman Secrets to service file"
                    sed -i 's|podman run|podman run --secret esim_mysql_db,type=env,target=ENV_DB --secret esim_mysql_password,type=env,target=ENV_PSWD|' ~/.config/systemd/user/esim.service
                    systemctl --user daemon-reload || true
                    systemctl --user enable esim || true
                    systemctl --user start esim || true
                    '''
                }
            }
        }

        stage('Mail enable') {
            steps {
                sh '''
                    podman exec esim_container apt update
                    podman exec esim_container apt install mailutils ssmtp -y
                    
                    podman exec esim_container sh -c 'echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf'
                    podman exec esim_container sh -c 'echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                    podman exec esim_container sh -c 'echo "'${ENV_USR}':'${ENV_USR}'@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                '''
            }
        }
    }
}

