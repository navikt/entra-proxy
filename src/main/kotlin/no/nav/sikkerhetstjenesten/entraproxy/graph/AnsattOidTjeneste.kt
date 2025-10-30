package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.Duration


@Component
class AnsattOidTjeneste(private val adapter: EntraRestClientAdapter) : CachableRestConfig {


    @Cacheable(cacheNames = [ENTRA_OID],key = "#ansattId.verdi")
     fun oid(ansattId: AnsattId) = adapter.oid(ansattId.verdi)

    override val varighet = Duration.ofDays(365)  // Godt nok, blås i skuddår
    override val navn = ENTRA_OID

    companion object {
        const val ENTRA_OID = "entraoid"
    }
}