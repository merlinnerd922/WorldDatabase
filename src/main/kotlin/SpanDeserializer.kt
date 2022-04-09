import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import opennlp.tools.util.Span

class SpanDeserializer(@Suppress("unused") private val spanClass: Class<Span>) : JsonDeserializer<Span>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Span {
        val mapper = p!!.codec as ObjectMapper
        val node: JsonNode = mapper.readTree(p)
        return Span(node.get("start").asInt(),
            node.get("end").asInt(),
            node.get("type").asText(),
            node.get("prob").asDouble());

    }

}
