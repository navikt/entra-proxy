package no.nav.sikkerhetstjenesten.entraproxy.felles

import no.nav.boot.conditionals.ConditionalOnNotProd
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpLogFormatter
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.HttpResponse
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.Sink
import org.zalando.logbook.Strategy
import org.zalando.logbook.core.StatusAtLeastStrategy
import org.zalando.logbook.json.JsonHttpLogFormatter
import tools.jackson.databind.json.JsonMapper

@Configuration
@NoCoverageAnalysis
@ConditionalOnNotProd
class LogbookBeanConfiguration {

    class LogbookStatusAtLeastExcluding(atLeast: HttpStatus, private vararg val excludedStatus: HttpStatus) : Strategy {
        private val delegate = StatusAtLeastStrategy(atLeast.value())

        override fun write(precorrelation: Precorrelation, request: HttpRequest, sink: Sink) {
            delegate.write(precorrelation, request, sink)
        }

        override fun write(correlation: Correlation, req: HttpRequest, res: HttpResponse, sink: Sink) {
            if ( res.status !in (excludedStatus.map { it.value() })) {
                delegate.write(correlation, req, res, sink)
            }
        }
    }

    class LogbookPrettyPrintingFormatter(private val mapper: JsonMapper) : HttpLogFormatter {
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

}