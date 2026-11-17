package no.nav.sikkerhetstjenesten.entraproxy.graph.tjeneste

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.EntraGraphAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.EntraGraphClient
import no.nav.sikkerhetstjenesten.entraproxy.graph.config.EntraOidConfig.Companion.ENTRA_OID
import no.nav.sikkerhetstjenesten.entraproxy.graph.exceptions.EntraIkkeFunnetException
import no.nav.sikkerhetstjenesten.entraproxy.graph.exceptions.EntraOidException
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable


@RestRetryingWhenRecoverableService
class EntraOidTjeneste(private val client: EntraGraphClient) {

    private val log = getLogger(javaClass)

    @Cacheable(ENTRA_OID, key = "#ansattId.verdi")
    fun ansattOid(ansattId: AnsattId) =
        with(client.users("id", filter = "${EntraGraphAdapter.BRUKER} eq '${ansattId.verdi}'").oids) {
            when (size) {
                0 -> throw EntraIkkeFunnetException(ansattId,
                    "Fant ingen oid for ${ansattId.verdi}, er den fremdeles gyldig?")
                1 -> singleOrNull()?.id.also {
                    log.info("Fant oid {} i Entra for {}", it, ansattId.verdi)
                }
                else -> throw EntraOidException(ansattId.verdi,
                    "Forventet nøyaktig én oid for navident ${ansattId.verdi}, fant $size (${joinToString(", ") { it.id.toString() }})")
            }
        }

    @Cacheable(ENTRA_OID)
    fun gruppeOid(gruppeNavn: String) =
        client.groups("id,displayName", "displayName eq '$gruppeNavn'").value.firstOrNull()?.id
}
