package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import no.nav.sikkerhetstjenesten.entraproxy.felles.leder.LeaderAware
import org.springframework.data.redis.annotation.RedisListener
import org.springframework.stereotype.Component

private const val CHANNEL_EXPIRED = "__keyevent@0__:expired"
private const val CHANNEL_DELETED = "__keyevent@0__:del"

@Component
class ValkeyEventListeningCacheOppfrisker(erLeder: Boolean = true,
                                          private vararg val oppfriskere: CacheOppfrisker) : LeaderAware(erLeder) {

    @RedisListener(CHANNEL_EXPIRED)
    @RedisListener(CHANNEL_DELETED)
    fun onEvent(nokkel: CacheNøkkel) {

        somLeder {
            oppfriskere.firstOrNull { it.cacheName == nokkel.cacheName }?.run {
                oppfrisk(nokkel)
            }
        }
    }

}
