package edu.stanford.nlp.pipeline

import edu.stanford.nlp.pipeline.CoreSentence
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.EntityTypeAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagProbsAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.WikipediaEntityAnnotation
import edu.stanford.nlp.pipeline.CoreEntityMention
import edu.stanford.nlp.pipeline.CoreDocument
import edu.stanford.nlp.ling.CoreAnnotations.CanonicalEntityMentionIndexAnnotation
import edu.stanford.nlp.util.Pair
import java.util.*

/**
 * Wrapper around a CoreMap representing a entity mention.  Adds some helpful methods.
 *
 */
class CoreEntityMention(private val sentence: CoreSentence, private val entityMentionCoreMap: CoreMap) {
    /** get the underlying CoreMap if need be  */
    fun coreMap(): CoreMap {
        return entityMentionCoreMap
    }

    /** get this entity mention's sentence  */
    fun sentence(): CoreSentence {
        return sentence
    }

    /** full text of the mention  */
    fun text(): String {
        return entityMentionCoreMap.get(TextAnnotation::class.java)
    }

    /** the list of tokens for this entity mention  */
    fun tokens(): List<CoreLabel> {
        return entityMentionCoreMap.get(TokensAnnotation::class.java)
    }

    /** char offsets of mention  */
    fun charOffsets(): Pair<Int, Int> {
        val beginCharOffset = entityMentionCoreMap.get(CharacterOffsetBeginAnnotation::class.java)
        val endCharOffset = entityMentionCoreMap.get(CharacterOffsetEndAnnotation::class.java)
        return Pair(beginCharOffset, endCharOffset)
    }

    /** return the type of the entity mention  */
    fun entityType(): String {
        return entityMentionCoreMap.get(EntityTypeAnnotation::class.java)
    }

    /** return a map of labels to confidences  */
    fun entityTypeConfidences(): Map<String, Double> {
        return entityMentionCoreMap.get(
            NamedEntityTagProbsAnnotation::class.java
        )
    }

    /** return the entity this entity mention is linked to  */
    fun entity(): String {
        return entityMentionCoreMap.get(WikipediaEntityAnnotation::class.java)
    }

    /** return the canonical entity mention for this entity mention  */
    fun canonicalEntityMention(): Optional<CoreEntityMention> {
        val myDocument = sentence.document()
        val canonicalEntityMentionIndex = Optional.ofNullable(
            coreMap().get(
                CanonicalEntityMentionIndexAnnotation::class.java
            )
        )
        return if (canonicalEntityMentionIndex.isPresent) Optional.of(
            sentence.document().entityMentions()!![canonicalEntityMentionIndex.get()]
        ) else Optional.empty()
    }

    override fun toString(): String {
        return coreMap().toString()
    }
}