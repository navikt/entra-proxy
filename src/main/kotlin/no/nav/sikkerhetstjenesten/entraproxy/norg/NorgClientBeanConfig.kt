package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig.Companion.createClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient.Builder
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler


@Configuration
class NorgClientBeanConfig {

    @Bean
    fun norgProxyClient( cfg: NorgConfig,b: Builder, errorHandler: ErrorHandler) =
        createClient<NorgProxyClient>(cfg, b, errorHandler)
    @Bean
    fun norgHealthIndicator(pingable: NorgPingable) =
        PingableHealthIndicator(pingable)
    
}

@Component
class NorgPingable(private val client: NorgProxyClient, cfg: NorgConfig) : Pingable {
    override fun ping() = client.ping()
    override val pingEndpoint = cfg.pingEndpoint.toString()
    override val name = NORG
}
