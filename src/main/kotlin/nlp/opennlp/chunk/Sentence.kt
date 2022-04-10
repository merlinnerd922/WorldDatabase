@file:Suppress("unused")

package nlp.opennlp.chunk

import utils.SmartAny
import opennlp.tools.tokenize.TokenizerME

/**
 * TODO
 * A representation of a sentence, as processed by an NLP.
 */
class Sentence() : SmartAny() {

    /**
     * Construct a sentence from the provided String.
     */
    internal constructor(sentenceString: String) : this() {
        this.sentenceString = sentenceString
    }

    /**
     * The String representation of this sentence.
     */
    private var sentenceString: String? = null;

    /**
     * The list of part-of-speech tags corresponding to the tokens in this sentence.
     */
    internal var tags: List<Tag>? = null;

    /**
     * The list of tokens corresponding to different words and punctuation marks in this sentence.
     */
    internal var tokens: List<Token>? = null;

    /**
     * TODO
     */
    internal var chunkList: List<Chunk>? =null;

    /**
     * TODO
     */
    fun tokenizeAsToken() {
        this.tokens = ENGLISH_TOKENIZER.tokenizeAsToken<Token>(sentenceString);
    }

    public fun <T> TokenizerME.tokenizeAsToken(sentenceString: String?) = tokenize(sentenceString)!!.map { Token(it) }

    /**
     * TODO
     */
    fun tag() {
        this.tags = ENGLISH_TAGGER.tag(tokens!!.map{it.tokenString}.toTypedArray())!!.map{ Tag(it) }
    }

    /**
     * TODO
     */
    fun chunkUp() {
        val chunksAsStrings = ENGLISH_CHUNKER.chunk(
            tokens!!.map { it.tokenString }.toTypedArray(),
            tags!!.map { it.tagString }.toTypedArray()
        );
        val newChunkProcessor = ChunkProcessor().also { it.verbose = true; }
        newChunkProcessor.processChunks(this, chunksAsStrings!!.toList());
    }

    /**
     * TODO
     */
    fun extractProperNouns(): List<Chunk> {
        return this.chunkList!!.filter { it.partOfSpeech == "NP" };
    }


}

fun processedSentence(sentenceString: String): Sentence {
    return Sentence(sentenceString).also { it.tokenizeAsToken(); it.tag(); it.chunkUp(); }
}