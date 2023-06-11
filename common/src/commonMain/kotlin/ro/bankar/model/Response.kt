package ro.bankar.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ResponseSerializer::class)
sealed class Response<T>

/**
 * Used to respond with a status, that defaults to "invalid", and a parameter name, to indicate that the request contained an invalid parameter value.
 * @param param request parameter that is invalid
 */
@Serializable
data class InvalidParamResponse<T>(val param: String, val reason: String? = null) : Response<T>()

/**
 * Used to indicate that a resource was not found
 * @param resource what was not found
 */
@Serializable
data class NotFoundResponse<T>(val resource: String) : Response<T>()

/**
 * Used to indicate a general failure, as described by `message`
 */
@Serializable
data class ErrorResponse<T>(val message: String) : Response<T>()

/**
 * Used to indicate success, when no data should be sent
 */
@Serializable
object SuccessResponse : Response<Unit>()

/**
 * Used to indicate success, returning requested data
 */
@Serializable
class ValueResponse<T>(val value: T) : Response<T>()

private class ResponseSerializer<T : Any>(tKSerializer: KSerializer<T>) : KSerializer<Response<T>> {
    @Serializable
    @OptIn(ExperimentalSerializationApi::class)
    private data class ResponseSurrogate<T : Any>(
        val type: Type,

        // Don't encode these reason when null
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val arg: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val reason: String? = null,

        val value: T? = null
    ) {
        @Serializable
        enum class Type {
            InvalidParam, NotFound, Error, Success, Value
        }
    }

    private val surrogateSerializer = ResponseSurrogate.serializer(tKSerializer)
    override val descriptor = surrogateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Response<T>) = surrogateSerializer.serialize(
        encoder, when (value) {
            is InvalidParamResponse -> ResponseSurrogate(ResponseSurrogate.Type.InvalidParam, value.param, value.reason)
            is NotFoundResponse -> ResponseSurrogate(ResponseSurrogate.Type.NotFound, value.resource)
            is ErrorResponse -> ResponseSurrogate(ResponseSurrogate.Type.Error, value.message)
            SuccessResponse -> ResponseSurrogate(ResponseSurrogate.Type.Success)
            is ValueResponse -> ResponseSurrogate(ResponseSurrogate.Type.Value, value = value.value)
        }
    )

    override fun deserialize(decoder: Decoder): Response<T> {
        val s = surrogateSerializer.deserialize(decoder)
        @Suppress("UNCHECKED_CAST")
        return when (s.type) {
            ResponseSurrogate.Type.InvalidParam -> InvalidParamResponse(s.arg!!, s.reason)
            ResponseSurrogate.Type.NotFound -> NotFoundResponse(s.arg!!)
            ResponseSurrogate.Type.Error -> ErrorResponse(s.arg!!)
            ResponseSurrogate.Type.Success -> SuccessResponse as Response<T>
            ResponseSurrogate.Type.Value -> ValueResponse(s.value!!)
        }
    }
}