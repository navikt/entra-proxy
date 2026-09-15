package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.OAuth2DownstreamURIContext.currentUri
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RestRetryingWhenRecoverableService
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.ENTRA_OID
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.web.ErrorResponseException


@RestRetryingWhenRecoverableService
class EntraOidTjeneste(private val client: EntraGraphClient)  {

    private val log = getLogger(javaClass)

    @Cacheable(ENTRA_OID,key = "#ansattId.verdi")
     fun ansattOid(ansattId: AnsattId) =
         with(client.users("id", filter = "$BRUKER eq '${ansattId.verdi}'").oids) {
             log.info("Fant $size oids ($this) i Entra for ${ansattId.verdi}")
             when (size) {
                 0 -> throw NotFoundRestException(currentUri, msg = "Fant ingen oid for navident ${ansattId.verdi}, er den fremdeles gyldig?")
                 1 -> singleOrNull()?.id
                 else -> throw EntraOidException(ansattId.verdi, "Forventet nøyaktig én oid for navident ${ansattId.verdi}, fant $size (${joinToString(", ") { it.id.toString() }})")
             }
         }

    @Cacheable(ENTRA_OID)
    fun gruppeOid(gruppeNavn: String) =
        client.groups("id,displayName", "displayName eq '$gruppeNavn'").value.firstOrNull()?.id
}

class EntraOidException(ansattId: String, msg: String) : ErrorResponseException(NOT_FOUND) {
    init {
        body.title = "Uventet respons fra Entra"
        body.detail = msg
        body.properties = mapOf("navIdent" to ansattId, "traceId" to Span.current().spanContext.traceId)
    }
}
