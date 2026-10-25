package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

interface NorgOperations {
    fun navn(enhet: Enhetnummer): String
}
