pluginManagement {
    val androidPluginVersion: String by settings
    val jetbrainsKotlinVersion: String by settings
    val nexusPublishPlugin: String by settings
    val kspVersion: String by settings
    plugins {
        id("com.android.application") version androidPluginVersion
        id("com.android.library") version androidPluginVersion
        kotlin("android") version jetbrainsKotlinVersion
        kotlin("jvm") version jetbrainsKotlinVersion
        id("com.google.devtools.ksp") version kspVersion
        id("org.jetbrains.dokka") version jetbrainsKotlinVersion
        id("io.github.gradle-nexus.publish-plugin") version nexusPublishPlugin
    }

    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
//        staging repo
        maven {
            url = uri("https://s01.oss.sonatype.org/content/groups/staging/")
        }
//        snapshot repo
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

rootProject.name = "Glassfy"
include(":newsreader-kotlin")
include(":newsreader-java")
include(":glassfy")
include(":paywall")
include(":showcase")
