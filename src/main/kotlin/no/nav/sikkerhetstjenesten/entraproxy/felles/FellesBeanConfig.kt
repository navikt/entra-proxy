package no.nav.sikkerhetstjenesten.entraproxy.felles

import io.micrometer.core.instrument.MeterRegistry
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.felles.rest.ConsumerAwareHandlerInterceptor
import no.nav.sikkerhetstjenesten.felles.rest.DefaultRestErrorHandler
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import org.apache.hc.core5.util.TimeValue
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION


@Configuration
class FellesBeanConfig(private val ctx: AuthContext,
                         private val meterRegistry: MeterRegistry,) : WebMvcConfigurer {


    @Bean
    fun handler() = DefaultRestErrorHandler()

    @Bean
    fun jackson3Customizer() = JsonMapperBuilderCustomizer {
        it.enable(INCLUDE_SOURCE_IN_LOCATION)
    }

    @Bean
    fun httpComponentsBuilderCustomizer():
            ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder> =
        ClientHttpRequestFactoryBuilderCustomizer { builder ->
            builder
                .withConnectionManagerCustomizer { cm ->
                    cm.setMaxConnTotal(300)
                    cm.setMaxConnPerRoute(50)
                }
                .withConnectionConfigCustomizer { cfg ->
                    cfg.setValidateAfterInactivity(TimeValue.ofSeconds(2))
                }
        }


    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(ConsumerAwareHandlerInterceptor(ctx, meterRegistry))
    }
    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(APPLICATION_JSON)
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToEnhetnummerConverter())
    }
    companion object {
        fun headerAddingRequestInterceptor(vararg verdier: Pair<String, String>) =
            ClientHttpRequestInterceptor { request, body, next ->
                verdier.forEach { (key, value) -> request.headers.add(key, value) }
                next.execute(request, body)
            }

    }
    private class StringToEnhetnummerConverter : Converter<String, Enhetnummer> {
        override fun convert(source: String) = Enhetnummer(source)
    }
}



