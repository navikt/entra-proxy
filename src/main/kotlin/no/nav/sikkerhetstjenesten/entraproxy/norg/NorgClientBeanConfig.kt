package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig.Companion.createClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient.Builder
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler

@Configuration
class NorgClientBeanConfig {

    @Bean
    fun norgProxyClient( cfg: NorgConfig,b: Builder, errorHandler: ErrorHandler) =
        createClient<NorgProxyClient>(cfg, b, errorHandler)

    @Bean
    fun norgHealthIndicator(client: NorgTjeneste) =  PingableHealthIndicator(client)
}

