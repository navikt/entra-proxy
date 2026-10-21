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

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        val oppfrisk = oppfriskingFor(nøkkelElementer.id)
            ?: return log.warn("Ukjent id-format i nøkkel $nøkkelElementer")
        runCatching {
            oppfrisk()
        }.onFailure {
            loggOppfriskingFeilet(nøkkelElementer, it)
        }
    }

    private fun oppfriskingFor(id: String): (() -> Unit)? =
        id.somAnsattId()?.let { { nom.ansatt(it) } }
            ?: id.somNomIdent()?.let { { nom.ansatteForNomIdent(it) } }
            ?: id.somEnhetnummer()?.let { { nom.ansatteForEnhet(it) } }

    private fun String.somAnsattId() = runCatching { AnsattId(this) }.getOrNull()

    private fun String.somNomIdent() = runCatching { NomIdent(this) }.getOrNull()

    private fun String.somEnhetnummer() = runCatching { Enhetnummer(this) }.getOrNull()
}
