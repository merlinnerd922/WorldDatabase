package edu.stanford.nlp.pipeline

import edu.stanford.nlp.ling.CoreAnnotations.*
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.*
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.util.Pair

/**
 * Wrapper around a CoreMap representing a quote.  Adds some helpful methods.
 *
 * @author Jason Bolton
 */

class CoreQuote(private val document: CoreDocument, private val quoteCoreMap: CoreMap) {
    private val sentences: MutableList<CoreSentence>

    // optional speaker info...note there may not be an entity mention corresponding to the speaker
    private var hasSpeaker: Boolean
    private var hasCanonicalSpeaker: Boolean
    private val speaker: String?
    internal val canonicalSpeaker: String?
    private var speakerTokens: MutableList<CoreLabel>?
    private var canonicalSpeakerTokens: MutableList<CoreLabel>?
    private var speakerCharOffsets: Pair<Int, Int>?
    private var canonicalSpeakerCharOffsets: Pair<Int, Int>?
    private var speakerEntityMention: CoreEntityMention?
    private var canonicalSpeakerEntityMention: CoreEntityMention?

    init {
        // attach sentences to the quote
        sentences = ArrayList()
        val firstSentenceIndex = quoteCoreMap.get(SentenceBeginAnnotation::class.java)
        val lastSentenceIndex = quoteCoreMap.get(SentenceEndAnnotation::class.java)
        for (currSentIndex in firstSentenceIndex..lastSentenceIndex) {
            sentences.add(document.sentences()!![currSentIndex])
        }
        // set up the speaker info
        speaker = 
            quoteCoreMap.get(
                QuoteAttributionAnnotator.SpeakerAnnotation::class.java
            )
        
        canonicalSpeaker = 
            quoteCoreMap.get(
                CanonicalMentionAnnotation::class.java
            )
        
        // set up info for direct speaker mention (example: "He")
        val firstSpeakerTokenIndex = quoteCoreMap.get(MentionBeginAnnotation::class.java)
        val lastSpeakerTokenIndex = quoteCoreMap.get(MentionEndAnnotation::class.java)
        speakerTokens = null
        speakerCharOffsets = null
        speakerEntityMention = null
        if (firstSpeakerTokenIndex != null && lastSpeakerTokenIndex != null) {
            speakerTokens = ArrayList()
            for (speakerTokenIndex in firstSpeakerTokenIndex..lastSpeakerTokenIndex) {
                speakerTokens!!.add(document.tokens()[speakerTokenIndex])
            }
            val speakerCharOffsetBegin = speakerTokens!![0].get(
                CharacterOffsetBeginAnnotation::class.java
            )
            val speakerCharOffsetEnd =
                speakerTokens!![speakerTokens!!.size - 1].get(CharacterOffsetEndAnnotation::class.java)
            speakerCharOffsets = Pair(speakerCharOffsetBegin, speakerCharOffsetEnd)
            for (candidateEntityMention in document.entityMentions()!!) {
                val entityMentionOffsets = candidateEntityMention.charOffsets()
                if (entityMentionOffsets.equals(speakerCharOffsets!!)) {
                    speakerEntityMention = candidateEntityMention
                    break
                }
            }
        }
        // set up info for canonical speaker mention (example: "Joe Smith")
        val firstCanonicalSpeakerTokenIndex = quoteCoreMap.get(
            CanonicalMentionBeginAnnotation::class.java
        )
        val lastCanonicalSpeakerTokenIndex = quoteCoreMap.get(CanonicalMentionEndAnnotation::class.java)
        canonicalSpeakerTokens = null
        canonicalSpeakerCharOffsets = null
        canonicalSpeakerEntityMention = null
        if (firstCanonicalSpeakerTokenIndex != null && lastCanonicalSpeakerTokenIndex != null) {
            canonicalSpeakerTokens = ArrayList()
            for (canonicalSpeakerTokenIndex in firstCanonicalSpeakerTokenIndex..lastCanonicalSpeakerTokenIndex) {
                canonicalSpeakerTokens!!.add(document.tokens()[canonicalSpeakerTokenIndex])
            }
            val canonicalSpeakerCharOffsetBegin = canonicalSpeakerTokens!![0].get(
                CharacterOffsetBeginAnnotation::class.java
            )
            val canonicalSpeakerCharOffsetEnd =
                canonicalSpeakerTokens!![canonicalSpeakerTokens!!.size - 1].get(CharacterOffsetEndAnnotation::class.java)
            canonicalSpeakerCharOffsets =
                Pair(canonicalSpeakerCharOffsetBegin, canonicalSpeakerCharOffsetEnd)
            for (candidateEntityMention in document.entityMentions()!!) {
                val entityMentionOffsets = candidateEntityMention.charOffsets()
                if (entityMentionOffsets.equals(canonicalSpeakerCharOffsets!!)) {
                    canonicalSpeakerEntityMention = candidateEntityMention
                    break
                }
            }
        }
        // record if there is speaker info
        hasSpeaker = speaker != null
        hasCanonicalSpeaker = canonicalSpeaker != null
    }

    /** get the underlying CoreMap if need be  */
    fun coreMap(): CoreMap {
        return quoteCoreMap
    }

    /** get this quote's document  */
    fun document(): CoreDocument {
        return document
    }

    /** full text of the mention  */
    fun text(): String {
        return quoteCoreMap.get(TextAnnotation::class.java)
    }

    /** retrieve the CoreSentence's attached to this quote  */
    fun sentences(): List<CoreSentence> {
        return sentences
    }

    /** retrieve the text of the speaker  */
    fun speaker(): String? {
        return speaker
    }

    /** retrieve the text of the canonical speaker  */
    fun canonicalSpeaker(): String? {
        return canonicalSpeaker
    }

    /** retrieve the tokens of the speaker  */
    fun speakerTokens(): MutableList<CoreLabel>? {
        return speakerTokens
    }

    /** retrieve the character offsets of the speaker  */
    fun speakerCharOffsets(): Pair<Int, Int>? {
        return speakerCharOffsets
    }

    /** retrieve the entity mention corresponding to the speaker if there is one  */
    fun speakerEntityMention(): CoreEntityMention? {
        return speakerEntityMention
    }

    /** retrieve the tokens of the canonical speaker  */
    fun canonicalSpeakerTokens(): MutableList<CoreLabel>? {
        return canonicalSpeakerTokens
    }

    /** retrieve the character offsets of the canonical speaker  */
    fun canonicalSpeakerCharOffsets(): Pair<Int, Int>? {
        return canonicalSpeakerCharOffsets
    }

    /** retrieve the entity mention corresponding to the canonical speaker if there is one  */
    fun canonicalSpeakerEntityMention(): CoreEntityMention? {
        return canonicalSpeakerEntityMention
    }

    /** char offsets of quote  */
    fun quoteCharOffsets(): Pair<Int, Int> {
        val beginCharOffset = quoteCoreMap.get(CharacterOffsetBeginAnnotation::class.java)
        val endCharOffset = quoteCoreMap.get(CharacterOffsetEndAnnotation::class.java)
        return Pair(beginCharOffset, endCharOffset)
    }

    override fun toString(): String {
        return coreMap().toString()
    }
}