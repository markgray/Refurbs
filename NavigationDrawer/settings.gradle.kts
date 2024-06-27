pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage") // TODO: Keep an eye on this
    repositories {
        google()
        mavenCentral()
    }
}
include("Application")
