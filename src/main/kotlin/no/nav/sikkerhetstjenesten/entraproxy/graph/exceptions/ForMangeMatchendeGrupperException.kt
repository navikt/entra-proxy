package no.nav.sikkerhetstjenesten.entraproxy.graph.exceptions

class ForMangeMatchendeGrupperException(
    val gruppenavnSubstring: String,
    val antall: Int,
    val maks: Int) : RuntimeException(
    "Søket '$gruppenavnSubstring' matcher $antall grupper, men maks er $maks. Gjør søket mer spesifikt.",
)
