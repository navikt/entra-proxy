package no.nav.sikkerhetstjenesten.entraproxy.graph.exceptions

import java.util.UUID

class GrupperIkkeFunnetException(val gruppeIder: Set<UUID>) :
    RuntimeException("Fant ikke følgende grupper i Entra: ${gruppeIder.joinToString()}")
