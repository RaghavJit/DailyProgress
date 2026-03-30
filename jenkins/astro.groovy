pipeline {
    agent any

    stages {

        stage ('Stop site') {
            steps {
                sh ''' 
                '''
            }
        }

        stage ('Clone Repo') {
            steps {
                dir('arduino_repo') {
                    checkout scmGit(
                        branches: [[name: '*/main']], 
                        extensions: [], 
                        userRemoteConfigs: [[
                            url: ''
                            ]]
                        )
                    }
             }
        }

        stage ('Make domain') {
            steps {
                script {
                        
                    }
                }
            }
        }

        stage('Generate certs') { 
            steps { 
                script {
                        
                    }
                }
            }
        }

        stage ('Nginx Config') {
            steps {
                script {
                        
                    }
            }
        }
    }
}
