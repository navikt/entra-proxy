package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable

@RestRetryingWhenRecoverableService
class NomTjeneste(private val nom: NomPort) {

    private val log = getLogger(javaClass)

    @Cacheable(cacheNames = [NOMGRAPH], key = "#ident.verdi")
    fun ansatt(ident: AnsattId): NomAnsatt? =
        nom.ansatt(ident).also {
            log.info("Hentet ansatt $it for navIdent $ident")
        }

    fun ansatte(identer: Set<AnsattId>) =
        nom.ansatte(identer).also {
            log.info("Hentet ansatte $it tema for $identer")
        }

    @Cacheable(cacheNames = [NOMGRAPH], key = "#enhet.verdi")
    fun ansatteForEnhet(enhet: Enhetnummer) =
        nom.ansatteForEnhet(enhet).also {
            log.info("Hentet ${it.size} ansatte for ${enhet.verdi}")
        }

    @Cacheable(cacheNames = [NOMGRAPH], key = "#ident.verdi")
    fun ansatteForNomIdent(ident: NomIdent) =
        nom.ansatteForNomIdent(ident).also {
            log.info("Hentet ansatt $it for nomIdent $ident")
        }

}