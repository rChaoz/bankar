package ro.bankar

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import ro.bankar.model.Response
import ro.bankar.model.ValueResponse
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal fun ApplicationTestBuilder.baseClient(
    token: String? = null,
    defaultRequest:  DefaultRequest. DefaultRequestBuilder.() -> Unit = {},
    block: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {}
) = createClient {
    install(ContentNegotiation) { json() }
    defaultRequest {
        contentType(ContentType.Application.Json)
        url.path("api/")
        if (token != null) header(HttpHeaders.Authorization, if (token.startsWith("Bearer")) token else "Bearer $token")
        defaultRequest()
    }
    block()
}

internal suspend inline fun <reified T> HttpClient.getValue(path: String): T {
    val response = get(path)
    assertTrue { response.status.value in 200..299 }
    val body = response.body<Response<T>>()
    assertIs<ValueResponse<T>>(body)
    return body.value
}

internal fun HttpResponse.assertOK(code: HttpStatusCode = HttpStatusCode.OK) = also { assertEquals(code, status) }