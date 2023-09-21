package io.glassfy.androidsdk

object Configuration {
    const val compileSdk = 33
    const val targetSdk = 33
    const val minSdk = 21
    private const val majorVersion = 1
    private const val minorVersion = 5
    private const val patchVersion = 2
    const val versionName = "$majorVersion.$minorVersion.$patchVersion"
    const val snapshotVersionName = "${versionName}-SNAPSHOT"
    const val artifactGroup = "io.glassfy"
    const val artifactId = "androidsdk"
}