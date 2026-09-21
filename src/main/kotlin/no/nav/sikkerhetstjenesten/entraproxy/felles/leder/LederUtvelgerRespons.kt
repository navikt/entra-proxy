package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LederUtvelgerRespons(val name: String)