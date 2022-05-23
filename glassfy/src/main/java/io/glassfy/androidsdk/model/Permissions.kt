package io.glassfy.androidsdk.model

data class Permissions(
    val originalApplicationVersion: String,
    val originalApplicationDate: String,
    val subscriberId: String,
    val all: List<Permission>,

    internal var installationId_: String
) {
    val installationId: String get() = installationId_
}

