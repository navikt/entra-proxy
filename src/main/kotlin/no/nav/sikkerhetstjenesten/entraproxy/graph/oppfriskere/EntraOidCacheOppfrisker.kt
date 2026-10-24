package no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component

@Component
class EntraOidCacheOppfrisker(private val oid: EntraOidTjeneste, cfg: EntraOidConfig) : AbstractCacheOppfrisker(cfg) {

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        AnsattId.parse(nøkkelElementer.id)?.let { ansattId ->
            oid.ansattOid(ansattId)
        } ?: oid.gruppeOid(nøkkelElementer.id)
    }
}
