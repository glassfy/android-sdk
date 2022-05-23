package io.glassfy.androidsdk.model

data class Permission(
    val permissionId: String,
    val entitlement: Entitlement,
    val expireDate: Long,
    val accountableSkus: List<SkuBase> = emptyList(),
) {
    val isValid: Boolean by lazy {
        entitlement.value > 0
    }
}