package edu.stanford.nlp.pipeline

import edu.stanford.nlp.coref.CorefCoreAnnotations
import edu.stanford.nlp.coref.data.CorefChain
import edu.stanford.nlp.pipeline.CoreEntityMention
import edu.stanford.nlp.pipeline.CoreQuote
import edu.stanford.nlp.pipeline.CoreSentence
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.pipeline.QuoteAnnotator
import java.util.stream.Collectors
import edu.stanford.nlp.ling.CoreAnnotations.DocIDAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.DocDateAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import java.util.function.Consumer

/**
 * Wrapper around an annotation representing a document.  Adds some helpful methods.
 *
 */
class CoreDocument {
    @JvmField
    var annotationDocument: Annotation
    private var entityMentions: List<CoreEntityMention>? = null
    private var quotes: List<CoreQuote>? = null
    private var sentences: List<CoreSentence>? = null

    constructor(documentText: String?) {
        annotationDocument = Annotation(documentText)
    }

    constructor(annotation: Annotation) {
        annotationDocument = annotation
        wrapAnnotations()
    }

    /** complete the wrapping process post annotation by a pipeline  */
    fun wrapAnnotations() {
        // wrap all of the sentences
        if (annotationDocument.get(SentencesAnnotation::class.java) != null) {
            wrapSentences()
            // if there are entity mentions, build a document wide list
            if (!sentences!!.isEmpty() && sentences!![0].entityMentions() != null) {
                buildDocumentEntityMentionsList()
            }
            // if there are quotes, build a document wide list
            if (QuoteAnnotator.gatherQuotes(annotationDocument) != null) buildDocumentQuotesList()
        }
    }

    /** create list of CoreSentence's based on the Annotation's sentences  */
    private fun wrapSentences() {
        sentences = annotationDocument.get(SentencesAnnotation::class.java).stream()
            .map { coreMapSentence: CoreMap? -> CoreSentence(this, coreMapSentence) }
            .collect(Collectors.toList())
        sentences!!.forEach(Consumer { obj: CoreSentence -> obj.wrapEntityMentions() })
    }

    /** build a list of all entity mentions in the document from the sentences  */
    private fun buildDocumentEntityMentionsList() {
        entityMentions = sentences!!.stream().flatMap { sentence: CoreSentence -> sentence.entityMentions().stream() }
            .collect(Collectors.toList())
    }

    private fun buildDocumentQuotesList() {
        quotes = QuoteAnnotator.gatherQuotes(annotationDocument).stream()
            .map { coreMapQuote: CoreMap? -> CoreQuote(this, coreMapQuote) }
            .collect(Collectors.toList())
    }

    /** provide access to the underlying annotation if needed  */
    fun annotation(): Annotation {
        return annotationDocument
    }

    /** return the doc id of this doc  */
    fun docID(): String {
        return annotationDocument.get(DocIDAnnotation::class.java)
    }

    /** return the doc date of this doc  */
    fun docDate(): String {
        return annotationDocument.get(DocDateAnnotation::class.java)
    }

    /** return the full text of the doc  */
    fun text(): String {
        return annotationDocument.get(TextAnnotation::class.java)
    }

    /** return the full token list for this doc  */
    fun tokens(): List<CoreLabel> {
        return annotationDocument.get(TokensAnnotation::class.java)
    }

    /** the list of sentences in this document  */
    fun sentences(): List<CoreSentence>? {
        return sentences
    }

    /** the list of entity mentions in this document  */
    fun entityMentions(): List<CoreEntityMention>? {
        return entityMentions
    }

    /** coref info  */
    fun corefChains(): Map<Int, CorefChain> {
        return annotationDocument.get(
            CorefCoreAnnotations.CorefChainAnnotation::class.java
        )
    }

    /** quotes  */
    fun quotes(): List<CoreQuote>? {
        return quotes
    }

    override fun toString(): String {
        return annotation().toString()
    }
}