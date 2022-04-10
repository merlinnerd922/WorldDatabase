package nlp.opennlp.chunk

import utils.SmartAny

/**
 * A grammatical chunk representing a concept representable by a single part-of-speech.
 */
class Chunk : SmartAny() {

    /**
     * The grammatical part of speech represented by this chunk.
     */
    public var partOfSpeech: String? = null;

    /**
     * The list of tags constituting this chunk.
     */
    public var tagList : MutableList<String> = mutableListOf();

    /**
     * The list of tokens constituting this chunk.
     */
    public var tokenList : MutableList<String> = mutableListOf();

    /**
     * Append the provided tag/token pair to this chunk by adding them to their respective lists.
     */
    internal fun addTokenTag(tag: String, token: String) {
        this.tagList.add(tag);
        tokenList.add(token);
    }
}

