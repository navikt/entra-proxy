package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class NorgClientBeanConfig {

    @Bean
    fun norgHealthIndicator(cfg: NorgConfig, client: NorgProxyClient) =
        PingableHealthIndicator(cfg, client::ping)
}
