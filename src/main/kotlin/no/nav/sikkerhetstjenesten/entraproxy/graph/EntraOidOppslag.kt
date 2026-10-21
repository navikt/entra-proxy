package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import org.slf4j.LoggerFactory.getLogger
import java.util.UUID

class EntraOidOppslag(private val oid: EntraOidTjeneste, private val cache: CacheOperations) : OidOperations {

    private val log = getLogger(javaClass)

    override fun ansattOid(ansattId: AnsattId) =
        oid.ansattOid(ansattId)

    override fun nyOid(ansattId: AnsattId): UUID {
        cache.delete(OID_CACHE, ansattId.verdi).also {
            log.info("Slettet cache innslag før henting av ny oid $ansattId")
        }
        return oid.ansattOid(ansattId) ?: throw EntraIkkeFunnetException(ansattId, "Fant ikke ny oid for $ansattId i Entra, selv etter cache-opprydding og nytt oppslag")
    }

    override fun toString() =
        "${javaClass.simpleName} [oid=$oid]"
}
