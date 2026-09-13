package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.tidOgLog
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.net.URI
import java.util.*

@RetryingWhenRecoverable
@Service
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norg: NorgTjeneste, private val oid: EntraOidTjeneste, private val cache: CacheOperations)  {

    private val log = getLogger(javaClass)

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        tidOgLog(log, "tema for $ansattId") {
            medNotFoundFallback(oid, {
                adapter.temaerForAnsatt("$it")
            }) {
                refreshOid(ansattId)
            }
        }


    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID) =
        tidOgLog(log, "enhet(er) for $ansattId") {
            medNotFoundFallback(oid, ::enheter) {
                refreshOid(ansattId)
            }
        }


    @Cacheable(MEDLEMMER)
    fun medlemmer(gruppeId: UUID) =
        tidOgLog(log, "medlem(mer) for gruppe $gruppeId") {
            adapter.gruppeMedlemmer("$gruppeId")
        }

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: AnsattId) =
        ansatt  {
            adapter.utvidetAnsatt(ansattId.verdi)
        }

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: TIdent) =
        ansatt  {
            adapter.utvidetAnsattTident(ansattId.verdi)
        }


    @Cacheable(GRAPH,key = "#root.methodName + ':' + #navIdent")
    fun grupperForAnsatt(navIdent: AnsattId, oid: UUID) =
        tidOgLog(log) {
            medNotFoundFallback(oid, { adapter.grupperForAnsatt(it.toString()) }) { refreshOid(navIdent) }
        }

    private inline fun <T> medNotFoundFallback(arg: UUID, main: (UUID) -> T, nyOid: (UUID) -> UUID)
            = runCatching { main(arg) }.getOrElse {
        if (it is NotFoundRestException) {
            main(nyOid(arg))
        } else{
            throw it
        }
    }

    private fun enheter(ansattOid: UUID) =
        buildSet {
            adapter.enheterForAnsatt("$ansattOid").forEach {
                add(Enhet(it, norg.navnFor(it)))
            }
        }


    private fun ansatt(block: () -> AnsattRespons?) =
        tidOgLog(log) {
            block()?.let {
                with(it) {
                    val enhetsNummer = Enhetnummer(streetAddress?: UKJENT_ENHET)
                    UtvidetAnsatt(
                        AnsattId(onPremisesSamAccountName), displayName, givenName, surname,
                        TIdent(jobTitle?: TIDENT_DEFAULT),
                        mail,
                        Enhet(enhetsNummer, norg.navnFor(enhetsNummer)))
                }
            }
        }

    private fun refreshOid(navIdent: AnsattId): UUID {
        cache.delete(OID_CACHE,navIdent.verdi).also {
            log.info("Slettet cache innslag før henting av ny oid $navIdent")
        }
        return oid.ansattOid(navIdent).also {
            log.info("Hentet  ny oid $it for $navIdent")
        }
            ?: throw NotFoundRestException(URI.create("http://www.vg.no"), "Fant ikke oid for ${navIdent.verdi} i Entra, selv etter cache-opprydding")
    }

    override fun toString() =
        "${javaClass.simpleName} [adapter=$adapter, norg=$norg]"
}

