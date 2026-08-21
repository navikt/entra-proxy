package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheSizeAware
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.current
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.local
import org.springframework.boot.SpringBootVersion
import org.springframework.boot.actuate.info.Info.Builder
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.core.SpringVersion
import org.springframework.stereotype.Component

@Component
@Lazy
class StartupInfoContributor(private val caches : CacheSizeAware, private val ctx: ConfigurableApplicationContext) :
    InfoContributor {

    override fun contribute(builder: Builder) {
        builder.withDetail("startup", ctx.startupDate.local())
        builder.withDetail("cache størrelser", caches.sizes())
        }
}

