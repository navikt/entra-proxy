package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOperations
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import  no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableRestConfig


@Component
@Lazy
class CacheSizeAware(private val cache: CacheOperations, private vararg val cfgs: CachableRestConfig) {
    fun sizes() = cache.sizes(*cfgs.flatMap { it.caches }.toTypedArray())
}