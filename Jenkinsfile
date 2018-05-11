#!groovy
pipeline {
	agent any
	tools {
		maven 'Maven 3'
	}
	options {
		timeout(time: 90, unit: 'MINUTES')
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
				anyOf{ branch 'master'; branch 'develop' }
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
				anyOf{ branch 'master'; branch 'develop' }
			}
			steps {
				script {
					def qualitygate = waitForQualityGate()
					if ("OK" != qualitygate.status) {
						error "Pipeline failed due to quality gate : ${qualitygate.status}"
					} 
				}
			}
		}
		stage('Publish Snapshot'){
			when {
				branch 'develop'
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
		stage('Deploy Staging'){
			when {
				branch 'develop'
			}
			options {
                timeout(time: 15, unit: 'MINUTES') 
            }
			environment {
				STAGING_SERVER_CREDENTIAL = credentials('tomcat-credential')
				STAGING_SERVER_IP = '192.168.1.41:8888'
			}
			steps {
				script {
					echo "Undeploying Webapp to Tomcat ${STAGING_SERVER_IP}"
					sh "curl " + 
						"-u ${STAGING_SERVER_CREDENTIAL_USR}:${STAGING_SERVER_CREDENTIAL_PSW} " +
						"http://${STAGING_SERVER_IP}/manager/text/undeploy?path=/test"

					echo "Deploying War file to Tomcat ${STAGING_SERVER_IP}"
					sh "curl -X PUT " +
						"-o /dev/null -w \"%{http_code}\" " +
						"-u ${STAGING_SERVER_CREDENTIAL_USR}:${STAGING_SERVER_CREDENTIAL_PSW} " +
						"--upload-file target/Test.war " +
						"http://${STAGING_SERVER_IP}/manager/text/deploy?path=/test " +
						"> status.txt"
					
				
					def deployStatus = readFile 'status.txt'
					if( "200" !=  deployStatus) {
						error "Failed to deploy the War file to server ${SERVER_IP}, response status ${deployStatus}"
					}
				}
			}
		}
		stage('Publish release'){
			when {
				branch 'master'
				buildingTag()
			}
			steps {
				echo "Publishing release ${PERSON}"
			}
		}
		stage('Deploy release'){
			when {
				branch 'master'
				buildingTag()
			}
			options {
                timeout(time: 15, unit: 'MINUTES') 
            }
			steps {
				echo 'Deploy to release staging'
			}
		}
	}
	post {
		always {
			echo 'Doing report'
		}
		success {
			script {
				if('master' == GIT_BRANCH){
					slackSend color: 'good', 
						message: "Build successfull ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
			}
		}
		failure {
			script {
				targetEmail = sh ( script : 'git log -1 --format=%ae $( git rev-parse HEAD )', returnStdout: true ).trim()
				if('master' == GIT_BRANCH) {
					targetEmail = 'team@mail.com'
					slackSend color: 'danger', 
						message: "Build failure ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
				echo "Sending email to ${targetEmail}"
			}
		}
		fixed {
			script {
				targetEmail = sh ( script : 'git log -1 --format=%ae $( git rev-parse HEAD )', returnStdout: true ).trim()
				if('master' == GIT_BRANCH) {
					targetEmail = 'team@mail.com'
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