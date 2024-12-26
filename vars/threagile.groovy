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
    // echo "Running Docker container..."
    // sh """
    //    docker run --rm -v \$(pwd):/app/work ${dockerImage} -verbose -model /app/work/${threagileYamlPath} -output /app/work/${outputDir} 
    // """


    // Create a temporary workspace folder in /tmp
    def tempWorkspace = "/tmp/threagile_workspace"
    echo "Creating temporary workspace at ${tempWorkspace}..."
    sh "mkdir -p ${tempWorkspace}"

    // Copy the threagile.yaml file to the temporary workspace
    def fileName = threagileYamlPath.tokenize('/').last()  // Get the file name from the path
    echo "Copying ${threagileYamlPath} to ${tempWorkspace}/${fileName}..."
    sh "cp ${threagileYamlPath} ${tempWorkspace}/${fileName}"

    // Run the Docker container with the updated paths, from the temporary workspace
    echo "Running Docker container..."
    sh """
       docker run --rm -v ${tempWorkspace}:/app/work -v \$(pwd):/app ${dockerImage} \
       -verbose -model /app/work/${fileName} -output /app/work/${outputDir}
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
