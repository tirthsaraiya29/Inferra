pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        constraints {
            classpath("org.jdom:jdom2:2.0.6.1")
            classpath("org.apache.httpcomponents:httpclient:4.5.13")
            classpath("org.bouncycastle:bcprov-jdk18on:1.85")
            classpath("org.bouncycastle:bcpkix-jdk18on:1.85")
            classpath("org.apache.commons:commons-lang3:3.18.0")
            classpath("org.bitbucket.b_c:jose4j:0.9.6")
            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
        }
    }
    configurations.all {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "org.jdom" -> if (requested.name == "jdom2") useVersion("2.0.6.1")
                "org.apache.httpcomponents" -> if (requested.name == "httpclient") useVersion("4.5.13")
                "org.bouncycastle" -> {
                    if (requested.name.startsWith("bcprov-") || requested.name.startsWith("bcpkix-")) {
                        useVersion("1.85")
                    }
                }
                "org.apache.commons" -> if (requested.name == "commons-lang3") useVersion("3.18.0")
                "org.bitbucket.b_c" -> if (requested.name == "jose4j") useVersion("0.9.6")
                "org.jetbrains.kotlin" -> if (requested.name == "kotlin-gradle-plugin") useVersion("2.4.20")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Inferra"
include(":app")
