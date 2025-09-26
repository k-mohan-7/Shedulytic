// Custom task to force clean locked files
tasks.register("forceClean") {
    group = "build"
    description = "Force cleans the build directory, removing locked files"
    
    doLast {
        // Get the build directory
        val buildDir = project.buildDir
        
        // Log the cleaning process
        println("Force cleaning build directory: ${buildDir}")
        
        // Try multiple deletion attempts with System.gc() calls between attempts
        for (attempt in 1..3) {
            try {
                // Delete the build directory
                if (buildDir.exists()) {
                    println("Deletion attempt ${attempt}...")
                    project.delete(buildDir)
                    
                    // If we reach here without exception, break the loop
                    if (!buildDir.exists()) {
                        println("Successfully deleted build directory on attempt ${attempt}")
                        break
                    }
                }
            } catch (e: Exception) {
                println("Deletion attempt ${attempt} failed: ${e.message}")
                // Force garbage collection to release file handles
                System.gc()
                // Wait a bit before next attempt
                Thread.sleep(1000)
            }
        }
        
        // Create a fresh build directory
        buildDir.mkdirs()
        println("Created fresh build directory")
    }
}

// Make the standard clean task depend on our forceClean task
tasks.named("clean") {
    dependsOn("forceClean")
}