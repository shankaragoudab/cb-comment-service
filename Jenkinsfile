@Library('deploy-conf') _
node() {
    try {
        String ANSI_GREEN = "\u001B[32m"
        String ANSI_NORMAL = "\u001B[0m"
        String ANSI_BOLD = "\u001B[1m"
        String ANSI_RED = "\u001B[31m"
        String ANSI_YELLOW = "\u001B[33m"

        ansiColor('xterm') {
            stage('Checkout') {
                if (!env.hub_org) {
                    println(ANSI_BOLD + ANSI_RED + "Uh Oh! Please set a Jenkins environment variable named hub_org with value as registry/sunbirded" + ANSI_NORMAL)
                    error 'Please resolve the errors and rerun..'
                } else {
                    println(ANSI_BOLD + ANSI_GREEN + "Found environment variable named hub_org with value as: " + hub_org + ANSI_NORMAL)
                }
            }

            cleanWs()
            checkout scm
            commit_hash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
            build_tag = sh(script: "echo " + params.github_release_tag.split('/')[-1] + "_" + commit_hash + "_" + env.BUILD_NUMBER, returnStdout: true).trim()
            echo "build_tag: " + build_tag

            if (params.enable_code_analysis) {
                stage('Code analysis') {
                    build job: "Build/CodeReview/${JOB_BASE_NAME}", wait: true
                }
            }

if (params.enable_owasp_scan) {
    stage('Dependency Check (Pre-Build)') {
        script {
            // Dynamically use Jenkins job name for project and report naming
            def projectName = "${env.JOB_BASE_NAME}"
            def reportDir = "/var/lib/jenkins/owasp-report"
            def reportFile = "${reportDir}/${projectName}-owasp-report.html"

            echo "🔍 Starting OWASP Dependency-Check for project: ${projectName}"
            echo "📁 Report will be saved to: ${reportFile}"

            // Use Jenkins global Java 17 environment variable
            withEnv(["JAVA_HOME=${JAVA17_HOME}", "PATH=${JAVA17_HOME}/bin:${env.PATH}"]) {
                sh """
                    set -e

                    echo "✅ Java version in use:"
                    java -version

                    echo "🧭 Preparing output directory..."
                    mkdir -p ${reportDir}

                    echo "🚀 Running OWASP Dependency-Check..."
                    time /var/lib/jenkins/dependency-check/bin/dependency-check.sh \
                        --project "${projectName}" \
                        --scan . \
                        --out ${reportDir} \
                        --format "HTML" \
                        --disableNodeAudit --disableRetireJS --disableAssembly

                    echo "📄 Renaming report to ${projectName}-owasp-report.html..."
                    mv ${reportDir}/dependency-check-report.html ${reportFile} || true

                    echo "================== DEPENDENCY-CHECK SUMMARY =================="
                    grep -E "Vulnerabilities Found|Critical|High|Medium|Low" ${reportFile} || true
                    echo "================================================================"
                """
            }

            // 🗂️ Archive only this job's specific HTML report
            archiveArtifacts artifacts: "${reportFile}", fingerprint: true
        }
    }
}
            
            stage('docker-pre-build') {
                sh '''
                    docker build -f ./Dockerfile.build -t $docker_pre_build .
                    docker run --name $docker_pre_build $docker_pre_build:latest && docker cp $docker_pre_build:/opt/target/cb-comment-service-0.0.1-SNAPSHOT.jar .
                    sleep 2
                    docker rm -f $docker_pre_build
                    docker rmi -f $docker_pre_build
                '''
            }

            stage('Build') {
                env.NODE_ENV = "build"
                print "Environment will be : ${env.NODE_ENV}"
                sh('chmod 777 build.sh')
                sh("bash -x build.sh ${build_tag} ${env.NODE_NAME} ${docker_server}")
            }

            // 🧩 New Stage: Docker Vulnerability Scan
            if (params.enable_docker_scan) {
                stage('Docker Scan') {
                    script {
                        def imageFullName = "${hub_org}/${JOB_BASE_NAME}:${build_tag}"
                        echo "🔍 Starting Trivy scan for image: ${imageFullName}"

                        sh """
                            mkdir -p trivy-reports

                            # Full Trivy scan in JSON format
                            trivy image --quiet --format json --output trivy-reports/trivy-report.json ${imageFullName}

                            # Extract by severity
                            jq '.Results[].Vulnerabilities[] | select(.Severity=="CRITICAL")' trivy-reports/trivy-report.json > trivy-reports/critical.json || true
                            jq '.Results[].Vulnerabilities[] | select(.Severity=="HIGH")' trivy-reports/trivy-report.json > trivy-reports/high.json || true
                            jq '.Results[].Vulnerabilities[] | select(.Severity=="MEDIUM")' trivy-reports/trivy-report.json > trivy-reports/medium.json || true

                            echo "================== TRIVY VULNERABILITY SUMMARY =================="
                            jq -r '.Results[].Vulnerabilities[].Severity' trivy-reports/trivy-report.json | sort | uniq -c | awk '{print \$2": "\$1}'
                            echo "================================================================="
                        """

                        // Archive the scan reports
                        archiveArtifacts artifacts: 'trivy-reports/*.json', fingerprint: true
                    }
                }
            }

            stage('ArchiveArtifacts') {
                archiveArtifacts "metadata.json"
                currentBuild.description = "${build_tag}"
            }
        }
    }
    catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }
    finally {
        // email_notify()
    }
}
