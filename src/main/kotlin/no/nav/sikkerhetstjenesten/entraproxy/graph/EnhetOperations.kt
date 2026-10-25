package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

interface EnhetOperations {
    fun navn(enhetnummer: Enhetnummer): String
}
