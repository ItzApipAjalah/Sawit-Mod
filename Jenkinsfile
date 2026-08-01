pipeline {
    agent any

    environment {
        // Set Java version based on branch name
        // 1.20.1, 1.20.2, 1.20.4 use Java 17
        // 1.20.5, 1.20.6, 1.21.1 use Java 21
        JAVA_HOME = "${env.BRANCH_NAME ==~ /^1\.20\.[124]$/ ? '/usr/lib/jvm/java-1.17.0-openjdk-amd64' : '/usr/lib/jvm/java-1.21.0-openjdk-amd64'}"
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Print Info') {
            steps {
                echo "Building branch: ${env.BRANCH_NAME}"
                echo "Using JAVA_HOME: ${env.JAVA_HOME}"
                sh 'java -version'
            }
        }
        stage('Build Mod') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew build'
            }
        }
    }

    post {
        always {
            // Archive output jars from all possible loader directories
            // allowEmptyArchive handles cases where a loader (like neoforge) might not exist on a specific branch
            archiveArtifacts artifacts: 'fabric/build/libs/*.jar, forge/build/libs/*.jar, quilt/build/libs/*.jar, neoforge/build/libs/*.jar, fabric-like/build/libs/*.jar', allowEmptyArchive: true
        }
        success {
            echo "Build finished successfully!"
        }
        failure {
            echo "Build failed! Please check the logs."
        }
    }
}