rootProject.name = "rewrite-concourse"

plugins {
    id("com.gradle.enterprise") version "3.8.1"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.6.2"
}

gradleEnterprise {
    val isCiServer = System.getenv("CI")?.equals("true") ?: false
    server = "https://ge.openrewrite.org/"
    val gradleCacheRemoteUsername: String? = System.getenv("GRADLE_ENTERPRISE_CACHE_USERNAME")
    val gradleCacheRemotePassword: String? = System.getenv("GRADLE_ENTERPRISE_CACHE_PASSWORD")

    buildCache {
        remote(HttpBuildCache::class) {
            url = uri("https://ge.openrewrite.org/cache/")
            isPush = isCiServer
            if (!gradleCacheRemoteUsername.isNullOrBlank() && !gradleCacheRemotePassword.isNullOrBlank()) {
                credentials {
                    username = gradleCacheRemoteUsername
                    password = gradleCacheRemotePassword
                }
            }
        }
    }

    buildScan {
        capture {
            isTaskInputFiles = true
        }

        isUploadInBackground = !isCiServer

        publishAlways()
        this as com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures
        publishIfAuthenticated()
    }

}
