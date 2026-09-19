package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import tools.jackson.databind.DatabindContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.jsontype.PolymorphicTypeValidator
import tools.jackson.databind.jsontype.PolymorphicTypeValidator.Validity.ALLOWED
import tools.jackson.databind.jsontype.PolymorphicTypeValidator.Validity.DENIED

class NavPolymorphicTypeValidator(private vararg val allowedPrefixes: String = arrayOf("no.nav.sikkerhetstjenesten",
    "java.",
    "kotlin.")) : PolymorphicTypeValidator() {

    override fun validateBaseType(ctx: DatabindContext, base: JavaType) =
        validityFor(base.rawClass.name)

    override fun validateSubClassName(ctx: DatabindContext, base: JavaType, subClassName: String) =
        validityFor(subClassName)

    override fun validateSubType(ctx: DatabindContext, base: JavaType, subType: JavaType) =
        validityFor(subType.rawClass.name)

    private fun validityFor(className: String) =
        if (allowedPrefixes.any(className::startsWith)) ALLOWED else DENIED
}