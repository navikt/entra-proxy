package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheBeanConfig.Companion.VALKEY_MAPPER
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isLocalOrTest
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isProd
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.maskFnr
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.text.Charsets.UTF_8
import kotlin.time.TimeSource.Monotonic.markNow


private val BATCH_SIZE = 10_000
private val SCRIPT = RedisScript.of(ClassPathResource("scripts/count-all-keys.lua"), List::class.java)

@Component
class ValkeyCacheOperations(
    private val valkey: StringRedisTemplate,
    vararg cfgs: CachableRestConfig) : CacheOperations {

    private val log = getLogger(javaClass)
    private val defaultTtlForCache = cfgs.flatMap { cfg ->
        cfg.caches.map { cache -> cache.fullName to cfg.varighet }
    }.toMap()

    init {
        if (isLocalOrTest) {
            runCatching {
                valkey.execute { connection ->
                    connection.serverCommands().setConfig("notify-keyspace-events", "Exg")
                }
            }.onFailure {
                log.warn("Klarte ikke å sette notify-keyspace-events=Exg for Valkey i lokal/test", it)
            }
        }
    }

    override fun replaceSet(nøkkel: String, verdier: Set<String>) {
        if (verdier.isEmpty()) return deleteSet(nøkkel)
        val tmp = "$nøkkel:tmp:${UUID.randomUUID()}"
        runCatching {
            valkey.opsForSet().add(tmp, *verdier.toTypedArray())
            valkey.expire(tmp, Duration.ofMinutes(5)) // selv-opprydding dersom rename eller onFailure-sletting feiler
            valkey.rename(tmp, nøkkel)
        }.onFailure {
            log.warn("Cache replaceSet feilet for nøkkel {}: {}", nøkkel, it.message, it)
            valkey.delete(tmp)
        }
    }

    override fun deleteSet(nøkkel: String) {
        runCatching {
            valkey.delete(nøkkel)
        }.onFailure {
            log.warn("Cache deleteSet feilet for nøkkel {}: {}", nøkkel, it.message, it)
        }
    }

    override fun getSet(nøkkel: String)  =
        runCatching {
            valkey.opsForSet().members(nøkkel)
        }.onFailure {
            log.warn("Cache getSet feilet for nøkkel {}: {}", nøkkel, it.message, it)
        }.getOrNull().orEmpty()

    override fun inneholder(nøkkel: String, verdi: String) =
        runCatching {
            valkey.opsForSet().isMember(nøkkel, verdi)
        }.onFailure {
            log.warn("Cache set inneholder feilet for nøkkel {} og verdi {}: {}", nøkkel, verdi, it.message, it)
        }.getOrElse {
            false
        }

    override fun delete(cache: CacheNøkkelConfig, id: String) =
        runCatching { valkey.unlink(cache.tilNøkkel(id)) }
            .onFailure {
                log.info("Cache delete feilet for {} nøkkel {}: {}", cache.fullName, id.maskFnr(), it.message, it)
            }.getOrElse {
                false
            }

    override fun <T : Any> getOne(cache: CacheNøkkelConfig, id: String, clazz: KClass<T>): T? {
        return runCatching {
            valkey.opsForValue().get(cache.tilNøkkel(id))?.let { VALKEY_MAPPER.readValue(it, clazz.java) }
        }.onFailure {
            log.info("Cache getOne feilet for {}, faller tilbake til tjenestekall", cache.fullName, it)
        }.getOrNull()
    }

    override fun putOne(cache: CacheNøkkelConfig, id: String, value: Any, ttl: Duration?) {
        runCatching {
            val key = cache.tilNøkkel(id)
            val json = VALKEY_MAPPER.writeValueAsString(value)
            val ops = valkey.opsForValue()
            effectiveTTL(cache, ttl)?.let {
                    ttlToUse ->  ops.set(key, json, ttlToUse)
            } ?: ops.set(key, json)
        }.onFailure {
            log.info("Cache putOne feilet for {} nøkkel {}: {}", cache.fullName, id, it.message, it)
        }
    }


    override fun <T : Any> getMany(cache: CacheNøkkelConfig, ids: Set<String>, clazz: KClass<T>) =  doGetMany(cache, ids.toList(), clazz)


    override fun putMany(cache: CacheNøkkelConfig, innslag: Map<String, Any>, ttl: Duration?) {
        val ttlToUse = effectiveTTL(cache, ttl)
        when {
            innslag.isEmpty() -> return
            innslag.size == 1 -> doPutOne(cache, innslag, ttlToUse)
            else -> doPutMany(cache, innslag, ttlToUse?.seconds)
        }
    }

    override fun clear(cache: CacheNøkkelConfig): Long {
        check(!isProd) { "Clear er ikke støttet i prod for å unngå utilsiktet sletting av cache-innhold" }
        log.info("Tømmer cache {}", cache.name)
        return valkey.execute {
            (it.keyCommands().scan(scanOptions(cache)) as Cursor<ByteArray>).use { cursor ->
                val batch = mutableListOf<String>()
                var deleted = 0L
                cursor.forEach {
                    keyBytes -> batch += keyBytes.toString(UTF_8)
                    if (batch.size == BATCH_SIZE) {
                        deleted += batch.size.toLong()
                        valkey.delete(batch)
                        batch.clear()
                    }
                }
                deleted += batch.size.toLong()
                valkey.delete(batch)
                deleted
            }
        }
    }

    override fun clearAll(): Long {
        check(!isProd) { "FlushDb er ikke støttet i prod for å unngå utilsiktet sletting av cache-innhold" }
        val before = valkey.execute { it.serverCommands().dbSize() } ?: 0L
        log.info("Tømmer hele Valkey-databasen, størrelse før tømming: {}", before)
        valkey.execute {
            it.serverCommands().flushDb()
        }
        return valkey.execute { it.serverCommands().dbSize() } ?: 0L
    }

    override fun sizes(vararg caches: CacheNøkkelConfig): Map<String, Long> {
        markNow().let { start ->
            val prefixes = caches.map { "${it.tilNøkkel("")}*" }

            @Suppress("UNCHECKED_CAST")
            val results = (valkey.execute(SCRIPT, emptyList(), *prefixes.toTypedArray()) as List<Number>)
                .map(Number::toLong)
            val totalDuration = start.elapsedNow()
            return caches.zip(results).associate {
                (cache, count) -> cache.fullName to count
            }
                .also { log.info("Cache størrelser {} slått opp, tok {}ms", it, totalDuration.inWholeMilliseconds) }
        }
    }

    private fun effectiveTTL(cache: CacheNøkkelConfig, ttl: Duration?) =
        ttl ?: defaultTtlForCache[cache.fullName]

    private fun <T : Any> doGetMany(cache: CacheNøkkelConfig,
                                    requestedIds: List<String>,
                                    clazz: KClass<T>): Map<String, T?> {
        markNow().let { start ->
            return runCatching {
                val values = valkey.opsForValue().multiGet(requestedIds.map(cache::tilNøkkel)).orEmpty()
                requestedIds.mapIndexedNotNull {
                    index, id ->
                    values.getOrNull(index)?.let { value ->
                        id to VALKEY_MAPPER.readValue<T>(value, clazz.java)
                    }
                }.toMap()
            }.onSuccess {
                verdier -> val varighet = start.elapsedNow()
                log.trace("getMany {} hentet {} av {} nøkler på {}ms",
                    cache.fullName, verdier.size, requestedIds.size, varighet.inWholeMilliseconds)
            }.onFailure {
                log.warn("{} getMany feilet for {} med {} nøkler: {}",
                    javaClass.simpleName, cache.fullName, requestedIds.size, it.message, it)
            }.getOrElse { emptyMap() }
        }
    }

    private fun scanOptions(cache: CacheNøkkelConfig) =
        ScanOptions.scanOptions().match("${cache.tilNøkkel("")}*").count(BATCH_SIZE.toLong()).build()

    private fun doPutOne(cache: CacheNøkkelConfig,
                         innslag: Map<String, Any>,
                         ttl: Duration?) {
        with(innslag.entries.single()) {
            putOne(cache, key, value, ttl)
        }
    }

    private fun doPutMany(cache: CacheNøkkelConfig,
                          innslag: Map<String, Any>,
                          ttl: Long?) {
        markNow().let { start ->
            val payload = innslag.entries.associate { (key, value) ->
                cache.tilNøkkel(key) to VALKEY_MAPPER.writeValueAsString(value)
            }
            val resultat = pipeline(payload, ttl)
            resultat.onSuccess {
                log.trace("Cache putMany {} lagret {} nøkler på {}ms",
                    cache.fullName,
                    innslag.size,
                    start.elapsedNow().inWholeMilliseconds)
            }.onFailure {
                log.warn("Cache putMany feilet for {} med {} nøkler: {}",
                    cache.fullName,
                    innslag.size,
                    it.message,
                    it)
            }
        }
    }

    private fun pipeline(payload: Map<String, String>, ttl: Long?) =
        runCatching {
            valkey.executePipelined { connection ->
                payload.forEach { (key, value) ->
                    val keyBytes = key.toByteArray()
                    val valueBytes = value.toByteArray()
                    if (ttl != null) {
                        connection.stringCommands().setEx(keyBytes, ttl, valueBytes)
                    } else {
                        connection.stringCommands().set(keyBytes, valueBytes)
                    }
                }
                null
            }
        }
}


