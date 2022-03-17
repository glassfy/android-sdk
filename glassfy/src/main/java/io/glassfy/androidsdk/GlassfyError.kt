package io.glassfy.androidsdk

data class GlassfyError(val code: GlassfyErrorCode, val description: String?, val debug: String?)

enum class GlassfyErrorCode {
    SDKNotInitialized,
    MissingPurchase,
    PendingPurchase,
    Purchasing,

    StoreError,
    UserCancelPurchase,
    ProductAlreadyOwned,

    ServerError,
    InternetConnection,
    IOException,
    HttpException,

    NotFoundOnGlassfy,
    NotFoundOnStore,

    UnknowError;

    internal fun toError(debug: String? = null): GlassfyError {
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
            InternetConnection -> "Check your internet connection"
            IOException -> "IOException"
            HttpException -> "HttpException"
            NotFoundOnGlassfy -> "Product not found on Glassfy. Did you add to the dashboard?"
            NotFoundOnStore -> "Product not found on Store"
            UnknowError -> "Unexpected error"
        }
    }
}
