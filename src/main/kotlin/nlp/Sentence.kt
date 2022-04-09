@file:Suppress("unused")

package nlp

import utils.SmartAny
import nlp.chunk.Chunk
import nlp.chunk.ChunkProcessor

/**
 * TODO
 * A representation of a sentence, as processed by an NLP.
 */
class Sentence() : SmartAny() {

    /**
     * Construct a sentence from the provided String.
     */
    constructor(sentenceString: String) : this() {
        this.sentenceString = sentenceString
    }

    /**
     * The String representation of this sentence.
     */
    private var sentenceString: String? = null;

    /**
     * The list of part-of-speech tags corresponding to the tokens in this sentence.
     */
    private var tags: List<Tag>? = null;
    private var tokens: List<Token>? = null;
    internal var chunkList: MutableList<Chunk>? =null;
    internal var chunksAsStrings: Array<String>? = null;
    internal var tagsAsStringArray: Array<String>? = null;
    internal var tokensAsStringArray: Array<String>? = null;

    fun tokenize() {
        this.tokensAsStringArray = ENGLISH_TOKENIZER.tokenize(sentenceString);
        this.tokens = this.tokensAsStringArray!!.map { Token(it) };
    }

    fun tag() {
        this.tagsAsStringArray = ENGLISH_TAGGER.tag(this.tokensAsStringArray);
        this.tags = this.tagsAsStringArray!!.map{ Tag(it) }
    }

    fun chunkUp() {
        this.chunksAsStrings = ENGLISH_CHUNKER.chunk(tokensAsStringArray, tagsAsStringArray);
        var newChunkProcessor = ChunkProcessor().also { it.verbose = true; }
        newChunkProcessor.processChunks(this);
    }

    fun extractProperNouns(): List<Chunk> {
        return this.chunkList!!.filter { it.partOfSpeech == "NP" };
    }


}
