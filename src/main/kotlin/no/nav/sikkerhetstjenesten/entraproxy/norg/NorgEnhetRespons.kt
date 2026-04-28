package no.nav.sikkerhetstjenesten.entraproxy.norg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NorgEnhetRespons(val enhetNr: Int,val navn: String)