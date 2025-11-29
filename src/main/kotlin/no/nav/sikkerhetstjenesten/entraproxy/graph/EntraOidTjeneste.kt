package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidCachableRestConfig.Companion.ENTRA_OID
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


@Component
class EntraOidTjeneste(private val adapter: EntraRestClientAdapter)  {

    @Cacheable(ENTRA_OID,key = "#ansattId.verdi")
     fun ansattOid(ansattId: AnsattId) =
         adapter.ansattOid(ansattId.verdi)

    @WithSpan
    @Cacheable(ENTRA_OID)
    fun gruppeOid(gruppeNavn: String) =
        adapter.gruppeOid(gruppeNavn)
}

