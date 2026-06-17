package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor.INITIAL
import io.lettuce.core.ScriptOutputType.MULTI
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheBeanConfig.Companion.VALKEY_MAPPER
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Operasjon
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Operasjon.CLEAR
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Operasjon.DELETE
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Operasjon.GET_ONE
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Operasjon.PUT_ONE
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Resultat
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Resultat.FEILET
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Resultat.HIT
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Resultat.MISS
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheTeller.Resultat.OK
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isLocalOrTest
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isProd
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Duration.ofSeconds
import kotlin.reflect.KClass
import kotlin.text.Charsets.UTF_8
import kotlin.time.measureTimedValue


@Component
class ValkeyCacheOperations(client: RedisClient, private val teller: ValkeyCacheTeller) : CacheOperations {
    private val conn = client.connect().apply {
        timeout = ofSeconds(30)
        if (isLocalOrTest) {
            sync().configSet("notify-keyspace-events", "Exd")
        }
    }

    private val log = getLogger(javaClass)

    @WithSpan
    override fun <T : Any> getOne(cache: CacheNøkkelConfig, id: String, clazz: KClass<T>): T? =
        runCatching {
            conn.sync().get(cache.tilNøkkel(id))?.let {
                VALKEY_MAPPER.readValue(it, clazz.java).also {
                    teller.tell(GET_ONE, cache.name, HIT)
                }
            } ?: run {
                teller.tell(GET_ONE, cache.name, MISS);
                null
            }
        }.getOrElse { e ->
            teller.tell(GET_ONE, cache.name, FEILET)
            log.info("Cache getOne feilet for {}, faller tilbake til tjenestekall", cache.name, e)
            null
        }

    @WithSpan
    override fun putOne(cache: CacheNøkkelConfig, id: String, value: Any, ttl: Duration) {
        conn.async().setex(cache.tilNøkkel(id), ttl.seconds, VALKEY_MAPPER.writeValueAsString(value))
        teller.tell(PUT_ONE, cache.name, OK)
    }

    override fun sizes(vararg caches: CacheNøkkelConfig): Map<String, Long> {
        val prefixes = caches.map { "${it.tilNøkkel("")}*" }.toTypedArray()
        val (results, tid) = eval(*prefixes)
        return caches.zip(results).associate {
                (cache, count) -> cache.fullName to count
        }.also {
            log.info("Cache størrelser {} slått opp, tok {}ms", it, tid.inWholeMilliseconds)
        }
    }

    @WithSpan
    override fun delete(cache: CacheNøkkelConfig, id: String) =
        conn.sync().del(cache.tilNøkkel(id)).also {
            teller.tell(DELETE, cache.name, OK)
        }
    override fun clear(cache: CacheNøkkelConfig) {
        check(!isProd) { "Clear er ikke støttet i prod for å unngå utilsiktet sletting av cache-innhold" }
        log.info("Tømmer cache {}", cache.name)
        val prefix = cache.tilNøkkel("")
        var cursor = INITIAL
        val args = ScanArgs().match("$prefix*").limit(10000)
        var slettet = 0L
        do {
            val result = conn.sync().scan(cursor, args)
            if (result.keys.isNotEmpty()) {
                slettet += conn.sync().del(*result.keys.toTypedArray())
            }
            cursor = result
        } while (!result.isFinished)
        teller.tell(CLEAR, cache.name, OK, slettet.toInt())
    }

    private fun eval(vararg prefixes: String) =
        measureTimedValue {
            conn.sync().eval<List<Long>>(SCRIPT, MULTI, emptyArray(), *prefixes)
        }
    companion object {
        private val SCRIPT = ClassPathResource("scripts/count-all-keys.lua").getContentAsString(UTF_8)
    }
}

