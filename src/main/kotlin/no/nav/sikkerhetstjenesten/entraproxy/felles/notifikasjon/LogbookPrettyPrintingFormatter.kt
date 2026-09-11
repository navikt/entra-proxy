package no.nav.sikkerhetstjenesten.entraproxy.felles.notifikasjon

import org.springframework.stereotype.Component
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpLogFormatter
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.HttpResponse
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.json.JsonHttpLogFormatter
import tools.jackson.databind.json.JsonMapper

@Component
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