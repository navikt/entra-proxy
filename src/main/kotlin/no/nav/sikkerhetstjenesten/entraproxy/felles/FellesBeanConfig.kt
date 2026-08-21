package no.nav.sikkerhetstjenesten.entraproxy.felles

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.springdoc.core.customizers.OpenApiCustomizer
import io.swagger.v3.oas.models.media.Schema
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.ConsumerAwareHandlerInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.security.OSLO
import org.apache.hc.core5.util.Timeout.ofSeconds
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
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
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import java.util.Date
import java.util.function.Function
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION


@Configuration
class FellesBeanConfig(private val ansattIdAddingInterceptor: ConsumerAwareHandlerInterceptor,
                       private val handler: ErrorHandler,
                       private val logbookInterceptor: ObjectProvider<LogbookClientHttpRequestInterceptor>)
    : WebMvcConfigurer {


    @Bean
    fun jackson3Customizer() = JsonMapperBuilderCustomizer {
        it.addMixIn(OAuth2AccessTokenResponse::class.java, IgnoreUnknownMixin::class.java)
        it.enable(INCLUDE_SOURCE_IN_LOCATION)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private interface IgnoreUnknownMixin


    @Bean
    fun restClientCustomizer() =
        RestClientCustomizer { c ->
            c.requestInterceptors {
                logbookInterceptor.ifAvailable { interceptor -> it.add(interceptor) }
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
                    cfg.setValidateAfterInactivity(2.sekunder)
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

    @Aspect
    @Component
    class TimingAspect(private val meterRegistry: MeterRegistry) {

        @Around("execution(* org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor.intercept(..))")
        fun timeMethod(joinPoint: ProceedingJoinPoint) = Timer.builder("mslogin")
            .description("Timer med histogram for mslogin")
            .tags("method", joinPoint.signature.name)
            .publishPercentileHistogram()
            .register(meterRegistry).recordCallable { joinPoint.proceed() }
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
    class StringToEnhetnummerConverter : Converter<String, Enhetnummer> {
        override fun convert(source: String): Enhetnummer = Enhetnummer(source)
    }

    @Bean
    fun openApiCustomiser(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        val schemas = openApi.components.schemas
        schemas["Enhetnummer"] = Schema<Enhetnummer>().apply {
            type = "string"
            description = "Enhetnummer (4 siffer)"
            example = Enhetnummer("1234")
        }
        schemas["Enhet"] = Schema<Enhet>().apply {
            type = "object"
            description = "Enhetnummer (4 siffer) og navn"
            example = Enhet(Enhetnummer("1234"),"Nav Avdeling Sydpolen")
        }
        schemas["Ansatt"] = Schema<Ansatt>().apply {
            type = "string"
            description = "Navn og ident for en ansatt"
            example = Ansatt(AnsattId("A123456"), "Tore Tang", "Tore", "Tang")
        }
        schemas["NavIdent"] = Schema<Ansatt>().apply {
            type = "string"
            description = "NavIdent (7 siffer)"
            example = AnsattId("A123456")
        }
        schemas["Tema"] = Schema<Tema>().apply {
            type = "string"
            description = "Tema (3 store bokstaver)"
            example = Tema("AAP")
        }
    }
}


@Retention(BINARY)  // = CLASS in bytecode — enough for JaCoCo
@Target(FUNCTION, CONSTRUCTOR, CLASS)
annotation class Generated
typealias NoCoverageAnalysis = Generated

val BRUKER_ID_REGEX = Regex("""(?<!\d)\d{11}(?!\d)""")

internal fun Map<String, Any>.withTimestampsInCurrentTimezone() =
    mapValues {
            (_, value) -> (value as? Date)?.toInstant()?.atZone(OSLO) ?: value
    }

internal fun String.shouldIgnoreGraphQlIntrospectionQuery() =
    GRAPHQL_INTROSPECTION_QUERY_BODY.matches(this)

internal fun HttpRequest.shouldIgnoreGraphQlIntrospectionQuery() =
    getBodyAsString().shouldIgnoreGraphQlIntrospectionQuery()

private val GRAPHQL_INTROSPECTION_QUERY_BODY =
    Regex("""(?s)^\s*\{\s*"query"\s*:\s*"\{\s*__typename\s*}"\s*}\s*$""")

val Int.sekunder get() = ofSeconds(toLong())
