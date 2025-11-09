package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import java.net.URI

abstract class AbstractRestClientAdapter(
    protected val restClient: RestClient,
    val cfg: AbstractRestConfig,
    protected val errorHandler: ErrorHandler = DefaultRestErrorHandler()) : Pingable {

    protected val log = getLogger(javaClass)
    override fun ping() = if (cfg.isEnabled) get<Any?>(cfg.pingEndpoint) else "disabled"

    protected inline fun <reified T> get(uri: URI, headers: Map<String, String> = emptyMap()) =
        restClient.get()
            .uri(uri)
            .accept(APPLICATION_JSON)
            .headers { it.setAll(headers) }
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler::handle)
            .body(T::class.java) ?: throw IrrecoverableRestException(INTERNAL_SERVER_ERROR, uri)

    override val name = cfg.name

    override val pingEndpoint = "${cfg.pingEndpoint}"
    override val isEnabled = cfg.isEnabled
    override fun toString() = "restClient=$restClient, cfg=$cfg, baseUri=${cfg.baseUri}"

}