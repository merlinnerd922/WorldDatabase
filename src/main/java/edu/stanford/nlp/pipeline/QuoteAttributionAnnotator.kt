package edu.stanford.nlp.pipeline

import edu.stanford.nlp.coref.CorefCoreAnnotations
import edu.stanford.nlp.pipeline.Annotator
import edu.stanford.nlp.ling.CoreAnnotation
import edu.stanford.nlp.parser.nndep.DependencyParser
import edu.stanford.nlp.util.PropertiesUtils
import edu.stanford.nlp.util.Timing
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator
import edu.stanford.nlp.quoteattribution.QuoteAttributionUtils
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation
import edu.stanford.nlp.paragraphs.ParagraphAnnotator
import edu.stanford.nlp.quoteattribution.ChapterAnnotator
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.QMSieve
import edu.stanford.nlp.quoteattribution.Sieves.MSSieves.MSSieve
import edu.stanford.nlp.pipeline.QuoteAnnotator
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.MentionBeginAnnotation
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.EntityMentionIndexAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CanonicalEntityMentionIndexAnnotation
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.CanonicalMentionAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.CanonicalMentionBeginAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TokenBeginAnnotation
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.CanonicalMentionEndAnnotation
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.TrigramSieve
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.DependencyParseSieve
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.OneNameSentenceSieve
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.VocativeSieve
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.ParagraphEndQuoteClosestSieve
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.SupervisedSieve
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.ConversationalSieve
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.LooseConversationalSieve
import edu.stanford.nlp.quoteattribution.Sieves.QMSieves.ClosestMentionSieve
import edu.stanford.nlp.quoteattribution.Sieves.MSSieves.DeterministicSpeakerSieve
import edu.stanford.nlp.quoteattribution.Sieves.MSSieves.LooseConversationalSpeakerSieve
import edu.stanford.nlp.quoteattribution.Sieves.MSSieves.BaselineTopSpeakerSieve
import edu.stanford.nlp.quoteattribution.Sieves.MSSieves.MajoritySpeakerSieve
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.MentionAnnotation
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.MentionEndAnnotation
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.MentionTypeAnnotation
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.MentionSieveAnnotation
import edu.stanford.nlp.pipeline.QuoteAttributionAnnotator.SpeakerSieveAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.ParagraphIndexAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.BeforeAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.AfterAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TokenEndAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation
import edu.stanford.nlp.quoteattribution.Person
import edu.stanford.nlp.util.logging.Redwood.RedwoodChannels
import edu.stanford.nlp.util.logging.Redwood
import java.util.*

/**
 * This annotator connects quotes in a text to their speakers. It uses a two-stage process that first links quotes
 * to mentions and then mentions to speakers. Each stage consists in a series of sieves that each try to make
 * predictions on the quote or mentions that have not been linked by previous sieves.
 *
 *
 * The annotator will add the following annotations to each QuotationAnnotation:
 *
 *  * MentionAnnotation : the text of the mention
 *  * MentionBeginAnnotation : the beginning token index of the mention
 *  * MentionEndAnnotation : the end token index of the mention
 *  * MentionTypeAnnotation : the type of mention (pronoun, name, or animate noun)
 *  * MentionSieveAnnotation : the sieve that made the mention prediction
 *  * SpeakerAnnotation : the name of the speaker
 *  * SpeakerSieveAnnotation : the name of the sieve that made the speaker prediction
 *
 *
 *
 * The annotator has the following options:
 *
 *  * quote.attribution.charactersPath (required): path to file containing the character names, aliases,
 * and gender information.
 *  * quote.attribution.booknlpCoref (required): path to tokens file generated from
 * [book-nlp](https://github.com/dbamman/book-nlp) containing coref information.
 *  * quote.attribution.QMSieves: list of sieves to use in the quote to mention linking phase
 * (default=tri,dep,onename,voc,paraend,conv,sup,loose). More information about the sieves can be found at our
 * [website](stanfordnlp.github.io/CoreNLP/quoteattribution.html).
 *  * quote.attribution.MSSieves: list of sieves to use in the mention to speaker linking phase
 * (default=det,top).
 *  * quote.attribution.model: path to trained model file.
 *  * quote.attribution.familyWordsFile: path to file with family words list.
 *  * quote.attribution.animacyWordsFile: path to file with animacy words list.
 *  * quote.attribution.genderNamesFile: path to file with names list with gender information.
 *
 *
 * @author Grace Muzny, Michael Fang
 */
