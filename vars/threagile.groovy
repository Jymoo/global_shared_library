// Define the function with a `call` method
def call(Map params = [:]) {
    // Default parameters
    def dockerImage = params.dockerImage ?: 'threagile/threagile:latest'
    def threagileYamlPath = params.threagileYamlPath ?: 'threagile.yaml'
    def outputDir = params.outputDir ?: 'results'
    def reportFile = params.reportFile ?: 'report.pdf'
    
    
    // Check if the Docker image exists locally
    def imageExists = false
    try {
        // Try to get the image info to check if it exists
        sh(script: "docker image inspect ${dockerImage}", returnStatus: true)
        imageExists = true
    } catch (Exception e) {
        // If the image doesn't exist, it will throw an error, and we'll pull the image
        echo "Docker image not found locally, pulling..."
    }
    
    // Pull the image if it doesn't exist locally
    if (!imageExists) {
        sh "docker pull ${dockerImage}"
    }

    // Run the Docker container with the provided command  docker run --rm -v \$(pwd):/app/work ${dockerImage} -verbose -model /app/work/${threagileYamlPath} -output /app/work/${outputDir}  docker run --rm -v \$(pwd):/app/work ${dockerImage} -verbose -model /app/work/${threagileYamlPath} -output /app/work/${outputDir}        docker run --rm -v /tmp:/app/work ${dockerImage} -verbose -model /app/work/${threagileYamlPath} -output /app/work/${outputDir}   docker run --rm -v /tmp:/app/work ${dockerImage} -verbose -model /app/work/${threagileYamlPath} -output /app/work/${outputDir}
    echo "Running Docker container..."
    sh """
       mkdir -p /tmp/threagile-work

        echo "Copying threagile.yaml to /tmp/threagile-work..."
        cp /var/jenkins_home/workspace/Shared-Library/threagile.yaml /tmp/threagile-work/

        # Check if the file was copied successfully
        if [ ! -f "/tmp/threagile-work/threagile.yaml" ]; then
            error "Failed to copy threagile.yaml to /tmp/threagile-work"
        else
            echo "threagile.yaml copied successfully to /tmp/threagile-work"
        fi

        # Run Docker container
        echo "Running Docker container..."
        docker run --rm -v /tmp/threagile-work:/app/work ${dockerImage} -verbose -model /app/work/threagile.yaml -output /app/work/results

        # Check if output directory exists
        if [ ! -d "/tmp/threagile-work/results" ]; then
            error "Output directory /app/work/results not found in the container."
        else
            echo "Output directory /app/work/results found in the container."
        fi

        # Check if report file exists
        reportFile="report.pdf" 
        if [ ! -f "/tmp/threagile-work/results/${reportFile}" ]; then
            error "Report file ${reportFile} not found in the output directory."
        else
            echo "Report file ${reportFile} found in the output directory."
        fi

    """
    
    // After running the container, check if the report.pdf file exists in the output directory
    def reportPath = "${PWD}/${outputDir}/${reportFile}"
    if (fileExists(reportPath)) {
        // Create a clickable link to the report in the Jenkins log
        echo "The report is ready. Click the link below to view the report:"
        echo "<a href='file://${reportPath}'>Click here to view the ${reportFile}</a>"
    } else {
        echo "No ${reportFile} found at ${reportPath}"
    }
}
