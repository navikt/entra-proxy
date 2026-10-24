package no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component

@Component
class EntraOidCacheOppfrisker(private val oid: EntraOidTjeneste) : AbstractCacheOppfrisker(setOf(OID_CACHE)) {

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        AnsattId.parse(nøkkelElementer.id)?.let { ansattId ->
            oid.ansattOid(ansattId)
        } ?: oid.gruppeOid(nøkkelElementer.id)
    }
}
