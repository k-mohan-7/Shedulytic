// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Plugin management is now handled in settings.gradle.kts

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}