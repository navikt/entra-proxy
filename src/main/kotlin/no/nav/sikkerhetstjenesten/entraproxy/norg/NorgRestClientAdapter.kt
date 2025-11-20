package no.nav.sikkerhetstjenesten.entraproxy.norg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient


@Component
class NorgRestClientAdapter(@Qualifier(NORG) restClient: RestClient, val cf: NorgConfig) :
    AbstractRestClientAdapter(restClient, cf) {
    fun navnFor(enhetNr: String) =
        get<NorgEnhetRespons>(cf.enhetURI(enhetNr)).navn

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class NorgEnhetRespons(val enhetNr: Int,val navn: String)
}