package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
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

    @ExceptionHandler(ForMangeMatchendeGrupperException::class)
    fun forMangeGrupper(e: ForMangeMatchendeGrupperException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(UNPROCESSABLE_CONTENT, e.message ?: "For mange matchende grupper").apply {
            title = "For bredt søk"
            setProperty("gruppenavnSubstring", e.gruppenavnSubstring)
            setProperty("antall", e.antall)
            setProperty("maks", e.maks)
            setProperty("traceId", Span.current().spanContext.traceId)
        }
}
