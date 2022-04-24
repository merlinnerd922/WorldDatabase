package edu.stanford.nlp.ling

import edu.stanford.nlp.ling.CoreAnnotation
import edu.stanford.nlp.util.ErasureUtils
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.util.CoreMap
import java.util.Calendar
import java.util.HashMap
import edu.stanford.nlp.ling.CoreAnnotations.SRL_ID
import edu.stanford.nlp.ling.WordLemmaTag
import edu.stanford.nlp.ling.MultiTokenTag
import java.util.SortedSet
import edu.stanford.nlp.ie.util.RelationTriple
import edu.stanford.nlp.util.IntPair
import edu.stanford.nlp.util.Pair
import edu.stanford.nlp.util.Triple

/**
 * Set of common annotations for [CoreMap]s. The classes
 * defined here are typesafe keys for getting and setting annotation
 * values. These classes need not be instantiated outside of this
 * class. e.g [TextAnnotation].class serves as the key and a
 * `String` serves as the value containing the
 * corresponding word.
 *
 *
 * New types of [CoreAnnotation] can be defined anywhere that is
 * convenient in the source tree - they are just classes. This file exists to
 * hold widely used "core" annotations and others inherited from the
 * [Label] family. In general, most keys should be placed in this file as
 * they may often be reused throughout the code. This architecture allows for
 * flexibility, but in many ways it should be considered as equivalent to an
 * enum in which everything should be defined
 *
 *
 * The getType method required by CoreAnnotation must return the same class type
 * as its value type parameter. It feels like one should be able to get away
 * without that method, but because Java erases the generic type signature, that
 * info disappears at runtime. See [ValueAnnotation] for an example.
 *
 * @author dramage
 * @author rafferty
 * @author bethard
 */
class CoreAnnotations private constructor() // only static members
{
    /**
     * The CoreMap key identifying the annotation's text.
     *
     * Note that this key is intended to be used with many different kinds of
     * annotations - documents, sentences and tokens all have their own text.
     */
    class TextAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key for getting the lemma (morphological stem, lexeme form) of a token.
     *
     * This key is typically set on token annotations.
     */
    class LemmaAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key for getting the Penn part of speech of a token.
     *
     * This key is typically set on token annotations.
     */
    class PartOfSpeechAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key for getting the token-level named entity tag (e.g., DATE,
     * PERSON, etc.)
     *
     * This key is typically set on token annotations.
     */
    class NamedEntityTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Label and probability pair representing the coarse grained label and probability
     */
    class NamedEntityTagProbsAnnotation : CoreAnnotation<Map<String, Double>> {
        override val type: Class<Map<String, Double>>
            get() = ErasureUtils.uncheckedCast(
                MutableMap::class.java
            )
    }

