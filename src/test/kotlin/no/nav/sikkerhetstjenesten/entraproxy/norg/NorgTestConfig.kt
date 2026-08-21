package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheTestConfig
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.resilience.annotation.EnableResilientMethods

@EnableCaching
@EnableResilientMethods
class NorgTestConfig : CacheTestConfig(NORG) {
    @Bean
    fun conversionService() = ApplicationConversionService.getSharedInstance()
}
