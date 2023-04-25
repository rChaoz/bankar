package ro.bankar.model

import kotlinx.serialization.Serializable

/**
 * Used to respond with a status, that defaults to "invalid", and a parameter name, to indicate that the request contained an invalid parameter value.
 * @param status defaults to "invalid"
 * @param param request parameter that is invalid
 */
@Serializable
data class InvalidParamResponse(val status: String = "invalid", val param: String)

/**
 * Used to indicate that a resource was not found
 */
@Serializable
data class NotFoundResponse(val status: String = "not_found", val resource: String)

/**
 * Used to respond with just a status
 * @param status "success", if request succeeded, or an error code
 */
@Serializable
data class StatusResponse(val status: String) {
    companion object {
        val Success = StatusResponse("success")
    }
}