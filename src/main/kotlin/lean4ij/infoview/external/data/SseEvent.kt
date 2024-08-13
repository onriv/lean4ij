package lean4ij.infoview.external.data

/**
 * The data class representing a SSE Event that will be sent to the client.
 */
data class SseEvent(val data: InfoviewEvent, val event: String? = null, val id: String? = null)