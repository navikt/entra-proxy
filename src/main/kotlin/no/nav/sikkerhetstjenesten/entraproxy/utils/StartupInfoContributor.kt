package no.nav.sikkerhetstjenesten.entraproxy.utils

import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.felles.utils.extensions.TimeExtensions.local
import org.springframework.boot.actuate.info.Info.Builder
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
@Lazy
class StartupInfoContributor(private val cache: CacheOperations, private val ctx: ConfigurableApplicationContext) :
    InfoContributor {

    override fun contribute(builder: Builder) {
        builder.withDetail("startup", ctx.startupDate.local())
        builder.withDetail("cache størrelser", cache.sizes())
    }
}