class QuoteAttributionAnnotator(props: Properties) : Annotator {
    class MentionAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class MentionBeginAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    class MentionEndAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    class MentionTypeAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class MentionSieveAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class SpeakerAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class SpeakerSieveAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class CanonicalMentionAnnotation : CoreAnnotation<String> {
        override val type: Class<String>
            get() = String::class.java
    }

    class CanonicalMentionBeginAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    class CanonicalMentionEndAnnotation : CoreAnnotation<Int> {
        override val type: Class<Int>
            get() = Int::class.java
    }

    // these paths go in the props file
    // cdm 2020: But they shouldn't be static, as they are set by properties per annotator.
    // todo [cdm 2020]: sort it out properly with default strings and actual value
    private var FAMILY_WORD_LIST = "edu/stanford/nlp/models/quoteattribution/family_words.txt"
    private var GENDER_WORD_LIST = "edu/stanford/nlp/models/quoteattribution/gender_filtered.txt"
    var buildCharacterMapPerAnnotation = false
    var useCoref = true
    val VERBOSE: Boolean

    // fields
    private val animacyList: Set<String>
    private val familyRelations: Set<String>
    private val genderMap: Map<String, Person.Gender>
    private var characterMap: HashMap<String, List<Person>>? = null
    private val qmSieveList: String
    private val msSieveList: String
    private val parser: DependencyParser

    init {
        VERBOSE = PropertiesUtils.getBool(props, "verbose", false)
        var timer: Timing? = null
        COREF_PATH = props.getProperty("booknlpCoref", null)
        if (COREF_PATH == null && VERBOSE) {
            log.err("Warning: no coreference map!")
        }
        MODEL_PATH = props.getProperty("modelPath", DEFAULT_MODEL_PATH)
        CHARACTERS_FILE = props.getProperty("charactersPath", null)
        if (CHARACTERS_FILE == null && VERBOSE) {
            log.err("Warning: no characters file!")
        }
        qmSieveList = props.getProperty("QMSieves", DEFAULT_QMSIEVES)
        msSieveList = props.getProperty("MSSieves", DEFAULT_MSSIEVES)
        if (VERBOSE) {
            timer = Timing()
            log.info("Loading QuoteAttribution coref [" + COREF_PATH + "]...")
            log.info("Loading QuoteAttribution characters [" + CHARACTERS_FILE + "]...")
        }
        // loading all our word lists
        FAMILY_WORD_LIST = props.getProperty("familyWordsFile", FAMILY_WORD_LIST)
        ANIMACY_WORD_LIST = props.getProperty("animacyWordsFile", ANIMACY_WORD_LIST)
        GENDER_WORD_LIST = props.getProperty("genderNamesFile", GENDER_WORD_LIST)
        familyRelations = QuoteAttributionUtils.readFamilyRelations(FAMILY_WORD_LIST)
        genderMap = QuoteAttributionUtils.readGenderedNounList(GENDER_WORD_LIST)
        animacyList = QuoteAttributionUtils.readAnimacyList(ANIMACY_WORD_LIST)
        if (characterMap != null) { // todo [cdm 2020]: Shouldn't this be testing against CHARACTERS_FILE?
            characterMap = QuoteAttributionUtils.readPersonMap(CHARACTERS_FILE)
        } else {
            buildCharacterMapPerAnnotation = true
        }
        // use Stanford CoreNLP coref to map mentions to canonical mentions
        // (at present this only determines requirements, CoreNLP coref is used if no booknlpCoref)
        useCoref = PropertiesUtils.getBool(props, "useCoref", useCoref)

        // setup dependency parser
        val DEPENDENCY_PARSER_MODEL =
            props.getProperty(Annotator.STANFORD_DEPENDENCIES + ".model", DependencyParser.DEFAULT_MODEL)
        val depparseProperties = PropertiesUtils.extractPrefixedProperties(props, Annotator.STANFORD_DEPENDENCIES + '.')
        parser = DependencyParser.loadFromModelFile(DEPENDENCY_PARSER_MODEL, depparseProperties)
        if (VERBOSE) {
            timer!!.stop("done.")
        }
    }

