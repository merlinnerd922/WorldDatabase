@file:Suppress("unused")

package exe.program

import com.fasterxml.jackson.module.kotlin.readValue
import nlp.SpanDeserializer
import nlp.getNamesFromSentenceAndSpanInfo
import opennlp.tools.util.Span
import utils.JSONUtils
import utils.getResourceText
import utils.readValueToNestedList

/**
Given a cached list of sentences (stored as the JSON file (jasmineKennedieFile.json)) and a cached nested list
of spans (also stored as a JSON file; it's named "jasmineKennedieNamesFound.json"), use the nested list of spans
to determine
where all mentioned names are within the given sentences, and print out that list.
 */
@Suppress("UNUSED_VARIABLE")
public fun getNamesFromJasmineKennedieFiles() {
    val namesFound = getNamesFromSentenceAndSpanInfo(

        // Provide the list of sentences from a resource file.
        sentenceList = JSONUtils.mapper.readValue(content = getResourceText("jasmineKennedieFile.json")),

        // Extract the list of spans from the given resource file, and use a special deserializer for Spans since
        // the Span class doesn't have an empty constructor.
        listOfSpans = JSONUtils.mapper.readValueToNestedList<Span>(
            stringToDeserialize = getResourceText("jasmineKennedieNamesFound.json"),
            deserializerToAdd = SpanDeserializer(),
        )
    )
    println(namesFound);
}

