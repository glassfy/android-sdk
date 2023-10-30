package io.glassfy.androidsdk

data class GlassfyError(val code: GlassfyErrorCode, val description: String?, val debug: String?)

enum class GlassfyErrorCode(internal val internalCode: Int? = null) {
    SDKNotInitialized,
    MissingPurchase,
    PendingPurchase,
    Purchasing(-199),

    StoreError,
    UserCancelPurchase,
    ProductAlreadyOwned,

    ServerError,
    LicenseAlreadyConnected(1050),
    LicenseNotFound(1051),
    UniversalCodeAlreadyConnected(1050),
    UniversalCodeNotFound(1051),
    InternetConnection,
    IOException,
    HttpException,

    NotFoundOnGlassfy,
    NotFoundOnStore,

    CouldNotBuildPaywall,

    UnknowError;

    fun toError(debug: String? = null): GlassfyError {
        return GlassfyError(this, description(), debug)
    }

    private fun description(): String {
        return when (this) {
            SDKNotInitialized -> "SDK not initialized"
            MissingPurchase -> "Purchase"
            PendingPurchase -> "Purchase is pending"
            Purchasing -> "Purchasing"
            StoreError -> "Store error"
            UserCancelPurchase -> "User cancel purchase"
            ProductAlreadyOwned -> "Product already owned"
            ServerError -> "Server error"
            LicenseAlreadyConnected -> "License already connected"
            LicenseNotFound -> "License not found"
            UniversalCodeAlreadyConnected -> "Universal Code already connected"
            UniversalCodeNotFound -> "Universal Code not found"
            InternetConnection -> "Check your internet connection"
            IOException -> "IOException"
            HttpException -> "HttpException"
            NotFoundOnGlassfy -> "Product not found on Glassfy. Did you add to the dashboard?"
            NotFoundOnStore -> "Product not found on Store"
            CouldNotBuildPaywall -> "Failed to build paywall"
            UnknowError -> "Unexpected error"
        }
    }
}
