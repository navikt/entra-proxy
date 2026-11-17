package no.nav.sikkerhetstjenesten.entraproxy.graph.exceptions

import io.opentelemetry.api.trace.Span
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.web.ErrorResponseException

class EntraOidException(ansattId: String, msg: String) : ErrorResponseException(NOT_FOUND) {
    init {
        body.title = "Uventet respons fra Entra"
        body.detail = msg
        body.properties = mapOf("navIdent" to ansattId, "traceId" to Span.current().spanContext.traceId)
    }
}
