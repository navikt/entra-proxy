package no.nav.sikkerhetstjenesten.entraproxy.felles

import io.swagger.v3.oas.models.Operation
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.Locale

@Configuration
class OpenApiMessageResolverConfig(private val messageSource: MessageSource) {

    @Bean
    fun openApiMessageCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.tags?.forEach { tag ->
            tag.description = resolve(tag.description)
        }

        openApi.paths?.values?.forEach { pathItem ->
            pathItem.readOperations().forEach { operation ->
                resolveOperation(operation)
            }
        }
    }

    private fun resolveOperation(operation: Operation) {
        operation.summary = resolve(operation.summary)
        operation.description = resolve(operation.description)
    }

    private fun resolve(text: String?): String? {
        if (text.isNullOrBlank() || !text.startsWith(MSG_PREFIX)) return text
        val key = text.removePrefix(MSG_PREFIX)
        return messageSource.getMessage(key, null, key, Locale.getDefault())
    }

    private companion object {
        const val MSG_PREFIX = "msg:"
    }
}