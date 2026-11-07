package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.API_V1
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraMailTjeneste
import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "MailController", description = "Disse endepunktene brukes normalt ikke direkte")
@Hidden
@RestController
@RequestMapping(API_V1)
class EntraMailController(private val mail: EntraMailTjeneste) {

    @PostMapping("mail")
    @Operation(summary = "Send e-post")
    fun sendMail(@RequestBody message: MailRequest) =
        mail.send(message)
}