    /**
     * The CoreMap key for getting the coarse named entity tag (i.e. LOCATION)
     */
    class CoarseNamedEntityTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key for getting the fine grained named entity tag (i.e. CITY)
     */
    class FineGrainedNamedEntityTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key for getting the token-level named entity tag (e.g., DATE,
     * PERSON, etc.) from a previous NER tagger. NERFeatureFactory is sensitive to
     * this tag and will turn the annotations from the previous NER tagger into
     * new features. This is currently used to implement one level of stacking --
     * we may later change it to take a list as needed.
     *
     * This key is typically set on token annotations.
     */
    class StackedNamedEntityTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key for getting the token-level true case annotation (e.g.,
     * INIT_UPPER)
     *
     * This key is typically set on token annotations.
     */
    class TrueCaseAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key identifying the annotation's true-cased text.
     *
     * Note that this key is intended to be used with many different kinds of
     * annotations - documents, sentences and tokens all have their own text.
     */
    class TrueCaseTextAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key for getting the tokens contained by an annotation.
     *
     * This key should be set for any annotation that contains tokens. It can be
     * done without much memory overhead using List.subList.
     */
    class TokensAnnotation : CoreAnnotation<List<CoreLabel>> {
        override val type: Class<List<CoreLabel>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * The CoreMap key for getting the tokens (can be words, phrases or anything that are of type CoreMap) contained by an annotation.
     *
     * This key should be set for any annotation that contains tokens (words, phrases etc). It can be
     * done without much memory overhead using List.subList.
     */
    class GenericTokensAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * The CoreMap key for getting the sentences contained in an annotation.
     * The sentences are represented as a `List<CoreMap>`.
     * Each sentence might typically have annotations such as `TextAnnotation`,
     * `TokensAnnotation`, `SentenceIndexAnnotation`, and `BasicDependenciesAnnotation`.
     *
     * This key is typically set only on document annotations.
     */
    class SentencesAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * The CoreMap key for getting the quotations contained by an annotation.
     *
     * This key is typically set only on document annotations.
     */
    class QuotationsAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * The CoreMap key for getting the quotations contained by an annotation.
     *
     * This key is typically set only on document annotations.
     */
    class UnclosedQuotationsAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * Unique identifier within a document for a given quotation. Counts up from zero.
     */
    class QuotationIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * The index of the sentence that this annotation begins in. Currently only used by quote attribution.
     * Set to the SentenceIndexAnnotation of the first sentence of a quote.
     */
    class SentenceBeginAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * The index of the sentence that this annotation begins in.
     */
    class SentenceEndAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * The CoreMap key for getting the paragraphs contained by an annotation.
     *
     * This key is typically set only on document annotations.
     */
    class ParagraphsAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * The CoreMap key identifying the first token included in an annotation. The
     * token with index 0 is the first token in the document.
     *
     * This key should be set for any annotation that contains tokens.
     */
    class TokenBeginAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * The CoreMap key identifying the last token after the end of an annotation.
     * The token with index 0 is the first token in the document.
     *
     * This key should be set for any annotation that contains tokens.
     */
    class TokenEndAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * The CoreMap key identifying the date and time associated with an
     * annotation.
     *
     * This key is typically set on document annotations.
     */
    class CalendarAnnotation : CoreAnnotation<Calendar> {
        override val type: Class<Calendar>
            get() = Calendar::class.java
    }
    /*
   * These are the keys hashed on by IndexedWord
   */
    /**
     * This refers to the unique identifier for a "document", where document may
     * vary based on your application.
     */
    open class DocIDAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * This indexes a token number inside a sentence.  Standardly, tokens are
     * indexed within a sentence starting at 1 (not 0: we follow common parlance
     * whereby we speak of the first word of a sentence).
     * This is generally an individual word or feature index - it is local, and
     * may not be uniquely identifying without other identifiers such as sentence
     * and doc. However, if these are the same, the index annotation should be a
     * unique identifier for differentiating objects.
     */
    class IndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * This indexes the beginning of a span of words, e.g., a constituent in a
     * tree. See [edu.stanford.nlp.trees.Tree.indexSpans].
     * This annotation counts tokens.
     * It standardly indexes from 1 (like IndexAnnotation).  The reasons for
     * this are: (i) Talking about the first word of a sentence is kind of
     * natural, and (ii) We use index 0 to refer to an imaginary root in
     * dependency output.
     */
    class BeginIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * This indexes the end of a span of words, e.g., a constituent in a
     * tree.  See [edu.stanford.nlp.trees.Tree.indexSpans]. This annotation
     * counts tokens.  It standardly indexes from 1 (like IndexAnnotation).
     * The end index is not a fencepost: its value is equal to the
     * IndexAnnotation of the last word in the span.
     */
    class EndIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * This indicates that starting at this token, the sentence should not be ended until
     * we see a ForcedSentenceEndAnnotation.  Used to force the ssplit annotator
     * (eg the WordToSentenceProcessor) to keep tokens in the same sentence
     * until ForcedSentenceEndAnnotation is seen.
     */
    class ForcedSentenceUntilEndAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * This indicates the sentence should end at this token.  Used to
     * force the ssplit annotator (eg the WordToSentenceProcessor) to
     * start a new sentence at the next token.
     */
    class ForcedSentenceEndAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * Unique identifier within a document for a given sentence. Counts up starting from zero.
     */
    class SentenceIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * Line number for a sentence in a document delimited by newlines
     * instead of punctuation.  May skip numbers if there are blank
     * lines not represented as sentences.  Indexed from 1 rather than 0.
     */
    class LineNumberAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * Contains the "value" - an ill-defined string used widely in MapLabel.
     */
    class ValueAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class CategoryAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The exact original surface form of a token.  This is created in the
     * invertible PTBTokenizer. The tokenizer may normalize the token form to
     * match what appears in the PTB, but this key will hold the original characters.
     */
    class OriginalTextAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Annotation for the whitespace characters appearing before this word. This
     * can be filled in by an invertible tokenizer so that the original text string can be
     * reconstructed.
     */
    class BeforeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Annotation for the whitespace characters appear after this word. This can
     * be filled in by an invertible tokenizer so that the original text string can be
     * reconstructed.
     *
     * Note: When running a tokenizer token-by-token, in general this field will only
     * be filled in after the next token is read, so you need to be reading this field
     * one behind. Be careful about this.
     */
    class AfterAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * CoNLL dep parsing - coarser POS tags.
     */
    class CoarseTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * CoNLL dep parsing - the dependency type, such as SBJ or OBJ. This should be unified with CoNLLDepTypeAnnotation.
     */
    class CoNLLDepAnnotation : CoreAnnotation<CoreMap> {
        override val type: Class<CoreMap>
            get() = CoreMap::class.java
    }

