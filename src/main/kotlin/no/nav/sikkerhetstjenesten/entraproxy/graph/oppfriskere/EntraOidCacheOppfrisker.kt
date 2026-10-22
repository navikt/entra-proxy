package no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.ENTRA_OID
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component

@Component
class EntraOidCacheOppfrisker(private val oid: EntraOidTjeneste) : AbstractCacheOppfrisker() {

    override val cacheName: String = ENTRA_OID

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        val ansattId = runCatching { AnsattId(nøkkelElementer.id) }.getOrNull()
        runCatching {
            if (ansattId != null) {
                oid.ansattOid(ansattId)
            } else {
                oid.gruppeOid(nøkkelElementer.id)
            }
        }.onFailure {
            loggOppfriskingFeilet(nøkkelElementer, it)
        }
    }
}
