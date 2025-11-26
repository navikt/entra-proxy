package no.nav.sikkerhetstjenesten.entraproxy.graph

import java.util.UUID

open class Ansatt(id: UUID,
                  val navIdent: AnsattId,
                  val navn: String, val fornavn: String,
                  val etternavn: String): Comparable<Ansatt> {


    override fun compareTo(other: Ansatt): Int = etternavn.compareTo(other.etternavn)

}

class UtvidetAnsatt(id: UUID, navIdent: AnsattId,  navn: String,
                    fornavn: String, etternavn: String, val tIdent: TIdent, val epost: String, val enhet: String) : Ansatt(id,navIdent,navn,fornavn,etternavn)
