package no.nav.sikkerhetstjenesten.entraproxy.felles

import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.MediaType.TEXT_HTML_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
@Order(HIGHEST_PRECEDENCE)
class PageNotFoundHandler {

    @ExceptionHandler(NoResourceFoundException::class, produces = [TEXT_HTML_VALUE])
    fun notFound(): ResponseEntity<ClassPathResource> =
        ResponseEntity.status(NOT_FOUND)
            .contentType(TEXT_HTML)
            .body(ClassPathResource("error-pages/404.html"))
}
