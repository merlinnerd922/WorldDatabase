package nlp.opennlp.chunk

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import opennlp.tools.util.Span

/**
 * A deserializer that exists for the purpose of deserializing spans, since the Span class does NOT have an empty
 * constructor.
 */
class SpanDeserializer : JsonDeserializer<Span>() {

    /**
     * Deserialize the topmost element within the provided parser object into a Span object.
     */
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Span {
        val mapper = p!!.codec as ObjectMapper
        val node: JsonNode = mapper.readTree(p)
        return Span(
            node.get("start").asInt(),
            node.get("end").asInt(),
            node.get("type").asText(),
            node.get("prob").asDouble()
        );

    }

}