    /**
     * if no character list is provided, produce a list of person names from entity mentions annotation
     */
    fun entityMentionsToCharacterMap(annotation: Annotation) {
        characterMap = HashMap()
        for (entityMention in annotation.get(MentionsAnnotation::class.java)) {
            if (entityMention.get(NamedEntityTagAnnotation::class.java) == "PERSON") {
                // always store the replaceAll version of the string so that
                // whitespace does not have to match exactly to find the
                // mention later
                val entityMentionString = entityMention.toString().replace("\\s+".toRegex(), " ")
                val newPerson = Person(entityMentionString, "UNK", ArrayList())
                val newPersonList = listOf(newPerson)
                characterMap!![entityMentionString] = newPersonList
            }
        }
    }

    override fun annotate(annotation: Annotation) {
        // boolean perDocumentCharacterMap = false;
        if (buildCharacterMapPerAnnotation) {
            if (annotation.containsKey(MentionsAnnotation::class.java)) {
                // Put all mentions from this key that are NER type PERSON into the characterMap
                entityMentionsToCharacterMap(annotation)
            }
        }
        // 0. pre-preprocess the text with paragraph annotations
        // TODO: maybe move this out, definitely make it so that you can set paragraph breaks
        val propsPara = Properties()
        propsPara.setProperty("paragraphBreak", "one")
        val pa = ParagraphAnnotator(propsPara, false)
        pa.annotate(annotation)

        // 1. preprocess the text
        // a) setup coref
        val pronounCorefMap = QuoteAttributionUtils.setupCoref(COREF_PATH, characterMap, annotation)
        // log.info("Pronoun coref map is " + pronounCorefMap);

        //annotate chapter numbers in sentences. Useful for denoting chapter boundaries
        ChapterAnnotator().annotate(annotation)
        // to incorporate sentences across paragraphs
        QuoteAttributionUtils.addEnhancedSentences(annotation, parser)
        //annotate depparse of quote-removed sentences
        QuoteAttributionUtils.annotateForDependencyParse(annotation, parser)

        // 2. Quote->Mention annotation
        val qmSieves = getQMMapping(annotation, pronounCorefMap)
        for (sieveName in qmSieveList.split(",").toTypedArray()) {
            qmSieves[sieveName]!!.doQuoteToMention(annotation)
        }

        // 3. Mention->Speaker annotation
        val msSieves = getMSMapping(annotation, pronounCorefMap)
        for (sieveName in msSieveList.split(",").toTypedArray()) {
            msSieves[sieveName]!!.doMentionToSpeaker(annotation)
        }

        // see if any speaker's could be matched to a canonical entity mention
        for (quote in QuoteAnnotator.gatherQuotes(annotation)) {
            val firstSpeakerTokenIndex = quote.get<Int>(MentionBeginAnnotation::class.java)
            if (firstSpeakerTokenIndex != null) {
                val firstSpeakerToken = annotation.get(
                    TokensAnnotation::class.java
                )[firstSpeakerTokenIndex]
                val entityMentionIndex = firstSpeakerToken.get(
                    EntityMentionIndexAnnotation::class.java
                )
                if (entityMentionIndex != null) {
                    // set speaker string
                    val entityMention = annotation.get(MentionsAnnotation::class.java)[entityMentionIndex]
                    val canonicalEntityMentionIndex = entityMention.get(
                        CanonicalEntityMentionIndexAnnotation::class.java
                    )
                    if (canonicalEntityMentionIndex != null) {
                        val canonicalEntityMention = annotation.get(
                            MentionsAnnotation::class.java
                        )[canonicalEntityMentionIndex]
                        // add canonical entity mention info to quote
                        quote.set<String>(
                            CanonicalMentionAnnotation::class.java,
                            canonicalEntityMention.get(TextAnnotation::class.java)
                        )
                        // set first and last tokens of canonical entity mention
                        val canonicalEntityMentionTokens = canonicalEntityMention.get(
                            TokensAnnotation::class.java
                        )
                        val canonicalEntityMentionFirstToken = canonicalEntityMentionTokens[0]
                        val canonicalEntityMentionLastToken =
                            canonicalEntityMentionTokens[canonicalEntityMentionTokens.size - 1]
                        quote.set<Int>(
                            CanonicalMentionBeginAnnotation::class.java,
                            canonicalEntityMentionFirstToken.get(TokenBeginAnnotation::class.java)
                        )
                        quote.set<Int>(
                            CanonicalMentionEndAnnotation::class.java,
                            canonicalEntityMentionLastToken.get(TokenBeginAnnotation::class.java)
                        )
                    }
                }
            }
        }
    }

