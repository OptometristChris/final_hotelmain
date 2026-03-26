pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "jehyung/final_hotelmain"
        SERVER_IP = "52.78.215.25"
        CONTAINER_NAME = "final_hotelmain"
    }

    tools {
        jdk 'jdk17'   // Jenkins 관리 > Tools > JDK installations 의 JDK Name 에 입력한 이름
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build with Gradle') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build'
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub_info', // 반드시 Jenkins 설치시 New credentials 에서 Username with password 에서 입력하였던 ID 이름을 넣어야 함. 
                    usernameVariable: 'DOCKER_USER', // Jenkins 내부에서 쓰는 환경 변수 이름이므로 그대로 써야함. 바꾸면 안됨. 
                    passwordVariable: 'DOCKER_PASS'  // Jenkins 내부에서 쓰는 환경 변수 이름이므로 그대로 써야함. 바꾸면 안됨. 
                )]) {

                    sh '''
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker build -t $DOCKER_IMAGE:latest .
                        docker push $DOCKER_IMAGE:latest
                    '''
                }
            }
        }

        stage('Deploy to Server') {
            steps {
                sshagent(['SERVER_SSH_KEY']) {  // 반드시 Jenkins 설치시 New credentials 에서 SSH Username with private key 에서 입력하였던 ID 이름을 넣어야 함.
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@$SERVER_IP '
                            docker stop $CONTAINER_NAME || true
                            docker rm $CONTAINER_NAME || true
                            docker pull $DOCKER_IMAGE:latest
                            docker run -d --name $CONTAINER_NAME -p 8001:8001 -v /home/ubuntu/file_images:/app/file_images $DOCKER_IMAGE:latest
                        '
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Deployment completed successfully."
        }
        failure {
            echo "Deployment failed."
        }
    }
}