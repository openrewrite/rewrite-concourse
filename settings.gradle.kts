rootProject.name = "rewrite-concourse"

gradleEnterprise {
    server = "https://ge.openrewrite.org/"

    buildCache {
        local {
            isEnabled = true
        }

        remote(HttpBuildCache::class) {
            isPush = true
            url = uri("https://ge.openrewrite.org/cache/")
        }

    }

    buildScan {
        publishAlways()
    }
}
