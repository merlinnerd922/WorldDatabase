package edu.stanford.nlp.pipeline

import edu.stanford.nlp.pipeline.CoreDocument
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.pipeline.CoreEntityMention
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation
import java.util.stream.Collectors
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation
import edu.stanford.nlp.pipeline.CoreSentence
import edu.stanford.nlp.trees.tregex.TregexPattern
import java.lang.RuntimeException
import edu.stanford.nlp.trees.tregex.TregexMatcher
import edu.stanford.nlp.semgraph.SemanticGraph
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree
import edu.stanford.nlp.ie.util.RelationTriple
import edu.stanford.nlp.ling.CoreAnnotations.KBPTriplesAnnotation
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.util.Pair
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

/**
 * Wrapper around a CoreMap representing a sentence.  Adds some helpful methods.
 *
 */
class CoreSentence(private val document: CoreDocument, private val sentenceCoreMap: CoreMap) {
    private var entityMentions: List<CoreEntityMention>? = null

    /** create list of CoreEntityMention's based on the CoreMap's entity mentions  */
    fun wrapEntityMentions() {
        if (sentenceCoreMap[MentionsAnnotation::class.java] != null) {
            entityMentions = sentenceCoreMap[MentionsAnnotation::class.java]!!.stream()
                .map { coreMapEntityMention: CoreMap? -> CoreEntityMention(this, coreMapEntityMention!!) }
                .collect(Collectors.toList())
        }
    }

    /** get the document this sentence is in  */
    fun document(): CoreDocument {
        return document
    }

    /** get the underlying CoreMap if need be  */
    fun coreMap(): CoreMap {
        return sentenceCoreMap
    }

    /** full text of the sentence  */
    fun text(): String {
        return sentenceCoreMap[TextAnnotation::class.java]!!
    }

    /** char offsets of mention  */
    fun charOffsets(): Pair<Int, Int> {
        val beginCharOffset = sentenceCoreMap[CharacterOffsetBeginAnnotation::class.java]
        val endCharOffset = sentenceCoreMap[CharacterOffsetEndAnnotation::class.java]
        return Pair(beginCharOffset!!, endCharOffset!!)
    }

    /** list of tokens  */
    fun tokens(): List<CoreLabel> {
        return sentenceCoreMap[TokensAnnotation::class.java]!!
    }

    /** list of tokens as String  */
    fun tokensAsStrings(): List<String> {
        return tokens().stream().map { obj: CoreLabel -> obj.word() }.collect(Collectors.toList())
    }

    /** list of pos tags  */
    fun posTags(): List<String> {
        return tokens().stream().map { obj: CoreLabel -> obj.tag() }.collect(Collectors.toList())
    }

    /** list of lemma tags  */
    fun lemmas(): List<String> {
        return tokens().stream().map { obj: CoreLabel -> obj.lemma() }.collect(Collectors.toList())
    }

    /** list of ner tags  */
    fun nerTags(): List<String> {
        return tokens().stream().map { obj: CoreLabel -> obj.ner() }.collect(Collectors.toList())
    }

    /** constituency parse  */
    fun constituencyParse(): Tree? {
        return sentenceCoreMap[TreeAnnotation::class.java]
    }

    /** Tregex - find subtrees of interest with a general Tregex pattern  */
    fun tregexResultTrees(s: String): List<Tree> {
        // the patterns are cached by computeIfAbsent, so we don't wastefully recompile a TregexPattern every sentence
        return tregexResultTrees(patternCache.computeIfAbsent(s, compilePattern))
    }

    fun tregexResultTrees(p: TregexPattern): List<Tree> {
        // throw a RuntimeException if no constituency parse available to signal to user to use "parse" annotator
        if (constituencyParse() == null) throw RuntimeException(
            "Error: Attempted to run Tregex on sentence without a constituency parse.  " +
                    "To use this method you must annotate the document with a constituency parse using the 'parse' " +
                    "annotator."
        )
        val results: MutableList<Tree> = ArrayList()
        val matcher = p.matcher(constituencyParse())
        while (matcher.find()) {
            results.add(matcher.match)
        }
        return results
    }

    fun tregexResults(p: TregexPattern): List<String> {
        return tregexResultTrees(p).stream().map(treeToSpanString).collect(Collectors.toList())
    }

    fun tregexResults(s: String): List<String> {
        return tregexResultTrees(s).stream().map(treeToSpanString).collect(Collectors.toList())
    }

    /** return noun phrases, assuming NP is the label  */
    fun nounPhraseTrees(): List<Tree> {
        return tregexResultTrees(nounPhrasePattern)
    }

    fun nounPhrases(): List<String> {
        return nounPhraseTrees().stream().map(treeToSpanString).collect(Collectors.toList())
    }

    /** return verb phrases, assuming VP is the label  */
    fun verbPhraseTrees(): List<Tree> {
        return tregexResultTrees(verbPhrasePattern)
    }

    fun verbPhrases(): List<String> {
        return verbPhraseTrees().stream().map(treeToSpanString).collect(Collectors.toList())
    }

    /** dependency parse  */
    fun dependencyParse(): SemanticGraph {
        return sentenceCoreMap[EnhancedPlusPlusDependenciesAnnotation::class.java]!!
    }

    /** sentiment  */
    fun sentiment(): String {
        return sentenceCoreMap[SentimentCoreAnnotations.SentimentClass::class.java]!!
    }

    /** sentiment tree  */
    fun sentimentTree(): Tree {
        return sentenceCoreMap[SentimentAnnotatedTree::class.java]!!
    }

    /** list of entity mentions  */
    fun entityMentions(): List<CoreEntityMention>? {
        return entityMentions
    }

    /** list of KBP relations found  */
    fun relations(): List<RelationTriple> {
        return sentenceCoreMap[KBPTriplesAnnotation::class.java]!!
    }

    override fun toString(): String {
        return coreMap().toString()
    }

    companion object {
        /** common patterns to search for constituency parses  */
        private val nounPhrasePattern = TregexPattern.compile("NP")
        private val verbPhrasePattern = TregexPattern.compile("VP")

        /** cache to hold general patterns  */
        private val patternCache = ConcurrentHashMap<String, TregexPattern>()
        private val compilePattern = Function { tregex: String? -> TregexPattern.compile(tregex) }
        private val treeToSpanString = Function { obj: Tree -> obj.spanString() }
    }
}