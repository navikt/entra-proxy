package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import io.micrometer.core.annotation.Timed
import no.nav.boot.conditionals.ConditionalOnGCP
import org.springframework.stereotype.Service

@Service
@Timed
@ConditionalOnGCP
class AnsattTjeneste(private val resolver: AnsattGruppeResolver) {

    fun ansatt(ansattId: AnsattId) =
        Ansatt(ansattId, ansattGrupper(ansattId))

    private fun ansattGrupper(ansattId: AnsattId) = resolver.grupperForAnsatt(ansattId)

}





