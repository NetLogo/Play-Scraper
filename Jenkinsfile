#!/usr/bin/env groovy

pipeline {

  agent any

  environment {
    CREDENTIALS_FROM_ENVIRONMENT = 'true'
  }

  stages {
    stage('Start') {
      steps {
        library 'netlogo-shared'
        sendNotifications('NetLogo/Play-Scraper', 'STARTED')
      }
    }


    stage('Build') {
      steps {
        library 'netlogo-shared'
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'play-scrape-test-deploy', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
          sbt 'playScrape/scripted'
        }
      }
    }
  }

  post {
    failure {
      library 'netlogo-shared'
        sendNotifications('NetLogo/Play-Scraper', 'FAILURE')
    }
    success {
      library 'netlogo-shared'
        sendNotifications('NetLogo/Play-Scraper', 'SUCCESS')
    }
    unstable {
      library 'netlogo-shared'
        sendNotifications('NetLogo/Play-Scraper', 'UNSTABLE')
    }
  }
}

