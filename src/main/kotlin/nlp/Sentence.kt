package nlp

import SmartAny

class Sentence : SmartAny {

    constructor() {

    }
    private var sentenceString: String? = null;

    constructor(sentenceString: String) : this() {
        this.sentenceString = sentenceString
    }

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
