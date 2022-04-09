@file:Suppress("unused")

package nlp

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
public val ENGLISH_TOKENIZER: TokenizerME get() = TokenizerME(TokenizerModel(getResource("en-token.bin")))
val ENGLISH_POS_TAGGER: POSTaggerME get() = POSTaggerME(POSModel(getResource("en-pos-maxent.bin")))
val ENGLISH_CHUNKER: ChunkerME get() = ChunkerME(ChunkerModel(getResource("en-chunker.bin")))
val ENGLISH_NAME_FINDER: NameFinderME
    get() = NameFinderME(TokenNameFinderModel(getResource("en-ner-person.bin")))

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
    val sentences = sentDetect(text).map { Sentence(it) }

    // TODO rename
    val withIndex = sentences.withIndex()
    for ((i, sentence) in withIndex) {
        println("Starting on sentence ${i+1} of ${withIndex.count()}")
        sentence.tokenize()
        sentence.tag()
        sentence.chunkUp()
    }
    return sentences
}

val ENGLISH_TAGGER: POSTaggerME = POSTaggerME(POSModel(getResource("en-pos-maxent.bin")))