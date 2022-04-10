@file:Suppress("unused")

package nlp.opennlp.chunk

import utils.getResource
import opennlp.tools.chunker.ChunkerME
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.namefind.NameFinderME
import opennlp.tools.namefind.TokenNameFinderModel
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import opennlp.tools.util.Span
import utils.resourceObj
import java.io.InputStream

fun getSentenceDetector(@Suppress("SameParameterValue") resourceName: String): SentenceDetectorME {
    val inputStream: InputStream = resourceObj.javaClass.getResourceAsStream(resourceName)!!
    val model = SentenceModel(inputStream)
    return SentenceDetectorME(model)
}


// TODO

/**
 * A sentence detector, meant to detect sentences written in English.
 */
public val ENGLISH_SENTENCE_DETECTOR: SentenceDetectorME get() = SentenceDetectorME(SentenceModel(getResource("en-sent.bin")!!))

/**
 * A tokenizer meant to detect tokens written in the English language.
 */
/**
 * A tokenizer meant to detect tokens written in the English language.
 */
public val ENGLISH_TOKENIZER: TokenizerME by lazy { TokenizerME(TokenizerModel(getResource("en-token.bin"))) }

/**
 * TODO
 */
val ENGLISH_POS_TAGGER: POSTaggerME by lazy { POSTaggerME(POSModel(getResource("en-pos-maxent.bin"))) }
val ENGLISH_CHUNKER: ChunkerME by lazy { ChunkerME(ChunkerModel(getResource("en-chunker.bin"))) }
val ENGLISH_NAME_FINDER: NameFinderME by lazy { NameFinderME(TokenNameFinderModel(
    getResource("en-ner-person.bin")
)) }

/**
 * TODO
 */
internal fun TokenizerME.tokenizeAll(sentences: Array<String>): List<List<String>> {
    return sentences.map { tokenize(it).toList() };
}

internal fun POSTaggerME.tagAll(tokenizedAll: List<List<String>>): List<List<String>> {
    return tokenizedAll.map { this.tag(it.toTypedArray()).toList() }
}

private fun ChunkerME.chunkUp(tokenizedAll: List<List<String>>,
                              sentenceArray: List<List<String>>): List<List<String>> {
    val returnList = mutableListOf<List<String>>();
    for ((i, _) in tokenizedAll.withIndex()) {
        returnList.add(this.chunk(tokenizedAll[i].toTypedArray(), sentenceArray[i].toTypedArray()).toList())
    }
    return returnList
}

internal fun SentenceDetectorME.detectSentencesAndProcess(text: String): List<Sentence> {
    val sentences = sentDetect(text).map { processedSentence(it) }
    return sentences
}

val ENGLISH_TAGGER: POSTaggerME = POSTaggerME(POSModel(getResource("en-pos-maxent.bin")))
public fun getNamesFromSentenceAndSpanInfo(sentenceList: List<Sentence>, listOfSpans: List<List<Span>>): MutableList<String> {
    // TODO
    val namesFound = mutableListOf<String>();
    for ((index, arraySpan) in listOfSpans.withIndex()) {
        populateListWithNames(namesFound, arraySpan, sentenceList[index])
    }
    return namesFound
}

internal fun populateListWithNames(
    namesFound: MutableList<String>,
    arraySpan: List<Span>,
    sentence: Sentence
) {
    for (span: Span in arraySpan) {
        println("${span.start} to ${span.end}");
        val slice = sentence.tokens!!.slice(IntRange(span.start, span.end - 1)).map { it.tokenString }
        val nameString = slice.joinToString(separator = " ");
        namesFound.add(nameString)
    }
}