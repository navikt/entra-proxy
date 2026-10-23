package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component

@Component
class NomCacheOppfrisker(private val nom: NomTjeneste) : AbstractCacheOppfrisker() {

    override val cacheName: String = NOMGRAPH

    override fun doOppfrisk(nøkkel: CacheNøkkel) {
        val oppfrisk = oppfriskingFor(nøkkel.id)
            ?: return log.warn("Ukjent id-format i nøkkel $nøkkel")
        runCatching {
            oppfrisk()
        }.onFailure {
            loggOppfriskingFeilet(nøkkel, it)
        }
    }

    private fun oppfriskingFor(id: String): (() -> Unit)? =
        AnsattId.parse(id)?.let {
            { nom.ansatt(it) }
        } ?: NomIdent.parse(id)?.let {
            { nom.ansatteForNomIdent(it) }
        } ?: Enhetnummer.parse(id)?.let {
            { nom.ansatteForEnhet(it) }
        }
}
