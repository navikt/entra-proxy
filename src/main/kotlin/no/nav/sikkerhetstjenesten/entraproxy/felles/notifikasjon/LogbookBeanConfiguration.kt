package no.nav.sikkerhetstjenesten.entraproxy.felles.notifikasjon

import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.sikkerhetstjenesten.entraproxy.felles.NoCoverageAnalysis
import org.zalando.logbook.Logbook
import org.zalando.logbook.attributes.AttributeExtractor
import org.zalando.logbook.core.Conditions.exclude
import org.zalando.logbook.core.Conditions.requestTo
import org.zalando.logbook.core.DefaultHttpLogWriter
import org.zalando.logbook.core.DefaultSink
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.HttpLogFormatter

@Configuration
@NoCoverageAnalysis
@ConditionalOnNotProd
class LogbookBeanConfiguration {

    @Bean
    fun logbook(formatter: HttpLogFormatter, jwtClaimsExtractor: AttributeExtractor) =
        Logbook.builder()
            .condition(
                exclude(
                    requestTo("**/internal/**"),
                    requestTo("**/monitoring/**"),
                    requestTo("**/actuator/**"),
                    requestTo("https://graph.microsoft.com/v1.0/organization"),
                ),
            )
            .attributeExtractor(jwtClaimsExtractor)
            .sink(DefaultSink(formatter, DefaultHttpLogWriter()))
            .build()

}