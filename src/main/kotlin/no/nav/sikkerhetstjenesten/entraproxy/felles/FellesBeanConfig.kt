package no.nav.sikkerhetstjenesten.entraproxy.felles

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.swagger.v3.oas.models.media.Schema
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.ConsumerAwareHandlerInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenTypeTellendeRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.OSLO
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.apache.hc.core5.util.TimeValue
import org.slf4j.LoggerFactory
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.actuate.endpoint.SanitizingFunction
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import java.util.function.Function
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION


@Configuration
class FellesBeanConfig(private val ansattIdAddingInterceptor: ConsumerAwareHandlerInterceptor,
                       private val handler: ErrorHandler) : WebMvcConfigurer {


    @Bean
    fun jackson3Customizer() = JsonMapperBuilderCustomizer {
        it.enable(INCLUDE_SOURCE_IN_LOCATION)
    }


    @Bean
    fun restClientCustomizer(tokenInterceptor: TokenTypeTellendeRequestInterceptor, logbookInterceptor: /*ObjectProvider<*/LogbookClientHttpRequestInterceptor/*>*/) =
        RestClientCustomizer { c ->
            c.requestInterceptors {
               /* logbookInterceptor.ifAvailable {
                    interceptor -> it.add(interceptor)
                }*/
                it.addFirst(logbookInterceptor)
                it.add(tokenInterceptor)
            }
            c.defaultStatusHandler(HttpStatusCode::isError, handler::handle)
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



    @Bean
    fun sanitizingFunction() = SanitizingFunction { data ->
        if (SENSITIVE_KEYS.any { data.key.contains(it, ignoreCase = true) }) data.withValue("******") else data
    }

    @Bean
    fun clusterAddingTimedAspect(meterRegistry: MeterRegistry, token: Token) =
        TimedAspect(meterRegistry, Function { pjp -> Tags.of("cluster", token.cluster, "method", pjp.signature.name, "client", token.systemNavn) })


    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(ansattIdAddingInterceptor)
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

        private val SENSITIVE_KEYS = setOf("password", "secret", "token", "key","credentials", "jwk","private_key")

    }
    private class StringToEnhetnummerConverter : Converter<String, Enhetnummer> {
        override fun convert(source: String) = Enhetnummer(source)
    }
}

@Retention(BINARY)  // = CLASS in bytecode — enough for JaCoCo
@Target(FUNCTION, CONSTRUCTOR, CLASS)
annotation class Generated
typealias NoCoverageAnalysis = Generated


