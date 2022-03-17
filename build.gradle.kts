// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    kotlin("android") apply false
    kotlin("jvm") apply false
    kotlin("kapt") apply false
    id("io.github.gradle-nexus.publish-plugin") apply true
}

apply("${rootDir}/scripts/publish-root.gradle")
