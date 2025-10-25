package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient.Builder

@Configuration
class NorgClientBeanConfig {

    @Bean
    @Qualifier(NORG)
    fun norgRestClient(b: Builder, cfg: NorgConfig) =
        b.baseUrl(cfg.baseUri).build()

    @Bean
    fun norgHealthIndicator(a: NorgRestClientAdapter) =  PingableHealthIndicator(a)
}