package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.boot.conditionals.ConditionalOnNotProd
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.TEXT_HTML_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Serverer ansatt.html kun i milj\u00f8er som ikke er prod.
 * Filen ligger utenfor classpath:/static/ slik at den ikke
 * blir servert automatisk av Spring Boot sin standard
 * static-resource-handtering i prod.
 */
@ConditionalOnNotProd
@RestController
class AnsattSideController {

    @GetMapping("/ansatt.html", produces = [TEXT_HTML_VALUE])
    fun ansattSide() = ClassPathResource("dev-static/ansatt.html")
}
