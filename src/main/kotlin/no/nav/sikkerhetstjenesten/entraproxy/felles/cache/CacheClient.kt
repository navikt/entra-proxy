package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.lettuce.core.RedisClient
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isLocalOrTest
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.format
import org.springframework.stereotype.Component
import java.time.Duration


@Component
class CacheClient(client: RedisClient, private val adapter : CacheStørrelseAdapter,  val handler: CacheNøkkelHandler,vararg val cfgs: CachableRestConfig)  {
    private val conn = client.connect().apply {
        timeout = Duration.ofSeconds(30)
        if (isLocalOrTest) {
            sync().configSet("notify-keyspace-events", "Exd")
        }
    }

    @WithSpan
    fun delete(id: String,cache: CachableConfig) =
        conn.sync().del(handler.tilNøkkel(cache, id))


    fun cacheStørrelser() =
        cfgs.associate {
            it.navn to "${cacheStørrelse(it.navn).toLong()} innslag, ttl: ${it.varighet.format()}"
        }
    fun cacheStørrelse(cache: String) =
        adapter.størrelse(cache).toDouble()
}

data class CachableConfig(val name: String, val extraPrefix: String? = null)