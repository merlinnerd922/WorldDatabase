@file:Suppress("unused")

package utils

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.gson.Gson
import kotlinx.serialization.*
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.Json.Default.encodeToString
import nlp.opennlp.chunk.SpanDeserializer
import opennlp.tools.util.Span

/**
 * A class of JSON-parsing related utilities.
 */
class JSONUtils {
    companion object {

        /**
         * An object mapper that makes use of Kotlin to dynamically perform certain functionalities.
         */
        public val mapper: ObjectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(SimpleModule().addDeserializer(
            Span::class.java, SpanDeserializer()
        ))
    }
}

/**
 * Convert the provided object (this) into a JSON representation of an object.
 */
inline fun <reified T> T.toJSONString(): String? {
//    return Json.encodeToString(this);
    return Gson().toJson(this);
//    return JSONUtils.mapper.writeValueAsString(this);
}

/**
 * Read the provided string into a nested list of type <T>.
 */
public inline fun <reified T> ObjectMapper.readValueToNestedList(
    stringToDeserialize: String,
    deserializerToAdd: JsonDeserializer<T>? = null,
): List<List<T>> {

    // Register a custom deserializer, if provided.
    if (deserializerToAdd != null) {
        this.registerModule(SimpleModule().addDeserializer(T::class.java, deserializerToAdd))
    }

    // Use the object mapper to convert the contents of the read array to a nested list of type T.
    val readValue: Array<Array<Any>>? = this.readValue(stringToDeserialize, Array<Array<Any>>::class.java);
    return readValue!!.map { it -> it.map  { this.convertValue(it, T::class.java)}};

}