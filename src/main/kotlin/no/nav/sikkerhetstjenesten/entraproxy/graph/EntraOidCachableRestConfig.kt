package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class EntraOidCachableRestConfig(@param:Value("\${entraoid.varighet:365d}") override val varighet: Duration) :
    CachableRestConfig {
    override val navn = ENTRA_OID

    override val caches = setOf(
        CacheNøkkelConfig(navn, GRUPPE_OID),
        CacheNøkkelConfig(navn, ANSATT_OID))

    companion object {
        const val ENTRA_OID = "entraoid"
        const val GRUPPE_OID = "gruppeOid"
        const val ANSATT_OID = "ansattOid"
        val GRUPPE_OID_CACHE = CacheNøkkelConfig(ENTRA_OID, GRUPPE_OID)
        val ANSATT_OID_CACHE = CacheNøkkelConfig(ENTRA_OID, ANSATT_OID)
    }
}