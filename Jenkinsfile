pipeline {
	agent any
	tools {
		maven 'Maven 3'
	}
	options {
		timeout(time: 1, unit: 'HOURS')
		skipStagesAfterUnstable()
		buildDiscarder(logRotator(numToKeepStr : "10"))
		disableConcurrentBuilds()
	}
	environment {
		VERSION = readMavenPom().getVersion()
		ARTIFACT_ID = readMavenPom().getArtifactId()
	}
	stages {
		stage('Initialisation'){
			steps {
				echo "Starting Job ${env.BUILD_NUMBER} : \n" + 
					"Artifact ID : ${ARTIFACT_ID} \n" + 
					"Version ${VERSION} \n" +
					"Branch ${GIT_BRANCH}"
			}
		}
		stage('Build') {
			steps {
				sh 'mvn clean install'
			} 
			post {
				always {
					junit 'target/surefire-reports/**/*.xml'
				}
			}
		}
		stage('SonarQube Analysis'){
			when {
				branch 'master'
			} 
			steps {
				withSonarQubeEnv('my-sonarqube') {
					sh "mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.4.0.905:sonar " +
						"-Dsonar.projectKey=Test-ci-cd-bitbucket " +
						"-Dsonar.java.source=1.8 " +
						"-Dsonar.jacoco.reportPaths=target/jacoco.exec "
				}
			}
		}
		stage('SonarQube Quality Gate'){
			when {
				branch 'master'
			}
			steps {
				script {
					def qualitygate = waitForQualityGate()
					if (qualitygate.status != "OK") {
						error "Pipeline failed due to quality gate : ${qualitygate.status}"
					} 
				}
			}
		}
		stage('Source publish'){
			when {
				branch 'master'
			}
			steps {
				script {
					def artifactoryServer = Artifactory.server('my-artifactory')
					def mavenBuild = Artifactory.newMavenBuild()
					mavenBuild.resolver server: artifactoryServer, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
					mavenBuild.deployer server: artifactoryServer, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
					mavenBuild.tool = 'Maven 3'
					def buildInfo = mavenBuild.run pom: 'pom.xml', goals: 'clean install'
					artifactoryServer.publishBuildInfo buildInfo
				}
			}
		}
		stage('Deploy'){
			when {
				branch 'master'
			}
			options {
                timeout(time: 15, unit: 'MINUTES') 
            }
			environment {
				SERVER_CREDENTIAL = credentials('tomcat-credential')
			}
			steps {
				script {
					def serverIP = "192.168.1.41:8888"
					echo "Deploying War file to Tomcat ${serverIP}"
					sh "curl -u ${SERVER_CREDENTIAL_USR}:${SERVER_CREDENTIAL_PSW} http://${serverIP}/manager/text/undeploy?path=/test"
					sh "curl --upload-file target/Test.war -X PUT -u ${SERVER_CREDENTIAL_USR}:${SERVER_CREDENTIAL_PSW} http://${serverIP}/manager/text/deploy?path=/test"
				}
			}
		}
	}
	post {
		always {
			echo 'Doing reporting'
		}
		success {
			script {
				if(GIT_BRANCH == 'origin/master'){
					slackSend color: 'good', 
						message: "Build successfull ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
			}
		}
		failure {
			script {
				targetEmail = 'Last commiter'
				if(GIT_BRANCH == 'origin/master'){
					targetEmail = 'Team'
					slackSend color: 'danger', 
						message: "Build failure ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
				echo "Sending email to ${targetEmail}"
			}
		}
		fixed {
			script {
				targetEmail = 'Last commiter'
				if(GIT_BRANCH == 'origin/master'){
					targetEmail = 'Team'
					slackSend color: 'good', 
						message: "Build back to normal ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
				echo "Sending email to ${targetEmail}"
			}
		}
		cleanup {
			deleteDir()
		}
	}
}