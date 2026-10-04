// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "org.jdom" -> if (requested.name == "jdom2") useVersion("2.0.6.1")
                "org.apache.httpcomponents" -> if (requested.name == "httpclient") useVersion("4.5.13")
                "org.bouncycastle" -> {
                    if (requested.name.startsWith("bcprov-") || requested.name.startsWith("bcpkix-")) {
                        useVersion("1.86")
                    }
                }
                "org.apache.commons" -> if (requested.name == "commons-lang3") useVersion("3.21.0")
                "org.bitbucket.b_c" -> if (requested.name == "jose4j") useVersion("0.9.7")
            }
        }
    }
}