    /**
     * CoNLL SRL/dep parsing - whether the word is a predicate
     */
    class CoNLLPredicateAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * CoNLL SRL/dep parsing - map which, for the current word, specifies its
     * specific role for each predicate
     */
    class CoNLLSRLAnnotation : CoreAnnotation<Map<Int, String>> {
        override val type: Class<Map<Int, String>>
            get() = ErasureUtils.uncheckedCast(MutableMap::class.java)
    }

    /**
     * CoNLL dep parsing - the dependency type, such as SBJ or OBJ. This should be unified with CoNLLDepAnnotation.
     */
    class CoNLLDepTypeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * CoNLL-U dep parsing - span of multiword tokens
     */
    class CoNLLUTokenSpanAnnotation : CoreAnnotation<IntPair> {
        override val type: Class<IntPair>
            get() = ErasureUtils.uncheckedCast(Pair::class.java)
    }

    /**
     * CoNLL-U dep parsing - List of secondary dependencies
     */
    class CoNLLUSecondaryDepsAnnotation : CoreAnnotation<HashMap<String, String>> {
        override val type: Class<HashMap<String, String>>
            get() = ErasureUtils.uncheckedCast(Pair::class.java)
    }

    /**
     * CoNLL-U dep parsing - List of morphological features
     */
    class CoNLLUFeats : CoreAnnotation<HashMap<String, String>> {
        override val type: Class<HashMap<String, String>>
            get() = ErasureUtils.uncheckedCast(HashMap::class.java)
    }

    /**
     * CoNLL-U dep parsing - Any other annotation
     */
    class CoNLLUMisc : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * CoNLL dep parsing - the index of the word which is the parent of this word
     * in the dependency tree
     */
    class CoNLLDepParentIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * Inverse document frequency of the word this label represents
     */
    class IDFAnnotation : CoreAnnotation<Double> {
        override val type: Class<Double>
            get() = Double::class.java
    }

    /**
     * The standard key for a propbank label which is of type Argument
     */
    class ArgumentAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Another key used for propbank - to signify core arg nodes or predicate
     * nodes
     */
    class MarkingAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for Semantic Head Word which is a String
     */
    class SemanticHeadWordAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for Semantic Head Word POS which is a String
     */
    class SemanticHeadTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Probank key for the Verb sense given in the Propbank Annotation, should
     * only be in the verbnode
     */
    class VerbSenseAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for storing category with functional tags.
     */
    class CategoryFunctionalTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * This is an NER ID annotation (in case the all caps parsing didn't work out
     * for you...)
     */
    class NERIDAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The key for the normalized value of numeric named entities.
     */
    class NormalizedNamedEntityTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    enum class SRL_ID {
        ARG, NO, ALL_NO, REL
    }

