package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppeTjeneste
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal

const val API_V1 = "/api/v1"

internal fun medlemmerIGruppe(oid: EntraOidTjeneste, gruppe: EntraGruppeTjeneste, gruppeNavn: String) =
    oid.gruppeOid(gruppeNavn)?.let {
        gruppe.medlemmerIGruppe(it)
    } ?: emptySet()

internal fun <R> OAuth2AuthenticatedPrincipal.requiredAttribute(attributeName: String, mapper: (String) -> R): R =
    mapper(requireNotNull(getAttribute(attributeName)) { "Mangler $attributeName i OBO-token" })
