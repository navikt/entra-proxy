package no.nav.sikkerhetstjenesten.entraproxy.graph

class EntraIkkeFunnetException(
    val ansattId: AnsattId,
    msg: String = "Fant ingen oid for $ansattId i Entra",
    cause: Throwable? = null,
) : RuntimeException(msg, cause)
