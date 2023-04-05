package io.glassfy.androidsdk.internal.repository

import com.squareup.moshi.JsonDataException
import io.glassfy.androidsdk.GlassfyErrorCode
import io.glassfy.androidsdk.internal.network.IApiService
import io.glassfy.androidsdk.internal.network.model.request.ConnectRequest
import io.glassfy.androidsdk.internal.network.model.request.InitializeRequest
import io.glassfy.androidsdk.internal.network.model.request.TokenRequest
import io.glassfy.androidsdk.internal.network.model.request.UserPropertiesRequest
import io.glassfy.androidsdk.internal.network.model.response.ServerInfo
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.*
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException
import java.util.*

internal class Repository(
    private val api: IApiService,
) : IRepository {

    override suspend fun initialize(
        init: InitializeRequest
    ): Resource<ServerInfo> {
        return try {
            val response = api.initialize(init)
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(result.toServerInfo())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun token(token: TokenRequest): Resource<Transaction> {
        return try {
            val response = api.postToken(token)
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(result.toTransaction())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun permissions(): Resource<Permissions> {
        return try {
            val response = api.getPermissions()
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(result.toPermissions())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun lastSeen(): Resource<Unit> {
        return try {
            api.putLastSeen()
            Resource.Success(Unit)
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun restoreTokens(
        historySubs: List<HistoryPurchase>,
        historyInapp: List<HistoryPurchase>
    ): Resource<Permissions> {
        val tokens = historySubs.map { TokenRequest.from(it, true) } + historyInapp.map {
            TokenRequest.from(
                it,
                false
            )
        }
        return try {
            val response = api.postRestoreTokens(tokens)
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(result.toPermissions())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun skuByIdentifier(id: String): Resource<Sku> {
        return try {
            val response = api.getSku(id)
            val result = response.body()
            if (response.isSuccessful && result?.sku != null) {
                when (val sku = result.sku.toSku()) {
                    is Sku -> Resource.Success(sku)
                    else -> Resource.Error(GlassfyErrorCode.UnknowError.toError("Wrong sku store"))
                }
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun skuByIdentifierAndStore(id: String, store: Store): Resource<ISkuBase> {
        return try {
            val instance = Currency.getInstance(Locale.getDefault())
            val response = api.getSku(id, store.value, instance.currencyCode)
            val result = response.body()
            if (response.isSuccessful && result?.sku != null) {
                Resource.Success(result.sku.toSku())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun skuByProductId(id: String): Resource<Sku> {
        return try {
            val response = api.getSkuByProductId(id)
            val result = response.body()
            if (response.isSuccessful && result?.sku != null) {
                Resource.Success(result.sku.toSku() as Sku)
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun offerings(): Resource<Offerings> {
        return try {
            val response = api.getOfferings()
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(result.toOfferings())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun connectCustomSubscriber(connect: ConnectRequest): Resource<Unit> {
        return try {
            val response = api.connectCustomSubscriber(connect)
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(Unit)
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun connectPaddleLicense(connect: ConnectRequest): Resource<Unit> {
        return try {
            val response = api.connectPaddleLicense(connect)
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(Unit)
            } else {
                val err =
                    result?.error?.description?.let {
                        when (result.error.code) {
                            GlassfyErrorCode.LicenseAlreadyConnected.internalCode -> GlassfyErrorCode.LicenseAlreadyConnected.toError(it)
                            GlassfyErrorCode.LicenseNotFound.internalCode -> GlassfyErrorCode.LicenseNotFound.toError(it)
                            else -> GlassfyErrorCode.ServerError.toError(it)
                        }
                    } ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun connectGlassfyUniversalCode(connect: ConnectRequest): Resource<Unit> {
        return try {
            val response = api.connectUniversalCode(connect)
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(Unit)
            } else {
                val err =
                    result?.error?.description?.let {
                        when (result.error.code) {
                            GlassfyErrorCode.UniversalCodeAlreadyConnected.internalCode -> GlassfyErrorCode.LicenseAlreadyConnected.toError(it)
                            GlassfyErrorCode.UniversalCodeNotFound.internalCode -> GlassfyErrorCode.LicenseNotFound.toError(it)
                            else -> GlassfyErrorCode.ServerError.toError(it)
                        }
                    } ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun storeInfo(): Resource<StoresInfo> {
        return try {
            val response = api.getStoreInfo()
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(result.toStoresInfo())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun setUserProperty(req: UserPropertiesRequest): Resource<Unit> {
        return try {
            val response = api.postUserProperty(req)
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(Unit)
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }


    override suspend fun setAttributions(req: Map<String, String?>): Resource<Unit> {
        return try {
            val response = api.postAttributions(req)
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(Unit)
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }


    override suspend fun getUserProperty(): Resource<UserProperties> {
        return try {
            val response = api.getUserProperty()
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(result.toUserProperties())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }

    override suspend fun getPurchaseHistory(): Resource<PurchasesHistory> {
        return try {
            val response = api.getPurchaseHistory()
            val result = response.body()
            if (response.isSuccessful && result != null && result.error == null) {
                Resource.Success(result.toPurchasesHistory())
            } else {
                val err =
                    result?.error?.description?.let { GlassfyErrorCode.ServerError.toError(it) }
                        ?: GlassfyErrorCode.UnknowError.toError(response.message())
                Resource.Error(err)
            }
        } catch (e: HttpException) {
            Resource.Error(GlassfyErrorCode.HttpException.toError(e.message ?: e.toString()))
        } catch (e: UnknownHostException) {
            Resource.Error(GlassfyErrorCode.InternetConnection.toError(e.message ?: e.toString()))
        } catch (e: IOException) {
            Resource.Error(GlassfyErrorCode.IOException.toError(e.message ?: e.toString()))
        } catch (e: JsonDataException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: DTOException) {
            Resource.Error(GlassfyErrorCode.ServerError.toError(e.message ?: e.toString()))
        } catch (e: Exception) {
            Resource.Error(GlassfyErrorCode.UnknowError.toError(e.message ?: e.toString()))
        }
    }
}