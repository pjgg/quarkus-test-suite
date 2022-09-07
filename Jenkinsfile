node('janitor') {
 try{
     stage 'Checkout'
     checkout scm

     stage 'target job execution'
      def scenario = sh (script:'''#!/bin/bash +x
            git remote add origin-qe git@github.com:quarkus-qe/quarkus-test-suite.git
            POM_CHANGED=""
            IGNORE_CHANGES=""
            MODULES_CHANGED_AMOUNT=$((MODULES_CHANGED_AMOUNT+0))

            while read srcmode dstmode srcsha dstsha status srcfile dstfile
            do
              if [[ "$srcfile" =~ "pom.xml" ]]; then
                      POM_CHANGED="${POM_CHANGED}true"
                      break
                  elif [[ "$srcfile" =~ "Jenkinsfile" ||  "$srcfile" =~ "*.md" ||  "$srcfile" =~ "*.yaml" ||  "$srcfile" =~ "*.sh" ||  "$srcfile" =~ "*.xml" ]]; then
                      IGNORE_CHANGES="${IGNORE_CHANGES}true"
                  else
                  if [ ! -z "$srcfile" ]; then
                     let "MODULES_CHANGED_AMOUNT++"
                  fi
              fi
            done <<< $(git diff-tree HEAD)

            if [[ $POM_CHANGED =~ "true" || $MODULES_CHANGED_AMOUNT > 3 ]] ; then
               echo "matrixJob"
            elif [[ $MODULES_CHANGED_AMOUNT == 0 && $IGNORE_CHANGES =~ "true" ]]; then
               echo "ignore"
            else
               echo "prJob"
            fi
          ''', returnStdout: true).trim()

        def scmUrl = scm.getUserRemoteConfigs()[0].getUrl()
        switch(scenario) {
          case "matrixJob":
            println "Running on quarkus-main-rhel8-jdk11-openshift-weekly-ts-jvm-ocp-4-stable job"
            build job: 'quarkus-main-rhel8-jdk11-openshift-weekly-ts-jvm-ocp-4-stable', parameters: [
                [$class: 'StringParameterValue', name: 'GIT_URL', value: scmUrl],
                [$class: 'StringParameterValue', name: 'GIT_BRANCH', value: ${env.BRANCH_NAME}]
            ]
            break
          case "ignore":
            println "Nothing to execute."
            break
          default:
            println "Running on quarkus-main-rhel8-jdk11-openshift-ts-jvm-githubci job"
            build job: 'quarkus-main-rhel8-jdk11-openshift-ts-jvm-githubci'
            break
        }
   } catch (e) {
 	  currentBuild.result = "FAILED"
 	  throw e
 }
}