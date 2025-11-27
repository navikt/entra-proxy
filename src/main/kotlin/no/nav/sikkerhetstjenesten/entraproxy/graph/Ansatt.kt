package no.nav.sikkerhetstjenesten.entraproxy.graph

import java.util.UUID

open class Ansatt(
                  val navIdent: AnsattId,
                  val navn: String, val fornavn: String,
                  val etternavn: String): Comparable<Ansatt> {


    override fun compareTo(other: Ansatt): Int = etternavn.compareTo(other.etternavn)

}

class UtvidetAnsatt(navIdent: AnsattId,  navn: String,
                    fornavn: String, etternavn: String, val tIdent: TIdent, val epost: String, val enhet: Enhet) : Ansatt(navIdent,navn,fornavn,etternavn)
