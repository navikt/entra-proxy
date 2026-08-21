package no.nav.sikkerhetstjenesten.entraproxy.felles

import com.nimbusds.jwt.SignedJWT
import no.nav.boot.conditionals.ConditionalOnNotProd
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType
import org.springframework.stereotype.Component
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpLogFormatter
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.HttpResponse
import org.zalando.logbook.Logbook
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.Sink
import org.zalando.logbook.Strategy
import org.zalando.logbook.attributes.AttributeExtractor
import org.zalando.logbook.attributes.HttpAttributes
import org.zalando.logbook.core.Conditions
import org.zalando.logbook.core.DefaultHttpLogWriter
import org.zalando.logbook.core.DefaultSink
import org.zalando.logbook.core.StatusAtLeastStrategy
import org.zalando.logbook.json.JsonHttpLogFormatter
import tools.jackson.databind.json.JsonMapper

@Configuration
@ConditionalOnNotProd
@NoCoverageAnalysis
class LogbookBeanConfiguration {

    @Bean
    fun logbook(formatter: PrettyPrintingLogbookFormatter, jwtClaimsExtractor: AttributeExtractor) =
        Logbook.builder()
            .strategy(StatusAtLeastExcluding(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND))
            .bodyFilter { _, body ->
                BRUKER_ID_REGEX.replace(body, "<brukerId>")
            }
            .condition(
                Conditions.exclude(
                    Conditions.requestTo("**/internal/**"),
                    Conditions.requestTo("**/monitoring/**"),
                    Conditions.requestTo("**/actuator/**"),
                    Conditions.requestTo("https://graph.microsoft.com/v1.0/organization"),
                ),
            )
            .attributeExtractor(jwtClaimsExtractor)
            .sink(DefaultSink(formatter, DefaultHttpLogWriter()))
            .build()

    @Component
    class PrettyPrintingLogbookFormatter(private val mapper: JsonMapper) : HttpLogFormatter {
        private val delegate = JsonHttpLogFormatter(mapper, true)

        override fun format(precorrelation: Precorrelation, request: HttpRequest) =
            prettyPrint(delegate.format(precorrelation, request))

        override fun format(correlation: Correlation, response: HttpResponse) =
            prettyPrint(delegate.format(correlation, response))

        private fun prettyPrint(raw: String) =
            runCatching {
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(raw))
            }.getOrDefault(raw)
    }

    @Component
    class NimbusJwtClaimsExtractor : AttributeExtractor {

        override fun extract(request: HttpRequest): HttpAttributes {
            val auth = request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return HttpAttributes.EMPTY
            return HttpAttributes(SignedJWT.parse(auth.removePrefix(TokenType.BEARER.value) + " ").jwtClaimsSet.claims.withTimestampsInCurrentTimezone())
        }
    }
}

class StatusAtLeastExcluding(atLeast: HttpStatus, private vararg val excludedStatus: HttpStatus) : Strategy {
   private val delegate = StatusAtLeastStrategy(atLeast.value())

   override fun write(precorrelation: Precorrelation, request: HttpRequest, sink: Sink) {
       if (!request.shouldIgnoreGraphQlIntrospectionQuery()) {
           delegate.write(precorrelation, request, sink)
       }
   }

   override fun write(correlation: Correlation, req: HttpRequest, res: HttpResponse, sink: Sink) {
       if (!req.shouldIgnoreGraphQlIntrospectionQuery() && res.status !in (excludedStatus.map { it.value() })) {
           delegate.write(correlation, req, res, sink)
       }
   }
}