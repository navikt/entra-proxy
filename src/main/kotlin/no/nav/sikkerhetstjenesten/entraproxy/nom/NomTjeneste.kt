package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.springframework.cache.annotation.Cacheable

@RestRetryingWhenRecoverableService
class NomTjeneste(private val nom: NomPort) {

    @Cacheable(cacheNames = [NOMGRAPH], key = "#ident.verdi")
    fun orgData(ident: AnsattId)  =
        nom.ansatt(ident)

    fun orgDataBulk(identer: Set<AnsattId>) =
        nom.ansatte(identer)

    @Cacheable(cacheNames = [NOMGRAPH], key = "#enhet.verdi")
    fun ansatteForEnhet(enhet: Enhetnummer) =
        nom.ansatteForEnhet(enhet)

    @Cacheable(cacheNames = [NOMGRAPH], key = "#ident.verdi")
    fun ansatteForNomIdent(ident: NomIdent) =
        nom.ansatteForNomIdent(ident)

}