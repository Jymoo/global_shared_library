#!/usr/bin/env groovy

/**
 * Jenkins Shared Library for ZAP Full Scan.
 * This script dynamically retrieves the target URL from an environment variable, ensures Docker and ZAP are set up, and performs a full scan.
 *
 * Usage:
 *   ZAPScanner()
 */

def call(String zapDockerImage = 'owasp/zap2docker-stable', String reportName = 'zap_full_report.html', String failOnRiskLevel = 'High') {
    // Configure environment variables using EnvironmentConfig
    EnvironmentConfig()

    // Ensure Docker is installed and running
    ensureDockerInstalledAndRunning()

    // Ensure ZAP Docker image is available
    ensureZapDockerImage(zapDockerImage)

    // Retrieve the target URL from the environment
    def targetUrl = env.TARGET_URL
    if (!targetUrl) {
        error "Environment variable 'TARGET_URL' is not set or empty. Please define it in Jenkins."
    }

    echo "Starting ZAP Full Scan for target URL: ${targetUrl}"

    def workspaceDir = "${env.WORKSPACE}/zap"
    def riskLevels = ['Low': 1, 'Medium': 2, 'High': 3]
    def failThreshold = riskLevels[failOnRiskLevel]

    // Step 1: Run the ZAP Full Scan
    runZapScan(targetUrl, workspaceDir, zapDockerImage, reportName)

    // Step 2: Analyze the ZAP Report
    def reportPath = "${workspaceDir}/${reportName}"
    def reportContent = readFile(file: reportPath)
    analyzeZapReport(reportContent, failThreshold)

    echo "ZAP Full Scan completed successfully. Report available at: ${reportPath}"
}

/**
 * Ensures Docker is installed and running.
 */
def ensureDockerInstalledAndRunning() {
    echo "Checking if Docker is installed and running..."

    try {
        // Check if Docker is installed
        sh "docker --version"
        echo "Docker is installed."

        // Check if Docker is running
        sh "docker info > /dev/null 2>&1"
        echo "Docker is running."
    } catch (Exception e) {
        echo "Docker is not installed or running. Installing Docker..."
        installDocker()
    }
}

/**
 * Installs Docker on the agent.
 */
def installDocker() {
    def os = sh(script: "uname -s", returnStdout: true).trim()

    if (os == "Linux") {
        sh """
            sudo apt-get update
            sudo apt-get install -y docker.io
            sudo systemctl start docker
            sudo systemctl enable docker
        """
        echo "Docker has been installed and started."
    } else {
        error "Docker installation is not supported for this OS in the script."
    }
}

/**
 * Ensures the ZAP Docker image is pulled.
 *
 * @param zapDockerImage The Docker image name for ZAP.
 */
def ensureZapDockerImage(String zapDockerImage) {
    echo "Checking if ZAP Docker image '${zapDockerImage}' is available..."

    try {
        sh "docker image inspect ${zapDockerImage} > /dev/null 2>&1"
        echo "ZAP Docker image '${zapDockerImage}' is already available."
    } catch (Exception e) {
        echo "ZAP Docker image '${zapDockerImage}' not found. Pulling image..."
        sh "docker pull ${zapDockerImage}"
        echo "ZAP Docker image '${zapDockerImage}' has been pulled successfully."
    }
}

/**
 * Runs the ZAP Full Scan using Docker.
 *
 * @param targetUrl      Target application URL.
 * @param workspaceDir   Directory to store the report.
 * @param zapDockerImage Docker image for ZAP.
 * @param reportName     Name of the generated report.
 */
def runZapScan(String targetUrl, String workspaceDir, String zapDockerImage, String reportName) {
    def zapCommand = """
        mkdir -p ${workspaceDir} && \
        docker run --rm -v ${workspaceDir}:/zap/wrk -t ${zapDockerImage} zap-full-scan.py \
            -t ${targetUrl} -r ${reportName} -z "-config api.addrs.addr.name=127.0.0.1" -d
    """
    executeShellCommand(zapCommand)
}

/**
 * Analyzes the ZAP Full Scan report to determine if the pipeline should fail.
 *
 * @param reportContent   Content of the ZAP report.
 * @param failThreshold   Risk level threshold for pipeline failure.
 */
def analyzeZapReport(String reportContent, int failThreshold) {
    def risks = ['High', 'Medium', 'Low']
    def highestRiskFound = 0

    risks.eachWithIndex { risk, index ->
        if (reportContent.contains("${risk} Risk")) {
            echo "${risk} Risk vulnerabilities found!"
            highestRiskFound = Math.max(highestRiskFound, index + 1)
        }
    }

    if (highestRiskFound >= failThreshold) {
        def riskName = risks[highestRiskFound - 1]
        error "Pipeline failed due to ${riskName} Risk vulnerabilities."
    } else {
        echo "No significant vulnerabilities detected. Pipeline passed."
    }
}

/**
 * Executes a shell command and handles errors.
 *
 * @param command The shell command to execute.
 */
def executeShellCommand(String command) {
    try {
        echo "Executing command: ${command}"
        sh command
    } catch (Exception e) {
        error "Error executing command: ${e.message}"
    }
}
