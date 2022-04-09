package example

import JSONUtils
import com.fasterxml.jackson.module.kotlin.readValue
import getResource
import nlp.Sentence
import opennlp.tools.util.Span

/**
 * TODO
 */
private fun getNamesFromJasmineKennedieFiles() {
    var jasmineKennedieText = getResource("jasmineKennedieFile.json").readText();
    val jasmineKennedieAnalysis: List<Sentence> = JSONUtils.mapper.readValue(jasmineKennedieText);
//    val namesFound: List<Array<Span>> = jasmineKennedieAnalysis.map { ENGLISH_NAME_FINDER.find(it.tokensAsStringArray) }
//    writeToResourceFile("jasmineKennedieNamesFound", namesFound.toJSONString());
    val jasmineKennedieNamesFound = getResource("jasmineKennedieNamesFound").readText();
    val jasmineKennedieNameSpans: List<Array<Span>> = JSONUtils.mapper.readValue(jasmineKennedieNamesFound);

    val namesFound = mutableListOf<String>();
    for ((index, arraySpan) in jasmineKennedieNameSpans.withIndex()) {
        for ((index2, span: Span) in arraySpan.withIndex()) {
            println("${span.start} to ${span.end}");
            val message = jasmineKennedieAnalysis[index]
            val slice = message.tokensAsStringArray!!.slice(IntRange(span.start, span.end - 1))
            var nameString =
                slice.joinToString(separator = " ");
            namesFound.add(nameString)
        }
    }
}