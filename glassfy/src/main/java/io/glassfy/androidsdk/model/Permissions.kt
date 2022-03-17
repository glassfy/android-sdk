package io.glassfy.androidsdk.model

data class Permissions(
    var originalApplicationVersion: String,
    var originalApplicationDate: String,
    val subscriberId: String,
    val all: List<Permission>,
)

