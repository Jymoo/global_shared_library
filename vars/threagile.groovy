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


    
    
    // Define the new workspace path within the user's home directory
    def newWorkspacePath = "~/threagile_workspaces"
    
    // Create the new workspace directory if it doesn't exist
    new File(newWorkspacePath).mkdirs()
    
    // Move the existing workspace to the new location
    sh "mv ${tempWorkspace} ${newWorkspacePath}/threagile_workspace"
    
    // Update the tempWorkspace variable to reflect the new location
    tempWorkspace = "${newWorkspacePath}/threagile_workspace"
    
    // Create the temporary workspace directory if it doesn't exist
    new File(tempWorkspace).mkdirs()
    
    // Copy the threagile.yaml file to the temporary workspace
    def fileName = new File(threagileYamlPath).name
    sh "cp ${threagileYamlPath} ${tempWorkspace}/${fileName}"
    
    // Change the current working directory to the temporary workspace
    dir(tempWorkspace) {
        // Run the Docker container with updated paths and volumes
        sh """
        docker run --rm \
            -v ${tempWorkspace}:/app/work \
            -v /home/jenkins/workspace/devsecops:/app \
            --userns=host \
            ${dockerImage} \
            -verbose -model /app/work/${fileName} -output /app/work/${outputDir}
        """
    }
        




    
    
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
