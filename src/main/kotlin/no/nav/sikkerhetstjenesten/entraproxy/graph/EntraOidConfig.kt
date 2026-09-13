package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class EntraOidConfig(@param:Value($$"${entraoid.varighet:365d}") override val varighet: Duration) : CachableRestConfig {
    override val navn = ENTRA_OID
    override val caches = setOf(OID_CACHE)

    companion object {
        const val ENTRA_OID = "entraoid"
        val OID_CACHE = CacheNøkkelConfig(ENTRA_OID)
    }
}