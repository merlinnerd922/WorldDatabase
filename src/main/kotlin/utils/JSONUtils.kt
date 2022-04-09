@file:Suppress("unused")

package utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import nlp.SpanDeserializer
import opennlp.tools.util.Span

/**
 * TODO
 */
class JSONUtils {
    companion object {
        public val mapper: ObjectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(

            SimpleModule().
        addDeserializer(Span::class.java, SpanDeserializer(Span::class.java)))
    }
}

/**
 * Convert the provided object (this) into a JSON representation of an object.
 */
fun Any.toJSONString(): String? {
    return JSONUtils.mapper.writeValueAsString(this);
}