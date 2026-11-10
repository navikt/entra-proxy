package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.ENTRA_OID
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable


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
                else -> throw EntraOidException(ansattId, size,
                    "Forventet nøyaktig én oid for navident ${ansattId.verdi}, fant $size (${joinToString(", ") { 
                        it.id.toString() }})")
                }
        }

    @Cacheable(ENTRA_OID)
    fun gruppeOid(gruppeNavn: String) =
        groupClient.groups("id,displayName", "displayName eq '$gruppeNavn'").value.firstOrNull()?.id
}

class EntraOidException(val ansattId: AnsattId, val antallTreff: Int, msg: String) : RuntimeException(msg)
