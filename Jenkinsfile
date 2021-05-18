#!/usr/bin/env groovy

pipeline {

  agent any

  stages {

    stage('Build') {
      steps {
        sh "sbt -Dsbt.log.noformat=true \"playScrapeServer/publishLocal\""
      }
    }

    stage('Test') {
      steps {
        sh "sbt -Dsbt.log.noformat=true \"playScrape/scripted play-scraper/absolute\" \"playScrape/scripted play-scraper/context\" \"playScrape/scripted play-scraper/delay\" \"playScrape/scripted play-scraper/simple\" \"playScrape/scripted play-scraper/versioned\""
      }
    }

    stage('Test Upload') {
      steps {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'ccl-aws-deploy', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
          sh "sbt -Dsbt.log.noformat=true \"playScrape/scripted play-scraper/upload\""
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
