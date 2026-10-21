package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class EntraExceptionHandler {

    @ExceptionHandler(EntraIkkeFunnetException::class)
    fun oidIkkeFunnet(e: EntraIkkeFunnetException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(NOT_FOUND, e.message ?: "Fant ikke oid for ${e.ansattId.verdi} i Entra").apply {
            title = "Fant ikke ansatt i Entra"
            setProperty("navIdent", e.ansattId.verdi)
            setProperty("traceId", Span.current().spanContext.traceId)
        }
}
