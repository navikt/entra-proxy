package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS
import tools.jackson.core.Version.unknownVersion
import tools.jackson.databind.AnnotationIntrospector
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder
import tools.jackson.databind.module.SimpleModule

/**
Dette er en modul for Jackson 3 Json-serialisering som legger til en egendefinert AnnotationIntrospector. Denne introspektoren styrer hvordan typeinformasjon håndteres ved serialisering, slik at objekter får med seg typeinformasjon i JSON-feltet @class.
 */
class CacheTypeInfoAddingJacksonModule : SimpleModule() {
    override fun setupModule(ctx: SetupContext) {
        ctx.insertAnnotationIntrospector(object : AnnotationIntrospector() {
            override fun findTypeResolverBuilder(config: MapperConfig<*>, ann: Annotated) =
                StdTypeResolverBuilder().init(
                    JsonTypeInfo.Value.construct(CLASS,
                        PROPERTY,
                        "@class",
                        null,
                        true,
                        true), null)
            override fun version() = unknownVersion()
        })
    }
}