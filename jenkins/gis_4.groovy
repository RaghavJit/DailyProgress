pipeline {
    agent any

    environment {
        ROOTPASS        = credentials('mysql')
        SITE_DB_PASSWD  = credentials('gis_user_password')
    }

    stages {
        


        stage ('Clone Repo') {
            steps {
                dir('gis_repo') {
                    checkout scmGit(
                        branches: [[name: '*/docker_gis']], 
                        extensions: [], 
                        userRemoteConfigs: [[
                            url: 'https://github.com/FOSSEE/gis_website/tree/docker_gis'
                            ]]
                        )
                    }
            }
        }

        stage ('Clone DB') {
            steps {
                dir('gis_db') {
                    checkout scmGit(
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            credentialsId: 'gis',
                            url: 'https://github.com/RaghavJit/gis_db'
                        ]]
                    )
                }
            }
        }

        stage ('Create new MySQL DB') {
            steps {
                script {
                    sh '''
                    set -e

                    echo $ROOTPASS
                    echo $SITE_DB_PASSWD

                    db_list=$(mysql -u root -p"$ROOTPASS" --silent --skip-column-names \
                        -e "SHOW DATABASES LIKE 'gis_db_jenkins_%';")

                    if [ -n "$db_list" ]; then
                        while IFS= read -r db; do
                            suffix="${db#gis_db_jenkins_}"
                            user="gis_user_jenkins_$suffix"

                            mysql -u root -p"$ROOTPASS" <<EOF
DROP DATABASE IF EXISTS $db;
DROP USER IF EXISTS '$user'@'localhost';
EOF
                        done <<< "$db_list"

                        mysql -u root -p"$ROOTPASS" -e "FLUSH PRIVILEGES;"
                    fi

                    newdb="gis_db_jenkins_${BUILD_NUMBER}"
                    newuser="gis_user_jenkins_${BUILD_NUMBER}"

                    mysql -u root -p"$ROOTPASS" <<EOF
CREATE DATABASE IF NOT EXISTS $newdb;
CREATE USER IF NOT EXISTS '$newuser'@'localhost' IDENTIFIED BY '$SITE_DB_PASSWD';
GRANT ALL PRIVILEGES ON $newdb.* TO '$newuser'@'localhost';
FLUSH PRIVILEGES;
EOF

                    if [ -f "gis_db/gis_10.sql" ]; then
                        mysql -u "$newuser" -p"$SITE_DB_PASSWD" "$newdb" < gis_db/gis_10.sql
                    else 
                        echo 'No file gis_10.sql found'
                    fi
                    '''
                }
            } 
        }

        stage('Mount Volume') { 
            steps { 
                script { 
                    sh "git config --global --add safe.directory ${WORKSPACE}/gis_repo" 
                    
                    def commitMsg = sh( script: "git -C ${WORKSPACE}/gis_repo log -1 --pretty=%B", returnStdout: true ).trim() 

                    echo "Commit Message: ${commitMsg}" 
                    if (commitMsg.toLowerCase().contains("new volume")) { 
                        echo "Creating Podman Volume: gis_vol" 
                        sh "podman volume create gis_vol_${env.BUILD_NUMBER}" 
                    } 
                    else { 
                        echo "Reusing old volume" 
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
                sh 'podman image ls --format "{{.Repository}}" | grep "^gis" | xargs -r podman image rm'
                sh """
                    podman build \
                        -t gis_image:latest \
                        -f "${WORKSPACE}/Dockerfile" \
                        --build-arg ENV_USR="gis_user_jenkins_${env.BUILD_NUMBER}" \
                        --build-arg REPO_DIR="gis_repo" \
                        --build-arg ENV_HOST="10.0.2.2" \
                        "${WORKSPACE}"
                """
            }
        }

        stage('Podman Secrets') {
            steps {
                sh """
                    set -e

                    podman secret rm gis_mysql_db || true
                    podman secret rm gis_mysql_password || true

                    printf "gis_db_jenkins_${BUILD_NUMBER}" | podman secret create gis_mysql_db -
                    printf "${SITE_DB_PASSWD}" | podman secret create gis_mysql_password -
                """
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

                    systemctl --user disable gis.service || true
                    systemctl --user stop gis.service || true

                    mkdir -p ~/.config/containers/systemd


                    cat > ~/.config/containers/systemd/gis.container <<EOF
[Unit]
Description=gis Persist Container
After=network-online.target
Wants=network-online.target

[Container]
Image=gis_image:latest
Pull=never
AddCapability=NET_RAW
ContainerName=gis_container
PublishPort=9104:80
Volume=gis_vol:/var/www/html/sites/default/files:Z
Network=slirp4netns:allow_host_loopback=true

[Service]
Restart=unless-stopped
TimeoutStartSec=1000

[Install]
WantedBy=default.target
EOF

                    systemctl --user daemon-reload || true

                    mkdir -p ~/.config/systemd/user
                    cp /run/user/$(id -u)/systemd/generator/gis.service ~/.config/systemd/user/ || true

                    echo "Adding Podman Secrets to service file"
                    sed -i 's|podman run|podman run --secret gis_mysql_db,type=env,target=ENV_DB --secret gis_mysql_password,type=env,target=ENV_PSWD|' ~/.config/systemd/user/gis.service
                    systemctl --user daemon-reload || true
                    systemctl --user enable gis || true
                    systemctl --user start gis || true
                    '''
                }
            }
        }

        stage('Mail enable') {
            steps {
                sh '''
                    podman exec gis_container apt update
                    podman exec gis_container apt install mailutils ssmtp -y
                    
                    podman exec gis_container sh -c 'echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf'
                    podman exec gis_container sh -c 'echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                    podman exec gis_container sh -c 'echo "'${ENV_USR}':'${ENV_USR}'@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
                '''
            }
        }
    }
}

