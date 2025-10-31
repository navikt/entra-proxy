package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import org.springframework.stereotype.Component
import java.time.Duration

interface CachableRestConfig {
    val varighet: Duration get() = Duration.ofHours(12)
    val navn: String
}

@Component
class MedlemmerCachableRestConfig : CachableRestConfig {
    override val varighet = Duration.ofHours(12)
    override val navn = "medlemmer"
}