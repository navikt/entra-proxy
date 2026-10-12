package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.springframework.cache.annotation.Cacheable

@RestRetryingWhenRecoverableService
class NomTjeneste(private val nom: NomPort) {

    @Cacheable(cacheNames = [NOMGRAPH], key = "#root.methodName + ':' + #ident.verdi")
    fun orgData(ident: AnsattId): NomAnsatt =
        nom.ansatt(ident)

    fun orgDataBulk(identer: Set<AnsattId>): Set<NomAnsatt> =
        nom.ansatte(identer)

    @Cacheable(cacheNames = [NOMGRAPH], key = "#root.methodName + ':' + #tilgangsenhetId.verdi")
    fun ansatteForTilgangsenhet(tilgangsenhetId: Enhetnummer): Set<NomAnsatt> =
        nom.ansatteForTilgangsenhet(tilgangsenhetId)

}