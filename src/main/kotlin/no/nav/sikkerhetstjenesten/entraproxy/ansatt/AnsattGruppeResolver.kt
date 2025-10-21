package no.nav.sikkerhetstjenesten.entraproxy.ansatt


import no.nav.sikkerhetstjenesten.entraproxy.ansatt.GlobalGruppe.Companion.girNasjonalTilgang
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.GlobalGruppe.Companion.globaleGrupper
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isProd
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.Token
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException

@Component
class AnsattGruppeResolver(private val entra: EntraTjeneste, private val token: Token, private val oid: AnsattOidTjeneste)  {

    private val log = getLogger(javaClass)

    fun grupperForAnsatt(ansattId: AnsattId) =
        when {
            token.erCC ->  grupperForCC(ansattId)
            token.erObo -> grupperForObo(ansattId)
            else -> grupperForUautentisert(ansattId)
        }

    private fun grupperForCC(ansattId: AnsattId) =
        entra.geoOgGlobaleGrupper(ansattId, oid.oidFraEntra(ansattId)).also {
            log.trace("CC-flow: {} slo opp globale og GEO-grupper i Entra", ansattId)
        }

    private fun grupperForObo(ansattId: AnsattId) = with(token.globaleGrupper()) {
        if (girNasjonalTilgang()) {
            this.also {
                log.trace("OBO-flow: {} har nasjonal tilgang, slo *ikke* opp GEO-grupper i Entra", ansattId)
            }
        } else {
            (this + entra.geoGrupper(ansattId, token.oid!!)).also {
                log.trace("OBO-flow: {} har ikke nasjonal tilgang, slo opp GEO-grupper i Entra", ansattId)
            }
        }
    }
    private fun grupperForUautentisert(ansattId: AnsattId) =
        if (isProd) {
            throw HttpClientErrorException(UNAUTHORIZED, "Autentisering påkrevet i produksjonsmiljøet", HttpHeaders(), null, null)
        } else {
            log.info("Intet token i dev/local for $ansattId, slår opp globale og GEO-grupper i Entra")
            entra.geoOgGlobaleGrupper(ansattId, oid.oidFraEntra(ansattId)).also {
                log.trace("Uautentisert: $ansattId slo opp $it i Entra for $ansattId")
            }
        }
}
