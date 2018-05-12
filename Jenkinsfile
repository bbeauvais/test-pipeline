#!/usr/bin/env groovy
pipeline {
	agent any
	tools {
		maven 'Maven 3' // Using the Maven tool configured in Jenkins
	}
	options {
		timeout(time: 90, unit: 'MINUTES') // Make the pipeline fail after 90 minutes
		skipStagesAfterUnstable() // Every step after the build goes to unstable stage are ignored
		buildDiscarder(logRotator(numToKeepStr : '10')) // Keep on the last 10 build on Jenkins
		disableConcurrentBuilds() // Block the execution of the same pipeline in parralel
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
					// Reporting JUnit result to Jenkins UI
					junit 'target/surefire-reports/**/*.xml'
				}
			}
		}
		stage('SonarQube Analysis'){
			when {
				// Execute the stage only if the build is on master or develop branch
				anyOf{ branch 'master'; branch 'develop' }
			} 
			steps {
				// Sonar analysis with the my-sonarqube configuration made in Jenkins
				withSonarQubeEnv('my-sonarqube') {
					sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.4.0.905:sonar ' +
						'-Dsonar.projectKey=Test-ci-cd-bitbucket ' +
						'-Dsonar.java.source=1.8 ' +
						'-Dsonar.jacoco.reportPaths=target/jacoco.exec '
				}
			}
		}
		stage('SonarQube Quality Gate'){
			when {
				anyOf{ branch 'master'; branch 'develop' }
			}
			steps {
				script {
					// Wait for the Webhook between Sonar and Jenkins to return Gality gate status
					def qualitygate = waitForQualityGate()
					// If quality gate not passed then abort the build
					if ("OK" != qualitygate.status) {
						error "Pipeline failed due to quality gate : ${qualitygate.status}"
					} 
				}
			}
		}
		stage('Publish Snapshot'){
			when {
				// Execute step on when on branch develop
				branch 'develop'
			}
			steps {
				echo "Publishing Snapshot ${VERSION}"
				script {
					// Retrievning the Artifactory server configured in Jenkins
					def artifactoryServer = Artifactory.server('my-artifactory')
					def mavenBuild = Artifactory.newMavenBuild()
					mavenBuild.resolver server: artifactoryServer, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
					mavenBuild.deployer server: artifactoryServer, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
					mavenBuild.tool = 'Maven 3'
					def buildInfo = mavenBuild.run pom: 'pom.xml', goals: 'clean install'

					//Publish SNAPSHOT to artifactory
					artifactoryServer.publishBuildInfo buildInfo
				}
			}
		}
		stage('Deploy to test environment'){
			when {
				branch 'develop'
			}
			options {
				// Abort the deployment to test environment if it takes more than 15 minutes, prevent the build of getting stuck 
                timeout(time: 15, unit: 'MINUTES') 
            }
			environment {
				TEST_SERVER_CREDENTIAL = credentials('tomcat-credential') // Retrieve the credentials configured in Jenkins and add then to the env for the stage
				TEST_SERVER_IP = '192.168.1.41:8888'
			}
			steps {
				echo "Undeploying Webapp to Tomcat test environment ${TEST_SERVER_IP}"
				// Using an HTTP request to undeploy the web application on the server path
				sh "curl " + 
						"-u ${TEST_SERVER_CREDENTIAL_USR}:${TEST_SERVER_CREDENTIAL_PSW} " +
						"http://${TEST_SERVER_IP}/manager/text/undeploy?path=/test"

				echo "Deploying War file to Tomcat test environment ${TEST_SERVER_IP}"
				// Using an HTTP request to deploy the web application on the server path
				// Aditionnal parameter are for filtering the return code status and store it in a tmp file
				sh "curl -X PUT " +
						"-o /dev/null -w \"%{http_code}\" " +
						"-u ${TEST_SERVER_CREDENTIAL_USR}:${TEST_SERVER_CREDENTIAL_PSW} " +
						"--upload-file target/Test.war " +
						"http://${TEST_SERVER_IP}/manager/text/deploy?path=/test " +
						" > status.tmp"
					
				script {
					// Check if the deployment is successfull
					String deployStatus = readFile 'status.tmp'
					if( '200' !=  deployStatus) {
						error "Failed to deploy the War file to server test environment ${TEST_SERVER_IP}, response status ${deployStatus}"
					}
				}
			}
		}
		stage('Publish release'){
			when {
				branch 'master'
			}
			steps {
				script {
					// Check that the version in the pom file is not a SNAPSHOT
					if(env.VERSION.contains("SNAPSHOT")){
						error "Pipeline failure, master shouldn\'t contains SNAPSHOT version"
					}

					// Retrieve the latest tag from current commit and check if it's the same version than on the pom file
					String lastTag = sh ( script : 'git describe', returnStdout: true ).trim()
					if(env.VERSION != lastTag){
						error "Pipeline failure, releasing on version ${VERSION} different than Tag ${lastTag}"
					}

					// Fail the build if any dependencies is a SNAPSHOT and not a release
					if(Artifactory.mavenDescriptor().hasSnapshots()){
						error 'Pipeline failure, release with SNAPSHOT dependencies is not allow'
					}

					echo "Publishing release version : ${VERSION}, on Tag : ${lastTag}"

					def artifactoryServer = Artifactory.server('my-artifactory')
					def mavenBuild = Artifactory.newMavenBuild()
					mavenBuild.resolver server: artifactoryServer, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
					mavenBuild.deployer server: artifactoryServer, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
					mavenBuild.tool = 'Maven 3'
					def buildInfo = mavenBuild.run pom: 'pom.xml', goals: 'clean install'

					// Publish the release version, the Artifactory plugin use the build version to determine
					// if it's a SNAPSHOT or a release version depending on if the word is present in it
					artifactoryServer.publishBuildInfo buildInfo
				}
			}
		}
		stage('Deploy release to staging'){
			when {
				branch 'master'
			}
			options {
                timeout(time: 15, unit: 'MINUTES') 
            }
			environment {
				STAGING_SERVER_CREDENTIAL = credentials('tomcat-credential')
				STAGING_SERVER_IP = '192.168.1.41:8888'
			}
			steps {
				echo "Undeploying Webapp to Tomcat ${STAGING_SERVER_IP}"
				// Using an HTTP request to undeploy the web application on the server path
				sh "curl " + 
						"-u ${STAGING_SERVER_CREDENTIAL_USR}:${STAGING_SERVER_CREDENTIAL_PSW} " +
						"http://${STAGING_SERVER_IP}/manager/text/undeploy?path=/test"

				
				echo "Deploying War file to Tomcat ${STAGING_SERVER_IP}"
				// Using an HTTP request to deploy the web application on the server path
				// Aditionnal parameter are for filtering the return code status and store it in a tmp file
				sh "curl -X PUT " +
						"-o /dev/null -w \"%{http_code}\" " +
						"-u ${STAGING_SERVER_CREDENTIAL_USR}:${STAGING_SERVER_CREDENTIAL_PSW} " +
						"--upload-file target/Test.war " +
						"http://${STAGING_SERVER_IP}/manager/text/deploy?path=/test " +
						" > status.tmp"

				script {	
					// Check if the deployment is successfull
					String deployStatus = readFile 'status.tmp'
					if( '200' !=  deployStatus) {
						error "Failed to deploy the War file to staging server ${STAGING_SERVER_IP}, response status ${deployStatus}"
					}
				}
			}
		}
	}
	post {
		always {
			// Always do reporting of the result for statistics
			echo 'Doing report'
		}
		success {
			script {
				// If the build is a success and is on a team level branch then notify everyone about it
				if(['master', 'develop'].contains(GIT_BRANCH)){
					slackSend color: 'good', 
						message: "Build successfull ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
			}
		}
		failure {
			script {
				String targetEmail = ''
				// If the build is broke for any reason on a shared branch
				if(['master', 'develop'].contains(GIT_BRANCH)) {
					// Then notify everyone by email and slack
					targetEmail = 'team@mail.com'
					slackSend color: 'danger', 
						message: "Build failure ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				} else {
					// Otherwise retrieve the email address of the last commiter on branch and notify him by email
					targetEmail = sh ( script : 'git log -1 --format=%ae $( git rev-parse HEAD )', returnStdout: true ).trim()
				}

				echo "Sending email to ${targetEmail}"
			}
		}
		fixed {
			script {
				// Same logique when the build is fixed ( goes from unstabme/failed to success )
				String targetEmail = ''
				if(['master', 'develop'].contains(GIT_BRANCH)) {
					targetEmail = 'team@mail.com'
					slackSend color: 'good', 
						message: "Build back to normal ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				} else {
					targetEmail = sh ( script : 'git log -1 --format=%ae $( git rev-parse HEAD )', returnStdout: true ).trim()
				}
				echo "Sending email to ${targetEmail}"
			}
		}
		cleanup {
			// After any operation clean workspace
			deleteDir()
		}
	}
}