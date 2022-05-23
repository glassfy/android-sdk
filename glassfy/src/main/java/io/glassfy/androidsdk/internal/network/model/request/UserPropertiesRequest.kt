package io.glassfy.androidsdk.internal.network.model.request

internal sealed class UserPropertiesRequest {
    class Email(val email: String?) : UserPropertiesRequest()
    class Token(val token: String?) : UserPropertiesRequest()
    class Extra(val info: Map<String, String?>?) : UserPropertiesRequest()
}