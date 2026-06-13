// =============================================================================
// Jenkins declarative pipeline for the unified UI + API automation framework.
//
// Flow: checkout -> build -> start Selenium Grid -> run tests (with credentials
// injected from the Jenkins store) -> publish reports -> tear down the Grid.
//
// Spotify secrets are NOT in the repo or the command history — they live in
// Jenkins' credential store and are bound at runtime, masked in the console log.
//
// Agent requirements: mvn, docker, docker compose, and curl on the PATH, and a
// JDK matching the project's compiler release.
//
// Required plugins: Pipeline, Credentials Binding, HTML Publisher, JUnit.
//
// Jenkins credentials to create (Manage Jenkins -> Credentials), each a
// "Secret text" with these exact IDs:
//   spotify-client-id, spotify-client-secret, spotify-refresh-token, spotify-user-id
// =============================================================================

pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['dev', 'staging', 'prod'],
               description: 'Target environment — selects config/<env>.properties')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'],
               description: 'Browser for UI tests on the Grid')
        string(name: 'TAGS', defaultValue: '@api or @ui',
               description: 'Cucumber tag filter (e.g. "@ui", "@smoke", "@api or @ui")')
    }

    options {
        timestamps()
        disableConcurrentBuilds()              // two runs would fight over Grid port 4444
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        GRID_COMPOSE = 'docker-compose-grid.yml'
        GRID_URL     = 'http://localhost:4444/wd/hub'
        GRID_STATUS  = 'http://localhost:4444/status'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Build all modules; skip tests here — the suites run in the Test stage,
                // after the Grid is up and credentials are bound.
                sh 'mvn -B clean install -DskipTests'
            }
        }

        stage('Start Selenium Grid') {
            steps {
                sh 'docker compose -f ${GRID_COMPOSE} up -d'
                // depends_on only starts containers; poll until the nodes register.
                sh '''
                    echo "Waiting for Selenium Grid at ${GRID_STATUS} ..."
                    for i in $(seq 1 30); do
                        if curl -sSf "${GRID_STATUS}" 2>/dev/null | grep -qE '"ready" *: *true'; then
                            echo "Grid is ready."
                            exit 0
                        fi
                        echo "  not ready yet ($i/30)..."
                        sleep 2
                    done
                    echo "Grid did not become ready in time." >&2
                    exit 1
                '''
            }
        }

        stage('Test') {
            steps {
                // Bind secrets from the Jenkins store as env vars for this block only.
                // Single-quoted sh + shell expansion (not Groovy interpolation) keeps the
                // secrets out of the Groovy string — the secure, warning-free pattern.
                withCredentials([
                    string(credentialsId: 'spotify-client-id',     variable: 'SPOTIFY_CLIENT_ID'),
                    string(credentialsId: 'spotify-client-secret', variable: 'SPOTIFY_CLIENT_SECRET'),
                    string(credentialsId: 'spotify-refresh-token', variable: 'SPOTIFY_REFRESH_TOKEN'),
                    string(credentialsId: 'spotify-user-id',       variable: 'SPOTIFY_USER_ID')
                ]) {
                    sh '''
                        mvn -B test -pl framework-tests -am \
                            -Denv=${ENV} \
                            -Dui.grid.enabled=true \
                            -Dui.grid.url=${GRID_URL} \
                            -Dui.browser=${BROWSER} \
                            -Dui.headless=true \
                            -Dapi.auth.client.id=${SPOTIFY_CLIENT_ID} \
                            -Dapi.auth.client.secret=${SPOTIFY_CLIENT_SECRET} \
                            -Dapi.auth.refresh.token=${SPOTIFY_REFRESH_TOKEN} \
                            -Dapi.user.id=${SPOTIFY_USER_ID} \
                            -Dcucumber.filter.tags="${TAGS}"
                    '''
                }
            }
        }
    }

    post {
        always {
            // TestNG/Surefire results -> Jenkins test trend.
            junit testResults: 'framework-tests/target/surefire-reports/*.xml',
                  allowEmptyResults: true

            // Extent HTML report -> linked on the build page.
            publishHTML(target: [
                reportDir            : 'framework-tests/target/reports',
                reportFiles          : 'extent-report.html',
                reportName           : 'Extent Report',
                keepAll              : true,
                alwaysLinkToLastBuild: true,
                allowMissing         : true
            ])

            archiveArtifacts artifacts: 'framework-tests/target/reports/**',
                             allowEmptyArchive: true

            // Always tear the Grid down, even if the tests failed.
            sh 'docker compose -f ${GRID_COMPOSE} down || true'
        }
        failure {
            echo 'Build failed — see the Extent Report and the console log for details.'
        }
        cleanup {
            cleanWs()
        }
    }
}
