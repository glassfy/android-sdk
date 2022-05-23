package io.glassfy.androidsdk.internal.network.model.utils

import com.squareup.moshi.*
import io.glassfy.androidsdk.internal.network.model.request.UserPropertiesRequest


internal class UserPropertiesAdapter {

    @ToJson
    fun toJson(
        writer: JsonWriter,
        usrProp: UserPropertiesRequest,
        delegate: JsonAdapter<Map<String, String?>>
    ) {
        writer.serializeNulls = true

        writer.beginObject()
        when (usrProp) {
            is UserPropertiesRequest.Email -> writer.name("email").value(usrProp.email)
            is UserPropertiesRequest.Token -> writer.name("token").value(usrProp.token)
            is UserPropertiesRequest.Extra -> {
                writer.name("info")
                delegate.serializeNulls().toJson(writer, usrProp.info)
            }
        }
        writer.endObject()
    }

    @Suppress("UNUSED_PARAMETER")
    @FromJson
    fun fromJson(reader: JsonReader): UserPropertiesRequest? = null

}