    /**
     * The key for semantic role labels (Note: please add to this description if
     * you use this key)
     */
    class SRLIDAnnotation : CoreAnnotation<SRL_ID> {
        override val type: Class<SRL_ID>
            get() = SRL_ID::class.java
    }

    /**
     * The standard key for the "shape" of a word: a String representing the type
     * of characters in a word, such as "Xx" for a capitalized word. See
     * [edu.stanford.nlp.process.WordShapeClassifier] for functions for
     * making shape strings.
     */
    class ShapeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The Standard key for storing the left terminal number relative to the root
     * of the tree of the leftmost terminal dominated by the current node
     */
    class LeftTermAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * The standard key for the parent which is a String
     */
    class ParentAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class INAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for span which is an IntPair
     */
    class SpanAnnotation : CoreAnnotation<IntPair> {
        override val type: Class<IntPair>
            get() = IntPair::class.java
    }

    /**
     * The standard key for the answer which is a String
     */
    class AnswerAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The matching probability for the AnswerAnnotation
     */
    class AnswerProbAnnotation : CoreAnnotation<Double> {
        override val type: Class<Double>
            get() = Double::class.java
    }

    /**
     * The standard key for the answer which is a String
     */
    class PresetAnswerAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for gold answer which is a String
     */
    class GoldAnswerAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for the features which is a Collection
     */
    class FeaturesAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for the semantic interpretation
     */
    class InterpretationAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for the semantic role label of a phrase.
     */
    class RoleAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The standard key for the gazetteer information
     */
    class GazetteerAnnotation : CoreAnnotation<List<String>> {
        override val type: Class<List<String>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * Stem of the word this label represents. (This means the output of an IR-style stemmer,
     * such as the Porter stemmer, not a lemma.
     */
    class StemAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class PolarityAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class MorphoNumAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class MorphoPersAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class MorphoGenAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class MorphoCaseAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * For Chinese: character level information, segmentation. Used for representing
     * a single character as a token.
     */
    class ChineseCharAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /** For Chinese: the segmentation info existing in the original text.  */
    class ChineseOrigSegAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /** For Chinese: the segmentation information from the segmenter.
     * Either a "1" for a new word starting at this position or a "0".
     */
    class ChineseSegAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Not sure exactly what this is, but it is different from
     * ChineseSegAnnotation and seems to indicate if the text is segmented
     */
    class ChineseIsSegmentedAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * for Arabic: character level information, segmentation
     */
    class ArabicCharAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /** For Arabic: the segmentation information from the segmenter.  */
    class ArabicSegAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key identifying the offset of the first char of an
     * annotation. The char with index 0 is the first char in the
     * document.
     *
     * Note that these are currently measured in terms of UTF-16 char offsets, not codepoints,
     * so that when non-BMP Unicode characters are present, such a character will add 2 to
     * the position. On the other hand, these values will work with String#substring() and
     * you can then calculate the number of codepoints in a substring.
     *
     * This key should be set for any annotation that represents a span of text.
     */
    class CharacterOffsetBeginAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * The CoreMap key identifying the offset of the last character after the end
     * of an annotation. The character with index 0 is the first character in the
     * document.
     *
     * Note that these are currently measured in terms of UTF-16 char offsets, not codepoints,
     * so that when non-BMP Unicode characters are present, such a character will add 2 to
     * the position. On the other hand, these values will work with String#substring() and
     * you can then calculate the number of codepoints in a substring.
     *
     * This key should be set for any annotation that represents a span of text.
     */
    class CharacterOffsetEndAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * Some codepoints count as more than one character.  For example,
     * mathematical symbols.  This can cause serious problems in other
     * languages such as Python which see those characters as one
     * character wide.
     * <br></br>
     * This annotation is how many codepoints to the beginning of the text.
     */
    class CodepointOffsetBeginAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * Some codepoints count as more than one character.  For example,
     * mathematical symbols.  This can cause serious problems in other
     * languages such as Python which see those characters as one
     * character wide.
     * <br></br>
     * This annotation is how many codepoints to the end of the text.
     */
    class CodepointOffsetEndAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * Key for relative value of a word - used in RTE
     */
    class CostMagnificationAnnotation : CoreAnnotation<Double> {
        override val type: Class<Double>
            get() = Double::class.java
    }

    class WordSenseAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class SRLInstancesAnnotation : CoreAnnotation<List<List<Pair<String, Pair<*, *>>>>> {
        override val type: Class<List<List<Pair<String, Pair<*, *>>>>>
            get() = ErasureUtils.uncheckedCast(
                MutableList::class.java
            )
    }

    /**
     * Used by RTE to track number of text sentences, to determine when hyp
     * sentences begin.
     */
    class NumTxtSentencesAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * Used in Trees
     */
    class TagLabelAnnotation : CoreAnnotation<Label> {
        override val type: Class<Label>
            get() = Label::class.java
    }

    /**
     * Used in CRFClassifier stuff PositionAnnotation should possibly be an int -
     * it's present as either an int or string depending on context CharAnnotation
     * may be "CharacterAnnotation" - not sure
     */
    class DomainAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class PositionAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class CharAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /** Note: this is not a catchall "unknown" annotation but seems to have a
     * specific meaning for sequence classifiers
     */
    class UnknownAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class IDAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /** Possibly this should be grouped with gazetteer annotation - original key
     * was "gaz".
     */
    class GazAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class PossibleAnswersAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class DistSimAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class AbbrAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class ChunkAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class GovernorAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class AbgeneAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class GeniaAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class AbstrAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class FreqAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class DictAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class WebAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class FemaleGazAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class MaleGazAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class LastGazAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * it really seems like this should have a different name or else be a boolean
     */
    class IsURLAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class LinkAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class MentionsAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /** index into the list of entity mentions in a document  */
    class EntityMentionIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = ErasureUtils.uncheckedCast(Int::class.java)
    }