    private fun getQMMapping(doc: Annotation, pronounCorefMap: Map<Int, String>): Map<String, QMSieve> {
        val map: MutableMap<String, QMSieve> = HashMap()
        map["tri"] = TrigramSieve(doc, characterMap, pronounCorefMap, animacyList)
        map["dep"] = DependencyParseSieve(doc, characterMap, pronounCorefMap, animacyList)
        map["onename"] = OneNameSentenceSieve(doc, characterMap, pronounCorefMap, animacyList)
        map["voc"] = VocativeSieve(doc, characterMap, pronounCorefMap, animacyList)
        map["paraend"] = ParagraphEndQuoteClosestSieve(doc, characterMap, pronounCorefMap, animacyList)
        val ss = SupervisedSieve(doc, characterMap, pronounCorefMap, animacyList)
        ss.loadModel(MODEL_PATH)
        map["sup"] = ss
        map["conv"] = ConversationalSieve(doc, characterMap, pronounCorefMap, animacyList)
        map["loose"] = LooseConversationalSieve(doc, characterMap, pronounCorefMap, animacyList)
        map["closest"] = ClosestMentionSieve(doc, characterMap, pronounCorefMap, animacyList)
        return map
    }

    private fun getMSMapping(doc: Annotation, pronounCorefMap: Map<Int, String>): Map<String, MSSieve> {
        val map: MutableMap<String, MSSieve> = HashMap()
        map["det"] = DeterministicSpeakerSieve(doc, characterMap, pronounCorefMap, animacyList)
        map["loose"] = LooseConversationalSpeakerSieve(doc, characterMap, pronounCorefMap, animacyList)
        map["top"] = BaselineTopSpeakerSieve(
            doc, characterMap, pronounCorefMap, animacyList, genderMap,
            familyRelations
        )
        map["maj"] = MajoritySpeakerSieve(doc, characterMap, pronounCorefMap, animacyList)
        return map
    }

    override fun requirementsSatisfied(): Set<Class<out CoreAnnotation<*>?>> {
        return HashSet<Class<out CoreAnnotation<*>?>>(
            Arrays.asList(
                MentionAnnotation::class.java,
                MentionBeginAnnotation::class.java,
                MentionEndAnnotation::class.java,
                CanonicalMentionAnnotation::class.java,
                CanonicalMentionBeginAnnotation::class.java,
                CanonicalMentionEndAnnotation::class.java,
                MentionTypeAnnotation::class.java,
                MentionSieveAnnotation::class.java,
                SpeakerAnnotation::class.java,
                SpeakerSieveAnnotation::class.java,
                ParagraphIndexAnnotation::class.java
            )
        )
    }

    override fun requires(): Set<Class<out CoreAnnotation<*>?>> {
        val quoteAttributionRequirements: MutableSet<Class<out CoreAnnotation<*>?>> = HashSet(
            Arrays.asList(
                TextAnnotation::class.java,
                TokensAnnotation::class.java,
                SentencesAnnotation::class.java,
                CharacterOffsetBeginAnnotation::class.java,
                CharacterOffsetEndAnnotation::class.java,
                PartOfSpeechAnnotation::class.java,
                LemmaAnnotation::class.java,
                NamedEntityTagAnnotation::class.java,
                MentionsAnnotation::class.java,
                BeforeAnnotation::class.java,
                AfterAnnotation::class.java,
                TokenBeginAnnotation::class.java,
                TokenEndAnnotation::class.java,
                IndexAnnotation::class.java,
                OriginalTextAnnotation::class.java
            )
        )
        if (useCoref) quoteAttributionRequirements.add(CorefCoreAnnotations.CorefChainAnnotation::class.java)
        return quoteAttributionRequirements
    }

    companion object {
        private val log = Redwood.channels(QuoteAttributionAnnotator::class.java)

        // settings
        const val DEFAULT_QMSIEVES = "tri,dep,onename,voc,paraend,conv,sup,loose"
        const val DEFAULT_MSSIEVES = "det,top"
        const val DEFAULT_MODEL_PATH = "edu/stanford/nlp/models/quoteattribution/quoteattribution_model.ser"
        @JvmField
        var ANIMACY_WORD_LIST = "edu/stanford/nlp/models/quoteattribution/animate.unigrams.txt"
        var COREF_PATH: String? = null // used to be "" but seemed wrong
        var MODEL_PATH = "edu/stanford/nlp/models/quoteattribution/quoteattribution_model.ser"
        var CHARACTERS_FILE: String? = null // used to be "";
    }
}