package no.nav.sikkerhetstjenesten.entraproxy.graph.exceptions

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId

class EntraIkkeFunnetException(
    val ansattId: AnsattId,
    msg: String = "Fant ingen oid for $ansattId i Entra",
    cause: Throwable? = null) : RuntimeException(msg, cause)
