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
                sh './gradlew clean build -x test --no-daemon'
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
		        withCredentials([
		            string(credentialsId: 'HOTEL_DB_JDBC_URL', variable: 'DB_JDBC_URL'),
		            string(credentialsId: 'HOTEL_DB_USERNAME', variable: 'DB_USERNAME'),
		            string(credentialsId: 'HOTEL_DB_PASSWORD', variable: 'DB_PASSWORD'),
		
		            string(credentialsId: 'HOTEL_MAIL_USERNAME', variable: 'MAIL_USERNAME'),
		            string(credentialsId: 'HOTEL_MAIL_PASSWORD', variable: 'MAIL_PASSWORD'),
		            string(credentialsId: 'HOTEL_APP_MAIL_FROM', variable: 'APP_MAIL_FROM'),
		
		            string(credentialsId: 'HOTEL_KAKAO_CLIENT_ID', variable: 'KAKAO_CLIENT_ID'),
		            string(credentialsId: 'HOTEL_KAKAO_CLIENT_SECRET', variable: 'KAKAO_CLIENT_SECRET'),
		
		            string(credentialsId: 'HOTEL_NAVER_CLIENT_ID', variable: 'NAVER_CLIENT_ID'),
		            string(credentialsId: 'HOTEL_NAVER_CLIENT_SECRET', variable: 'NAVER_CLIENT_SECRET'),
		
		            string(credentialsId: 'HOTEL_IAMPORT_API_KEY', variable: 'IAMPORT_API_KEY'),
		            string(credentialsId: 'HOTEL_IAMPORT_API_SECRET', variable: 'IAMPORT_API_SECRET'),
		
		            string(credentialsId: 'HOTEL_COOLSMS_API_KEY', variable: 'COOLSMS_API_KEY'),
		            string(credentialsId: 'HOTEL_COOLSMS_API_SECRET', variable: 'COOLSMS_API_SECRET'),
		            string(credentialsId: 'HOTEL_COOLSMS_FROM', variable: 'COOLSMS_FROM'),
		
		            string(credentialsId: 'HOTEL_JWT_SECRET', variable: 'JWT_SECRET')
		        ]) {
		            sshagent(['SERVER_SSH_KEY']) {
		                sh """
		                    ssh -o StrictHostKeyChecking=no ubuntu@$SERVER_IP "
		                        docker stop $CONTAINER_NAME || true
		                        docker rm $CONTAINER_NAME || true
		                        docker pull $DOCKER_IMAGE:latest
		                        docker run -d \\
		                          --name $CONTAINER_NAME \\
		                          -p 8001:8001 \\
		                          -v /home/ubuntu/file_images:/app/file_images \\
		                          -e SPRING_PROFILES_ACTIVE=local \\
		                          -e SERVER_PORT=8001 \\
		                          -e DB_JDBC_URL='$DB_JDBC_URL' \\
		                          -e DB_USERNAME='$DB_USERNAME' \\
		                          -e DB_PASSWORD='$DB_PASSWORD' \\
		                          -e MAIL_USERNAME='$MAIL_USERNAME' \\
		                          -e MAIL_PASSWORD='$MAIL_PASSWORD' \\
		                          -e APP_MAIL_FROM='$APP_MAIL_FROM' \\
		                          -e KAKAO_CLIENT_ID='$KAKAO_CLIENT_ID' \\
		                          -e KAKAO_CLIENT_SECRET='$KAKAO_CLIENT_SECRET' \\
		                          -e NAVER_CLIENT_ID='$NAVER_CLIENT_ID' \\
		                          -e NAVER_CLIENT_SECRET='$NAVER_CLIENT_SECRET' \\
		                          -e IAMPORT_API_KEY='$IAMPORT_API_KEY' \\
		                          -e IAMPORT_API_SECRET='$IAMPORT_API_SECRET' \\
		                          -e COOLSMS_API_KEY='$COOLSMS_API_KEY' \\
		                          -e COOLSMS_API_SECRET='$COOLSMS_API_SECRET' \\
		                          -e COOLSMS_FROM='$COOLSMS_FROM' \\
		                          -e JWT_SECRET='$JWT_SECRET' \\
		                          $DOCKER_IMAGE:latest
		                    "
		                """
		            }
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