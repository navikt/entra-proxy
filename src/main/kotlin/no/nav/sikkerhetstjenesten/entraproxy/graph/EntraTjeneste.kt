package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.micrometer.core.annotation.Timed
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerCachableRestConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.tidOgLog
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.net.URI
import java.util.*

@RetryingWhenRecoverable
@Service
@Timed(value = GRAPH, histogram = true)
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norg: NorgTjeneste, private val oid: EntraOidTjeneste, private val cache: CacheClient)  {

    private val log = getLogger(javaClass)

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        tidOgLog(log, "tema for $ansattId") {
            withNotFoundFallbackArg(oid, { adapter.tema("$it") }) { refreshOid(ansattId) }
        }


    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID) =
        tidOgLog(log, "enhet(er) for $ansattId") {
            withNotFoundFallbackArg(oid, ::enheter) { refreshOid(ansattId) }
        }


    @WithSpan
    @Cacheable(MEDLEMMER)
    fun medlemmer(gruppeId: UUID) =
        tidOgLog(log, "medlem(mer) for gruppe $gruppeId") {
            adapter.gruppeMedlemmer("$gruppeId")
        }

    @WithSpan
    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: AnsattId) =
        ansatt  {
            adapter.utvidetAnsatt(ansattId.verdi)
        }

    @WithSpan
    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: TIdent) =
        ansatt  {
            adapter.utvidetAnsattTident(ansattId.verdi)
        }


    @WithSpan
    @Cacheable(GRAPH,key = "#root.methodName + ':' + #navIdent")
    fun grupperForAnsatt(navIdent: AnsattId, oid: UUID) =
        tidOgLog(log) {
            withNotFoundFallbackArg(oid, { adapter.ansatteGrupper(it.toString()) }) { refreshOid(navIdent) }
        }

    private inline fun <T> withNotFoundFallbackArg(
        arg: UUID,
        main: (UUID) -> T,
        fallbackArg: (UUID) -> UUID
    ): T = runCatching { main(arg) }.getOrElse {
        if (it is NotFoundRestException) main(fallbackArg(arg)) else throw it
    }

    private fun enheter(oid: UUID) =  buildSet {
        adapter.enheter("$oid").forEach {
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
        cache.delete(navIdent.verdi, OID_CACHE).also {
            log.info("Slettet cache innslag for $navIdent")
        }
        return oid.ansattOid(navIdent)
            ?: throw NotFoundRestException(URI.create(""), "Fant ikke ${navIdent.verdi} i Entra, selv etter cache-opprydding")
    }

    override fun toString() =
        "${javaClass.simpleName} [adapter=$adapter, norg=$norg]"
}

