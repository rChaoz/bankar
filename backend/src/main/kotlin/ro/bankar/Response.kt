package ro.bankar

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import ro.bankar.model.ErrorResponse
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.NotFoundResponse
import ro.bankar.model.Response
import ro.bankar.model.SuccessResponse
import ro.bankar.model.ValueResponse

/**
 * Respond to a call with the given serializable value, to be boxed in [ValueResponse].
 */
suspend inline fun <reified T> ApplicationCall.respondValue(value: T, status: HttpStatusCode = HttpStatusCode.OK) =
    respond<Response<T>>(status, ValueResponse(value))

/**
 * Respond to a call, signaling that a resource was not found.
 */
suspend inline fun ApplicationCall.respondNotFound(resource: String) =
    respond<Response<Unit>>(HttpStatusCode.NotFound, NotFoundResponse(resource))

/**
 * Respond to a call, signaling that a request parameter was invalid, optionally supplying a reason.
 */
suspend inline fun ApplicationCall.respondInvalidParam(param: String, reason: String? = null) =
    respond<Response<Unit>>(HttpStatusCode.BadRequest, InvalidParamResponse(param, reason))

/**
 * Respond to a call with a error message identifier.
 */
suspend inline fun ApplicationCall.respondError(message: String, status: HttpStatusCode = HttpStatusCode.BadRequest) =
    respond<Response<Unit>>(status, ErrorResponse(message))

/**
 * Respond to a call with a success status code.
 */
suspend inline fun ApplicationCall.respondSuccess(status: HttpStatusCode = HttpStatusCode.OK) =
    respond<Response<Unit>>(status, SuccessResponse)