pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        DOCKER_IMAGE  = 'jehyung/final_hotelmain'
        SERVICE_NAME  = 'hotelmain'
        CONTAINER_NAME = 'final_hotelmain'
        DEPLOY_DIR    = '/home/chris/ciel'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build with Gradle') {
            steps {
                sh '''
                    set -eu

                    chmod +x ./gradlew
                    ./gradlew clean build -x test --no-daemon
                '''
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub_info',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    ),
                    sshUserPrivateKey(
                        credentialsId: 'CIEL_MINIPC_SSH',
                        keyFileVariable: 'CIEL_SSH_KEY',
                        usernameVariable: 'CIEL_SSH_USER'
                    )
                ]) {
                    sh '''
                        set -eu
                        set +x

                        KNOWN_HOSTS="$(mktemp)"
                        JOB_SAFE="$(
                            printf '%s' "$JOB_NAME" |
                            tr -c 'A-Za-z0-9_.-' '_'
                        )"

                        REMOTE_DOCKER_CONFIG="/tmp/ciel-docker-${JOB_SAFE}-${BUILD_NUMBER}"
                        SSH_TARGET="${CIEL_SSH_USER}@127.0.0.1"

                        cleanup() {
                            ssh -T \
                                -i "$CIEL_SSH_KEY" \
                                -o BatchMode=yes \
                                -o StrictHostKeyChecking=accept-new \
                                -o UserKnownHostsFile="$KNOWN_HOSTS" \
                                "$SSH_TARGET" \
                                "rm -rf '$REMOTE_DOCKER_CONFIG'" \
                                >/dev/null 2>&1 || true

                            rm -f "$KNOWN_HOSTS"
                        }

                        trap cleanup EXIT

                        ssh -T \
                            -i "$CIEL_SSH_KEY" \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=accept-new \
                            -o UserKnownHostsFile="$KNOWN_HOSTS" \
                            "$SSH_TARGET" \
                            "mkdir -p '$REMOTE_DOCKER_CONFIG' &&
                             chmod 700 '$REMOTE_DOCKER_CONFIG'"

                        printf '%s' "$DOCKER_PASS" |
                        ssh -T \
                            -i "$CIEL_SSH_KEY" \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=accept-new \
                            -o UserKnownHostsFile="$KNOWN_HOSTS" \
                            "$SSH_TARGET" \
                            "DOCKER_CONFIG='$REMOTE_DOCKER_CONFIG'
                             docker login
                             -u '$DOCKER_USER'
                             --password-stdin"

                        tar -cf - \
                            Dockerfile \
                            build/libs/*.jar |
                        ssh -T \
                            -i "$CIEL_SSH_KEY" \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=accept-new \
                            -o UserKnownHostsFile="$KNOWN_HOSTS" \
                            "$SSH_TARGET" "
                                set -eu

                                export DOCKER_CONFIG='$REMOTE_DOCKER_CONFIG'

                                docker build \
                                    --pull \
                                    -t '$DOCKER_IMAGE:latest' \
                                    -

                                docker push '$DOCKER_IMAGE:latest'
                            "
                    '''
                }
            }
        }

        stage('Deploy to MiniPC') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'CIEL_CREDENTIALS_ENC',
                        variable: 'CIEL_CREDENTIALS_FILE'
                    ),
                    string(
                        credentialsId: 'CIEL_EXPORT_PASSWORD',
                        variable: 'CIEL_EXPORT_PASSWORD'
                    ),
                    sshUserPrivateKey(
                        credentialsId: 'CIEL_MINIPC_SSH',
                        keyFileVariable: 'CIEL_SSH_KEY',
                        usernameVariable: 'CIEL_SSH_USER'
                    )
                ]) {
                    sh '''
                        set -eu
                        set +x

                        KNOWN_HOSTS="$(mktemp)"
                        trap 'rm -f "$KNOWN_HOSTS"' EXIT

                        REMOTE_SCRIPT_B64="$(cat <<'PY' | base64 -w 0
import json
import os
import subprocess
import sys

data = json.load(sys.stdin)

required = [
    "DB_USERNAME",
    "DB_PASSWORD",
    "MAIL_USERNAME",
    "MAIL_PASSWORD",
    "APP_MAIL_FROM",
    "KAKAO_CLIENT_ID",
    "KAKAO_CLIENT_SECRET",
    "NAVER_CLIENT_ID",
    "NAVER_CLIENT_SECRET",
    "IAMPORT_API_KEY",
    "IAMPORT_API_SECRET",
    "COOLSMS_API_KEY",
    "COOLSMS_API_SECRET",
    "COOLSMS_FROM",
    "JWT_SECRET"
]

missing = [
    key for key in required
    if key not in data
    or data[key] is None
    or str(data[key]) == ""
]

if missing:
    raise SystemExit(
        "Missing credentials: " + ", ".join(missing)
    )

environment = os.environ.copy()

for key in required:
    environment[key] = str(data[key])

deploy_directory = "/home/chris/ciel"

subprocess.run(
    [
        "docker",
        "compose",
        "pull",
        "hotelmain"
    ],
    cwd=deploy_directory,
    env=environment,
    check=True
)

subprocess.run(
    [
        "docker",
        "compose",
        "up",
        "-d",
        "--no-deps",
        "hotelmain"
    ],
    cwd=deploy_directory,
    env=environment,
    check=True
)
PY
)"

                        openssl enc -d \
                            -aes-256-cbc \
                            -pbkdf2 \
                            -iter 200000 \
                            -in "$CIEL_CREDENTIALS_FILE" \
                            -pass env:CIEL_EXPORT_PASSWORD |
                        ssh -T \
                            -i "$CIEL_SSH_KEY" \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=accept-new \
                            -o UserKnownHostsFile="$KNOWN_HOSTS" \
                            "$CIEL_SSH_USER@127.0.0.1" \
                            "python3 -c \\"import base64; exec(base64.b64decode('$REMOTE_SCRIPT_B64'))\\""

                        ssh -T \
                            -i "$CIEL_SSH_KEY" \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=accept-new \
                            -o UserKnownHostsFile="$KNOWN_HOSTS" \
                            "$CIEL_SSH_USER@127.0.0.1" '
                                set -eu

                                STARTED=0

                                for count in \
                                    1 2 3 4 5 6 \
                                    7 8 9 10 11 12
                                do
                                    STATUS="$(
                                        docker inspect \
                                            -f "{{.State.Status}}" \
                                            final_hotelmain \
                                            2>/dev/null || true
                                    )"

                                    if [ "$STATUS" != "running" ]; then
                                        echo "HotelMain container status=$STATUS"
                                        docker logs --tail 120 final_hotelmain || true
                                        exit 1
                                    fi

                                    if docker logs final_hotelmain 2>&1 |
                                        grep -q "Started "
                                    then
                                        STARTED=1
                                        break
                                    fi

                                    sleep 5
                                done

                                echo "===== Container ====="

                                docker ps \
                                    --filter name=final_hotelmain \
                                    --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"

                                echo "===== Logs ====="

                                docker logs \
                                    --tail 120 \
                                    final_hotelmain

                                test "$STARTED" = "1"
                            '
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'HotelMain build and MiniPC deployment completed.'
        }

        failure {
            echo 'HotelMain pipeline failed.'
        }
    }
}
