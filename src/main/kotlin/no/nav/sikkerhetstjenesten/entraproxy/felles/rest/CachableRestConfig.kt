package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import java.time.Duration

interface CachableRestConfig {
    val varighet: Duration
    val navn: String
}