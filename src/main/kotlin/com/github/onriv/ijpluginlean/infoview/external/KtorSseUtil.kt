package com.github.onriv.ijpluginlean.infoview.external

import com.github.onriv.ijpluginlean.infoview.external.data.SseEvent
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow

/**
 * Method that responds an [ApplicationCall] by reading all the [SseEvent]s from the specified [eventFlow] [Flow]
 * and serializing them in a way that is compatible with the Server-Sent Events specification.
 *
 * You can read more about it here: https://www.html5rocks.com/en/tutorials/eventsource/basics/
 *
 * This is copied from https://github.com/ktorio/ktor-samples/blob/main/sse/src/main/kotlin/io/ktor/samples/sse/SseApplication.kt
 */
suspend fun ApplicationCall.respondSse(eventFlow: Flow<SseEvent>) {
    response.cacheControl(CacheControl.NoCache(null))
    response.header(
        // for local dev for vite seems to have problem for proxy sse
        // https://github.com/vitejs/vite/issues/12157
        // TODO confirmed this, maybe it's caused by rpc timeout
        HttpHeaders.AccessControlAllowOrigin, "*"
    )
    val gson = Gson()
    respondBytesWriter(contentType = ContentType.Text.EventStream) {
        eventFlow.collect { event ->
            if (event.id != null) {
                writeStringUtf8("id: ${event.id}\n")
            }
            if (event.event != null) {
                writeStringUtf8("event: ${event.event}\n")
            }
            for (dataLine in gson.toJson(event.data).lines()) {
                writeStringUtf8("data: $dataLine\n")
            }
            writeStringUtf8("\n")
            flush()
        }
    }
}