    /** Index into the list of entity mentions in a document for canonical entity mention.
     * This is primarily for linking entity mentions to their canonical entity mention.
     */
    class CanonicalEntityMentionIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = ErasureUtils.uncheckedCast(Int::class.java)
    }

    /**
     * mapping from coref mentions to corresponding ner derived entity mentions
     */
    class CorefMentionToEntityMentionMappingAnnotation : CoreAnnotation<Map<Int, Int>> {
        override val type: Class<Map<Int, Int>>
            get() = ErasureUtils.uncheckedCast(MutableMap::class.java)
    }

    /**
     * Mapping from NER-derived entity mentions to coref mentions.
     */
    class EntityMentionToCorefMentionMappingAnnotation : CoreAnnotation<Map<Int, Int>> {
        override val type: Class<Map<Int, Int>>
            get() = ErasureUtils.uncheckedCast(MutableMap::class.java)
    }

    class EntityTypeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * it really seems like this should have a different name or else be a boolean
     */
    class IsDateRangeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class PredictedAnswerAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /** Seems like this could be consolidated with something else...  */
    class OriginalCharAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class UTypeAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    class EntityRuleAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Store a list of sections in the document
     */
    class SectionsAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * Store an index into a list of sections
     */
    class SectionIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = ErasureUtils.uncheckedCast(Int::class.java)
    }

