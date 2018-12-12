#!groovy

node('maven') {

    dir ("build")   {


        stage('Checkout Source') {
            git url: "${git_url}", branch: 'master'
        }

        dir("src/${app_name}") {

            def mvn = "mvn -U -B -q -s ../settings.xml -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"
            def dev_project = "${org}-dev"
            def prod_project = "${org}-prod"
            def app_url_dev = "http://${app_name}.${dev_project}.svc:8080"
            def groupId = getGroupIdFromPom("pom.xml")
            def artifactId = getArtifactIdFromPom("pom.xml")
            def version = getVersionFromPom("pom.xml")
            def packaging = getPackagingFromPom("pom.xml")
            def sonar_url = "http://sonarqube.cicd.svc:9000"
            def nexus_url = "http://nexus.cicd.svc:8081/repository/maven-snapshots"
            def shortCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()

            stage('Build jar') {
                echo "Building version : ${version}"
                sh "${mvn} clean package -Dspring.profiles.active=dev -DskipTests"
            }

            // Using Maven run the unit tests
            stage('Unit Tests') {
                echo "Running Unit Tests"
                sh "${mvn} test -Dspring.profiles.active=dev"
            }

            // Using Maven call SonarQube for Code Analysis
            stage('Code Analysis') {
                echo "Running Code Analysis"
                sh "${mvn} sonar:sonar -Dspring.profiles.active=dev -Dsonar.host.url=${sonar_url}"
            }

            // Publish the built war file to Nexus
            stage('Publish to Nexus') {
                echo "Publish to Nexus"
                sh "${mvn} deploy -DskipTests"
            }

            //Build the OpenShift Image in OpenShift and tag it.
            stage('Build and Tag OpenShift Image') {
                echo "Building OpenShift container image ${app_name}:${devTag}for commit ${shortCommit}"
                echo "Project : ${dev_project}"
                echo "App : ${app_name}"
                echo "Group ID : ${groupId}"
                echo "Artifact ID : ${artifactId}"
                echo "Version : ${version}"
                echo "Packaging : ${packaging}"
                echo "Git Commit Id : ${shortCommit}"

                sh "${mvn} clean"
                sh "${mvn} dependency:copy -DstripVersion=true -Dartifact=${groupId}:${artifactId}:${version}:${packaging} -DoutputDirectory=."
                sh "cp \$(find . -type f -name \"${artifactId}-*.${packaging}\")  ${artifactId}-${shortCommit}.${packaging}"
                sh "pwd; ls -ltr"
                //sh "ls -ltr target"
                //sh "oc start-build ${app_name} --follow --from-file=target/${artifactId}-${version}.${packaging} -n ${dev_project}"
                sh "oc start-build ${app_name} --follow --from-file=${artifactId}-${shortCommit}.${packaging} -n ${dev_project}"
                openshiftVerifyBuild apiURL: '', authToken: '', bldCfg: app_name, checkForTriggeredDeployments: 'false', namespace: dev_project, verbose: 'false', waitTime: ''
                openshiftTag alias: 'false', apiURL: '', authToken: '', destStream: app_name, destTag: devTag, destinationAuthToken: '', destinationNamespace: dev_project, namespace: dev_project, srcStream: app_name, srcTag: 'latest', verbose: 'false'
                openshiftTag alias: 'false', apiURL: '', authToken: '', destStream: app_name, destTag: shortCommit, destinationAuthToken: '', destinationNamespace: dev_project, namespace: dev_project, srcStream: app_name, srcTag: 'latest', verbose: 'false'

            }

            // Deploy the built image to the Development Environment.
            stage('Deploy to Dev') {
                echo "Deploying container image to Development Project"
                echo "Project : ${dev_project}"
                echo "App : ${app_name}"
                echo "Dev Tag : ${devTag}"
                sh "oc set image dc/${app_name} ${app_name}=docker-registry.default.svc:5000/${dev_project}/${app_name}:${shortCommit} -n ${dev_project}"
                def ret = sh(script: "oc delete configmap ${app_name}-config --ignore-not-found=true -n ${dev_project}", returnStdout: true)
                ret = sh(script: "oc create configmap ${app_name}-config --from-file=${config_file} -n ${dev_project}", returnStdout: true)

                openshiftDeploy apiURL: '', authToken: '', depCfg: app_name, namespace: dev_project, verbose: 'false', waitTime: '180', waitUnit: 'sec'
                openshiftVerifyDeployment apiURL: '', authToken: '', depCfg: app_name, namespace: dev_project, replicaCount: '1', verbose: 'false', verifyReplicaCount: 'true', waitTime: '180', waitUnit: 'sec'

                openshift.withCluster() {
                    openshift.withProject(dev_project) {

                        //openshift.selector('pods', [app: app_name]).describe()

                        def pod = openshift.selector('pods', [app: app_name]).object()
                        pod.metadata.labels['commit'] = shortCommit // Adjust the model
                        openshift.apply(pod) // Patch the object on the server

                        def dc = openshift.selector('dc', [app: app_name]).object()
                        dc.metadata.labels['commit'] = shortCommit // Adjust the model
                        openshift.apply(dc) // Patch the object on the server
                    }
                }
            }
        }
    }

    dir("build-metadata") {

        stage('manage version data') {
            manageVersionData(shortCommit, build_tracking_git_url)
        }

    }

}

// Convenience Functions to read variables from the pom.xml
// Do not change anything below this line.
// --------------------------------------------------------
def getVersionFromPom(pom) {
    def matcher = readFile(pom) =~ '<version>(.+)</version>'
    matcher ? matcher[0][1] : null
}
def getGroupIdFromPom(pom) {
    def matcher = readFile(pom) =~ '<groupId>(.+)</groupId>'
    matcher ? matcher[0][1] : null
}
def getArtifactIdFromPom(pom) {
    def matcher = readFile(pom) =~ '<artifactId>(.+)</artifactId>'
    matcher ? matcher[0][1] : null
}
def getPackagingFromPom(pom) {
    def matcher = readFile(pom) =~ '<packaging>(.+)</packaging>'
    matcher ? matcher[0][1] : null
}

def manageVersionData(commitId, build_tracking_git_url) {
    git url: "${build_tracking_git_url}", branch: 'master', credentialsId: '1c0e3c0a-f7bd-444e-918f-69799380d061'
    def workspace = pwd()
    def versionFileName = "version"
    versionFileName = workspace+"/"+versionFileName
    def versiondata = sh(returnStdout: true, script: "cat ${versionFileName} | head -1")
    println "Existing version data : "+versiondata
    def versionnumber = versiondata.tokenize(':')[0]
    def gitcommitid = versiondata.tokenize(':')[1]
    int newVersion = versionnumber.toInteger()
    newVersion = newVersion + 1
    def newVersionString = newVersion+":"+commitId
    println "New version data :  : "+newVersionString
    sh(returnStdout: true, script: "echo ${newVersionString} > ${versionFileName}")
    def newversiondata = sh(returnStdout: true, script: "cat ${versionFileName} | head -1")
    sshagent(['1c0e3c0a-f7bd-444e-918f-69799380d061']) {
        sh ("git config --global user.email \"justinndavis@gmail.com\"")
        sh ("git config --global user.name \"Justin Davis\"")
        sh ("git add version")
        sh ("git commit -m \"updating version data to ${newVersionString}\"")
        sh ("git push origin master")
    }
}