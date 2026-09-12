package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENTRA_OID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class EntraOidCachableRestConfig(@param:Value($$"${entraoid.varighet:365d}") override val varighet: Duration) :
    CachableRestConfig {
    override val navn = ENTRA_OID

    companion object {
    }
}