    /**
     * Store the beginning of the author mention for this section
     */
    class SectionAuthorCharacterOffsetBeginAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = ErasureUtils.uncheckedCast(Int::class.java)
    }

    /**
     * Store the end of the author mention for this section
     */
    class SectionAuthorCharacterOffsetEndAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = ErasureUtils.uncheckedCast(Int::class.java)
    }

    /**
     * Store the xml tag for the section as a CoreLabel
     */
    class SectionTagAnnotation : CoreAnnotation<CoreLabel> {
        override val type: Class<CoreLabel>
            get() = ErasureUtils.uncheckedCast(CoreLabel::class.java)
    }

    /**
     * Store a list of CoreMaps representing quotes
     */
    class QuotesAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * Indicate whether a sentence is quoted
     */
    class QuotedAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * Section of a document
     */
    class SectionAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Date for a section of a document
     */
    class SectionDateAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Id for a section of a document
     */
    class SectionIDAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Indicates that the token starts a new section and the attributes
     * that should go into that section
     */
    class SectionStartAnnotation : CoreAnnotation<CoreMap> {
        override val type: Class<CoreMap>
            get() = CoreMap::class.java
    }

    /**
     * Indicates that the token end a section and the label of the section
     */
    class SectionEndAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class WordPositionAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class ParaPositionAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class SentencePositionAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    // Why do both this and sentenceposannotation exist? I don't know, but one
    // class
    // uses both so here they remain for now...
    class SentenceIDAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class EntityClassAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class AnswerObjectAnnotation : CoreAnnotation<Any> {
        override val type: Class<Any>
            get() = Any::class.java
    }

    /**
     * Used in Task3 Pascal system
     */
    class BestCliquesAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class BestFullAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class LastTaggedAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Used in wsd.supwsd package
     */
    class LabelAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class NeighborsAnnotation : CoreAnnotation<List<Pair<WordLemmaTag, String>>> {
        override val type: Class<List<Pair<WordLemmaTag, String>>>
            get() = ErasureUtils.uncheckedCast(
                MutableList::class.java
            )
    }

    class ContextsAnnotation : CoreAnnotation<List<Pair<String, String>>> {
        override val type: Class<List<Pair<String, String>>>
            get() = ErasureUtils.uncheckedCast(
                MutableList::class.java
            )
    }

    class DependentsAnnotation : CoreAnnotation<List<Pair<Triple<String, String, String>, String>>> {
        override val type: Class<List<Pair<Triple<String, String, String>, String>>>
            get() = ErasureUtils.uncheckedCast(
                MutableList::class.java
            )
    }

    class WordFormAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class TrueTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class SubcategorizationAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class BagOfWordsAnnotation : CoreAnnotation<List<Pair<String, String>>> {
        override val type: Class<List<Pair<String, String>>>
            get() = ErasureUtils.uncheckedCast(
                MutableList::class.java
            )
    }

    /**
     * Used in srl.unsup
     */
    class HeightAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class LengthAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Used in Gale2007ChineseSegmenter
     */
    class LBeginAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class LMiddleAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class LEndAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class D2_LBeginAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class D2_LMiddleAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class D2_LEndAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class UBlockAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /** Used in Chinese segmenters for whether there was space before a character.  */
    class SpaceBeforeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }
    /*
   * Used in parser.discrim
   */
    /**
     * The base version of the parser state, like NP or VBZ or ...
     */
    class StateAnnotation : CoreAnnotation<CoreLabel> {
        override val type: Class<CoreLabel>
            get() = CoreLabel::class.java
    }

    /**
     * used in binarized trees to say the name of the most recent child
     */
    class PrevChildAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * used in binarized trees to specify the first child in the rule for which
     * this node is the parent
     */
    class FirstChildAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * whether the node is the parent in a unary rule
     */
    class UnaryAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * annotation stolen from the lex parser
     */
    class DoAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * annotation stolen from the lex parser
     */
    class HaveAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * annotation stolen from the lex parser
     */
    class BeAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * annotation stolen from the lex parser
     */
    class NotAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * annotation stolen from the lex parser
     */
    class PercentAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * specifies the base state of the parent of this node in the parse tree
     */
    class GrandparentAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The key for storing a Head word as a string rather than a pointer (as in
     * TreeCoreAnnotations.HeadWordAnnotation)
     */
    class HeadWordStringAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Used in nlp.coref
     */
    class MonthAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class DayAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class YearAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Used in propbank.srl
     */
    class PriorAnnotation : CoreAnnotation<Map<String, Double>> {
        override val type: Class<Map<String, Double>>
            get() = ErasureUtils.uncheckedCast(
                MutableMap::class.java
            )
    }

    class SemanticWordAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class SemanticTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class CovertIDAnnotation : CoreAnnotation<List<IntPair>> {
        override val type: Class<List<IntPair>>
            get() = ErasureUtils.uncheckedCast(
                MutableList::class.java
            )
    }

    class ArgDescendentAnnotation : CoreAnnotation<Pair<String, Double>> {
        override val type: Class<Pair<String, Double>>
            get() = ErasureUtils.uncheckedCast(
                Pair::class.java
            )
    }

    /**
     * Used in SimpleXMLAnnotator. The value is an XML element name String for the
     * innermost element in which this token was contained.
     */
    class XmlElementAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Used in CleanXMLAnnotator.  The value is a list of XML element names indicating
     * the XML tag the token was nested inside.
     */
    class XmlContextAnnotation : CoreAnnotation<List<String>> {
        override val type: Class<List<String>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     *
     * Used for Topic Assignments from LDA or its equivalent models. The value is
     * the topic ID assigned to the current token.
     *
     */
    class TopicAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    // gets the synonymn of a word in the Wordnet (use a bit differently in sonalg's code)
    class WordnetSynAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    //to get words of the phrase
    class PhraseWordsTagAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    //to get pos tag of the phrase i.e. root of the phrase tree in the parse tree
    class PhraseWordsAnnotation : CoreAnnotation<List<String>> {
        override val type: Class<List<String>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    //to get prototype feature, see Haghighi Exemplar driven learning
    class ProtoAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    //which common words list does this word belong to
    class CommonWordsAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    // Document date
    // Needed by SUTime
    class DocDateAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Document type
     * What kind of document is it: story, multi-part article, listing, email, etc
     */
    class DocTypeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Document source type
     * What kind of place did the document come from: newswire, discussion forum, web...
     */
    class DocSourceTypeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Document title
     * What is the document title
     */
    class DocTitleAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Reference location for the document
     */
    class LocationAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * Author for the document
     * (really should be a set of authors, but just have single string for simplicity)
     */
    class AuthorAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    // Numeric annotations
    // Per token annotation indicating whether the token represents a NUMBER or ORDINAL
    // (twenty first => NUMBER ORDINAL)
    class NumericTypeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    // Per token annotation indicating the numeric value of the token
    // (twenty first => 20 1)
    class NumericValueAnnotation : CoreAnnotation<Number> {
        override val type: Class<Number>
            get() = Number::class.java
    }

    // Per token annotation indicating the numeric object associated with an annotation
    class NumericObjectAnnotation : CoreAnnotation<Any> {
        override val type: Class<Any>
            get() = Any::class.java
    }

    /** Annotation indicating whether the numeric phrase the token is part of
     * represents a NUMBER or ORDINAL (twenty first =&gt; ORDINAL ORDINAL).
     */
    class NumericCompositeValueAnnotation : CoreAnnotation<Number> {
        override val type: Class<Number>
            get() = Number::class.java
    }

    /** Annotation indicating the numeric value of the phrase the token is part of
     * (twenty first =&gt; 21 21 ).
     */
    class NumericCompositeTypeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /** Annotation indicating the numeric object associated with an annotation.  */
    class NumericCompositeObjectAnnotation : CoreAnnotation<Any> {
        override val type: Class<Any>
            get() = Any::class.java
    }

    class NumerizedTokensAnnotation : CoreAnnotation<List<CoreMap>> {
        override val type: Class<List<CoreMap>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * used in dcoref.
     * to indicate that the it should use the discourse information annotated in the document
     */
    class UseMarkedDiscourseAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * used in dcoref.
     * to store discourse information. (marking `<TURN>` or quotation)
     */
    class UtteranceAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * used in dcoref.
     * to store speaker information.
     */
    class SpeakerAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * used to store speaker type information for coref
     */
    class SpeakerTypeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * used in dcoref.
     * to store paragraph information.
     */
    class ParagraphAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * used in ParagraphAnnotator.
     * to store paragraph information.
     */
    class ParagraphIndexAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * used in dcoref.
     * to store premarked entity mentions.
     */
    class MentionTokenAnnotation : CoreAnnotation<MultiTokenTag> {
        override val type: Class<MultiTokenTag>
            get() = MultiTokenTag::class.java
    }

    /**
     * used in incremental DAG parser
     */
    class LeftChildrenNodeAnnotation : CoreAnnotation<SortedSet<Pair<CoreLabel, String>>> {
        override val type: Class<SortedSet<Pair<CoreLabel, String>>>
            get() = ErasureUtils.uncheckedCast(
                SortedSet::class.java
            )
    }

    /**
     * Stores an exception associated with processing this document
     */
    class ExceptionAnnotation : CoreAnnotation<Throwable> {
        override val type: Class<Throwable>
            get() = ErasureUtils.uncheckedCast(Throwable::class.java)
    }

    /**
     * The CoreMap key identifying the annotation's antecedent.
     *
     * The intent of this annotation is to go with words that have been
     * linked via coref to some other entity.  For example, if "dog" is
     * corefed to "cirrus" in the sentence "Cirrus, a small dog, ate an
     * entire pumpkin pie", then "dog" would have the
     * AntecedentAnnotation "cirrus".
     *
     * This annotation is currently used ONLY in the KBP slot filling project.
     * In that project, "cirrus" from the example above would also have an
     * AntecedentAnnotation of "cirrus".
     * Generally, you want to use the usual coref graph annotations
     */
    class AntecedentAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class LabelWeightAnnotation : CoreAnnotation<Double> {
        override val type: Class<Double>
            get() = Double::class.java
    }

    class ColumnDataClassifierAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class LabelIDAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    /**
     * An annotation for a sentence tagged with its KBP relation.
     * Attaches to a sentence.
     *
     * @see edu.stanford.nlp.pipeline.KBPAnnotator
     */
    class KBPTriplesAnnotation : CoreAnnotation<List<RelationTriple>> {
        override val type: Class<List<RelationTriple>>
            get() = ErasureUtils.uncheckedCast(MutableList::class.java)
    }

    /**
     * An annotation for the Wikipedia page (i.e., canonical name) associated with
     * this token.
     * This is the recommended annotation to use for entity linking that links to Wikipedia.
     * Attaches to a token, as well as to a mention (see (@link MentionsAnnotation}).
     *
     * @see edu.stanford.nlp.pipeline.WikidictAnnotator
     */
    class WikipediaEntityAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = ErasureUtils.uncheckedCast(String::class.java)
    }

    /**
     * The CoreMap key identifying the annotation's text, as formatted by the
     * [edu.stanford.nlp.naturalli.QuestionToStatementTranslator].
     *
     * This is attached to [CoreLabel]s.
     */
    class StatementTextAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreMap key identifying an entity mention's potential gender.
     *
     * This is attached to [CoreMap]s.
     */
    class GenderAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreLabel key identifying whether a token is a newline or not
     *
     * This is attached to [CoreLabel]s.
     */
    class IsNewlineAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * The CoreLabel key identifying whether a token is a multi-word-token
     *
     * This is attached to [CoreLabel]s.
     */
    class IsMultiWordTokenAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }

    /**
     * Text of the token that was used to create this word during a multi word token split.
     */
    class MWTTokenTextAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    /**
     * The CoreLabel key identifying whether a token is the first word derived
     * from a multi-word-token.  So if "des" is split into "de" and "les", "de"
     * would be marked as true.
     */
    class IsFirstWordOfMWTAnnotation : CoreAnnotation<Boolean> {
        override val type: Class<Boolean>
            get() = Boolean::class.java
    }
}