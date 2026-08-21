package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import no.nav.sikkerhetstjenesten.entraproxy.felles.NoCoverageAnalysis
import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator


class PingableHealthIndicator(private val pingable: Pingable) : HealthIndicator {

    private val log = getLogger(javaClass)

    override fun health() =
        runCatching {
            pingable.ping()
            up()
        }.getOrElse {
            log.warn("Kunne ikke pinge ${pingable.pingEndpoint}", it)
            down(it)
        }

    private fun up() =
        with(pingable) {
            Health.up()
                .withDetail(ENDPOINT, pingEndpoint)
                .build()
        }

    private fun down(e: Throwable) =
        with(pingable) {
            Health.down()
                .withDetail(ENDPOINT, pingEndpoint)
                .withException(e)
                .build()
        }

    @NoCoverageAnalysis
    override fun toString() = "${javaClass.simpleName} [pingable=$pingable]"

    companion object {
        const val ENDPOINT = "endpoint"

        operator fun invoke(cfg: RestConfig, ping: () -> Any?) =
            PingableHealthIndicator(object : Pingable {
                override val name = cfg.name
                override val pingEndpoint = cfg.pingEndpoint
                override fun ping() = ping()
            })
    }
}

