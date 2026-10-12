package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

interface NorgPort {
    fun navn(enhet: Enhetnummer): String
}
