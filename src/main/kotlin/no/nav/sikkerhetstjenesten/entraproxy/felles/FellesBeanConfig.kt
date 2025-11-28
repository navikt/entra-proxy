package no.nav.sikkerhetstjenesten.entraproxy.felles

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.springdoc.core.customizers.OpenApiCustomizer
import io.swagger.v3.oas.models.media.Schema
import jakarta.servlet.http.HttpServletRequest
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.spring.oauth2.OAuth2ClientRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelTeller
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.ConsumerAwareHandlerInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.MedlemmerCachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.MedlemmerCachableRestConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenTypeTellendeRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository
import org.springframework.boot.actuate.web.exchanges.Include.defaultIncludes
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.boot.servlet.actuate.web.exchanges.HttpExchangesFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import java.util.function.Function
import kotlin.text.toDouble


@Configuration
class FellesBeanConfig(private val ansattIdAddingInterceptor: ConsumerAwareHandlerInterceptor) : WebMvcConfigurer {


    @Bean
    fun jackson3Customizer() = JsonMapperBuilderCustomizer {
        it.addMixIn(OAuth2AccessTokenResponse::class.java, IgnoreUnknownMixin::class.java)
        it.enable(INCLUDE_SOURCE_IN_LOCATION)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private interface IgnoreUnknownMixin

    @Bean
    fun restClientCustomizer(interceptor: OAuth2ClientRequestInterceptor, tokenInterceptor: TokenTypeTellendeRequestInterceptor) =
        RestClientCustomizer { c ->
            c.requestFactory(HttpComponentsClientHttpRequestFactory().apply {
                setConnectionRequestTimeout(2000)
                setReadTimeout(2000)
            })
            c.requestInterceptors {
                it.addFirst(interceptor)
                it.add(tokenInterceptor)
            }
        }

    @Bean
    fun clusterAddingTimedAspect(meterRegistry: MeterRegistry, token: Token) =
        TimedAspect(meterRegistry, Function { pjp -> Tags.of("cluster", token.cluster, "method", pjp.signature.name, "client", token.systemNavn) })

    @Bean
    @ConditionalOnNotProd
    fun traceRepository() = InMemoryHttpExchangeRepository()


    @Bean
    @ConditionalOnNotProd
    fun httpExchangesFilter(repository: HttpExchangeRepository) =
        object : HttpExchangesFilter(repository, defaultIncludes()) {
            override fun shouldNotFilter(request: HttpServletRequest) = request.servletPath.contains("monitoring")
        }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(ansattIdAddingInterceptor)
    }
    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(APPLICATION_JSON)
    }

    @Aspect
    @Component
    class TimingAspect(private val meterRegistry: MeterRegistry) {

        @Around("execution(* no.nav.security.token.support.client.spring.oauth2.OAuth2ClientRequestInterceptor.intercept(..))")
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


