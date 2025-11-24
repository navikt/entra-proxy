package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.Duration


@Component
class EntraOidTjeneste(private val adapter: EntraRestClientAdapter) : CachableRestConfig {

    override val varighet = Duration.ofDays(365)  // Godt nok, blås i skuddår
    override val navn = ENTRA_OID

    @Cacheable(ENTRA_OID,key = "#ansattId.verdi")
     fun ansattOid(ansattId: AnsattId) =
         adapter.ansattOid(ansattId.verdi)

    @WithSpan
    @Cacheable(ENTRA_OID)
    fun gruppeOid(gruppeNavn: String) =
        adapter.gruppeOid(gruppeNavn)

    companion object {
        const val ENTRA_OID = "entraoid"
    }
}