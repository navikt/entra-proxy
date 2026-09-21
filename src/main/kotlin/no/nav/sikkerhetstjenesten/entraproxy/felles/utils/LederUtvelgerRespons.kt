package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LederUtvelgerRespons(val name: String)