package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.lettuce.core.RedisClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isLocalOrTest
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.format
import org.springframework.stereotype.Component
import java.time.Duration


@Component
class CacheClient(client: RedisClient,private val teller : CacheKeyCounter,private vararg val cfgs: CachableRestConfig)  {
    init {
        client.connect().apply {
            timeout = Duration.ofSeconds(30)
            if (isLocalOrTest) {
                sync().configSet("notify-keyspace-events", "Exd")
            }
        }
    }
    fun cacheStørrelser() =
        cfgs.associate {
            it.navn to "${cacheStørrelse(it.navn).toLong()} innslag, ttl: ${it.varighet.format()}"
        }

    private fun cacheStørrelse(prefix: String) =
        teller.count(prefix).toDouble()
}
