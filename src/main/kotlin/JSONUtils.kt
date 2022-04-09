import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import opennlp.tools.util.Span

class JSONUtils {
    companion object {
        public val mapper: ObjectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(SimpleModule().
        addDeserializer(Span::class.java, SpanDeserializer(Span::class.java)))
    }
}

fun Any.toJSONString(): String? {
    return JSONUtils.mapper.writeValueAsString(this);
}