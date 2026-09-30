package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CsvWebMvcConfig : WebMvcConfigurer {

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.mediaType("csv", MediaType("text", "csv"))
    }

    override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
        builder.addCustomConverter(CsvHttpMessageConverter())
    }
}
