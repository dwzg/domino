pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Domino"
// Server-only builds (e.g. the Docker image) skip the Android app, so no Android SDK is needed.
if (System.getenv("DOMINO_SERVER_ONLY") == null) include(":app")
include(":engine")
include(":protocol")
include(":server")
