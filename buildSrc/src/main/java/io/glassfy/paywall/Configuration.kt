package io.glassfy.paywall

object Configuration {
    const val compileSdk = 33
    const val targetSdk = 33
    const val minSdk = 24
    private const val majorVersion = 1
    private const val minorVersion = 4
    private const val patchVersion = 0
    const val versionName = "$majorVersion.$minorVersion.$patchVersion"
    const val snapshotVersionName = "${versionName}-SNAPSHOT"
    const val artifactGroup = "io.glassfy"
    const val artifactId = "paywall"
}