package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.ENTRA_OID
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.NAVIDENT
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.web.ErrorResponseException


@RestRetryingWhenRecoverableService
class EntraOidTjeneste(private val userClient: EntraUserClient, private val groupClient: EntraGroupClient) {

    private val log = getLogger(javaClass)

    @Cacheable(ENTRA_OID, key = "#ansattId.verdi")
    fun ansattOid(ansattId: AnsattId) =
        with(userClient.users("id", filter = "${EntraGraphAdapter.BRUKER} eq '${ansattId.verdi}'").oids) {
            when (size) {
                0 -> throw EntraIkkeFunnetException(ansattId,
                    "Fant ingen oid for ${ansattId.verdi}, er den fremdeles gyldig?")
                1 -> singleOrNull()?.id.also {
                    log.info("Fant oid {} i Entra for {}", it, ansattId.verdi)
                }
                else -> throw EntraOidException(ansattId.verdi,
                    "Forventet nøyaktig én oid for navident ${ansattId.verdi}, fant $size (${joinToString(", ") { 
                        it.id.toString() }})")
                }
        }

    @Cacheable(ENTRA_OID)
    fun gruppeOid(gruppeNavn: String) =
        groupClient.groups("id,displayName", "displayName eq '$gruppeNavn'").value.firstOrNull()?.id
}

class EntraOidException(ansattId: String, msg: String) : ErrorResponseException(NOT_FOUND) {
    init {
        body.title = "Uventet respons fra Entra"
        body.detail = msg
        body.properties = mapOf(NAVIDENT to ansattId, "traceId" to Span.current().spanContext.traceId)
    }
}
