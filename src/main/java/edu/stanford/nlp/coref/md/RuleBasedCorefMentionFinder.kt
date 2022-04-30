package edu.stanford.nlp.coref.md

import edu.stanford.nlp.trees.HeadFinder
import edu.stanford.nlp.coref.CorefProperties
import edu.stanford.nlp.coref.data.Dictionaries
import edu.stanford.nlp.coref.data.Mention
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.util.Generics
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation
import edu.stanford.nlp.semgraph.SemanticGraph
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation
import edu.stanford.nlp.trees.tregex.TregexPattern
import edu.stanford.nlp.trees.tregex.TregexMatcher
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.util.IntPair
import utils.matches
import java.util.*

class RuleBasedCorefMentionFinder(allowReparsing: Boolean, headFinder: HeadFinder?, lang: Locale?) :
    CorefMentionFinder() {
    constructor(headFinder: HeadFinder?, props: Properties?) : this(
        true,
        headFinder,
        CorefProperties.getLanguage(props)
    ) {
    }

    init {
        this.headFinder = headFinder
        this.allowReparsing = allowReparsing
        this.lang = lang
    }

    /** When mention boundaries are given  */
    fun filterPredictedMentions(
        allGoldMentions: List<List<Mention>?>,
        doc: Annotation,
        dict: Dictionaries?,
        props: Properties?
    ): List<List<Mention>> {
        val predictedMentions: MutableList<List<Mention>> = ArrayList()
        for (i in allGoldMentions.indices) {
            val s = doc.get(SentencesAnnotation::class.java)!![i]
            val goldMentions = allGoldMentions[i]
            val mentions: MutableList<Mention> = ArrayList()
            predictedMentions.add(mentions)
            mentions.addAll(goldMentions!!)
            findHead(s, mentions)

            // todo [cdm 2013]: This block seems to do nothing - the two sets are never used
            val mentionSpanSet = Generics.newHashSet<IntPair>()
            val namedEntitySpanSet = Generics.newHashSet<IntPair>()
            for (m in mentions) {
                mentionSpanSet.add(IntPair(m.startIndex, m.endIndex))
                if (m.headWord.get(NamedEntityTagAnnotation::class.java) != "O") {
                    namedEntitySpanSet.add(IntPair(m.startIndex, m.endIndex))
                }
            }
            setBarePlural(mentions)
        }
        removeSpuriousMentions(doc, predictedMentions, dict, CorefProperties.removeNestedMentions(props), lang)
        return predictedMentions
    }

    /** Main method of mention detection.
     * Extract all NP, PRP or NE, and filter out by manually written patterns.
     */
    override fun findMentions(doc: Annotation, dict: Dictionaries, props: Properties): List<MutableList<Mention>> {
        val predictedMentions: MutableList<MutableList<Mention>> = ArrayList()
        val neStrings = Generics.newHashSet<String>()
        val mentionSpanSetList: MutableList<Set<IntPair>> = Generics.newArrayList()
        val sentences = doc.get(
            SentencesAnnotation::class.java
        )!!

        // extract premarked mentions, NP/PRP, named entity, enumerations
        for (s in sentences) {
            val mentions: MutableList<Mention> = ArrayList()
            predictedMentions.add(mentions)
            val mentionSpanSet = Generics.newHashSet<IntPair>()
            val namedEntitySpanSet = Generics.newHashSet<IntPair>()
            extractPremarkedEntityMentions(s, mentions, mentionSpanSet, namedEntitySpanSet)
            extractNamedEntityMentions(s, mentions, mentionSpanSet, namedEntitySpanSet)
            extractNPorPRP(s, mentions, mentionSpanSet, namedEntitySpanSet)
            extractEnumerations(s, mentions, mentionSpanSet, namedEntitySpanSet)
            addNamedEntityStrings(s, neStrings, namedEntitySpanSet)
            mentionSpanSetList.add(mentionSpanSet)
        }
        if (CorefProperties.liberalMD(props)) {
            extractNamedEntityModifiers(sentences, mentionSpanSetList, predictedMentions, neStrings)
        }

        // find head
        var i = 0
        val sz = sentences.size
        while (i < sz) {
            findHead(sentences[i], predictedMentions[i])
            setBarePlural(predictedMentions[i])
            i++
        }

        // mention selection based on document-wise info
        if (lang === Locale.ENGLISH && !CorefProperties.liberalMD(props)) {
            removeSpuriousMentionsEn(doc, predictedMentions, dict)
        } else if (lang === Locale.CHINESE) {
            if (CorefProperties.liberalMD(props)) {
                removeSpuriousMentionsZhSimple(doc, predictedMentions, dict)
            } else {
                removeSpuriousMentionsZh(
                    doc, predictedMentions, dict,
                    CorefProperties.removeNestedMentions(props)
                )
            }
        }
        return predictedMentions
    }

    fun extractNPorPRP(
        s: CoreMap,
        mentions: MutableList<Mention>,
        mentionSpanSet: MutableSet<IntPair>,
        namedEntitySpanSet: Set<IntPair>?
    ) {
        val sent = s.get(TokensAnnotation::class.java)!!
        val tree = s.get(TreeAnnotation::class.java)
        tree!!.indexLeaves()
        val basicDependency = s.get(BasicDependenciesAnnotation::class.java)
        var enhancedDependency = s.get(EnhancedDependenciesAnnotation::class.java)
        if (enhancedDependency == null) {
            enhancedDependency = s.get(BasicDependenciesAnnotation::class.java)
        }
        val tgrepPattern = npOrPrpMentionPattern
        val matcher = tgrepPattern.matcher(tree)
        while (matcher.find()) {
            val t = matcher.match
            val mLeaves = t.getLeaves<Tree>()
            val beginIdx = (mLeaves[0].label() as CoreLabel).get(IndexAnnotation::class.java)!! - 1
            val endIdx = (mLeaves[mLeaves.size - 1].label() as CoreLabel).get(
                IndexAnnotation::class.java
            )!!
            //if (",".equals(sent.get(endIdx-1).word())) { endIdx--; } // try not to have span that ends with ,
            val mSpan = IntPair(beginIdx, endIdx)
            if (!mentionSpanSet.contains(mSpan) && (lang === Locale.CHINESE || !insideNE(mSpan, namedEntitySpanSet))) {
//      if(!mentionSpanSet.contains(mSpan) && (!insideNE(mSpan, namedEntitySpanSet) || t.value().startsWith("PRP")) ) {
                val dummyMentionId = -1
                val m = Mention(
                    dummyMentionId,
                    beginIdx,
                    endIdx,
                    sent,
                    basicDependency,
                    enhancedDependency,
                    ArrayList(sent.subList(beginIdx, endIdx)),
                    t
                )
                mentions.add(m)
                mentionSpanSet.add(mSpan)

//        if(m.originalSpan.size() > 1) {
//          boolean isNE = true;
//          for(CoreLabel cl : m.originalSpan) {
//            if(!cl.tag().startsWith("NNP")) isNE = false;
//          }
//          if(isNE) {
//            namedEntitySpanSet.add(mSpan);
//          }
//        }
            }
        }
    }

    /** Filter out all spurious mentions
     */
    public override fun removeSpuriousMentionsEn(
        doc: Annotation,
        predictedMentions: List<MutableList<Mention>>,
        dict: Dictionaries
    ) {
        val standAlones: Set<String> = HashSet()
        val sentences = doc.get(
            SentencesAnnotation::class.java
        )!!
        for (i in predictedMentions.indices) {
            val s = sentences[i]
            val mentions = predictedMentions[i]
            val tree = s.get(TreeAnnotation::class.java)
            val sent = s.get(
                TokensAnnotation::class.java
            )!!
            val remove = Generics.newHashSet<Mention>()
            for (m in mentions) {
                val headPOS = m.headWord.get(PartOfSpeechAnnotation::class.java)!!
                val headNE = m.headWord.get(NamedEntityTagAnnotation::class.java)!!
                // pleonastic it
                if (isPleonastic(m, tree)) {
                    remove.add(m)
                }

                // non word such as 'hmm'
                if (dict.nonWords.contains(m.headString)) remove.add(m)

                // quantRule : not starts with 'any', 'all' etc
                if (m.originalSpan.size > 0) {
                    val firstWord = m.originalSpan[0].get(TextAnnotation::class.java)!!
                        .lowercase()
                    if (firstWord.matches("none|no|nothing|not")) {
                        remove.add(m)
                    }
                    //          if(dict.quantifiers.contains(firstWord)) remove.add(m);
                }

                // partitiveRule
                if (partitiveRule(m, sent, dict)) {
                    remove.add(m)
                }

                // bareNPRule
                if (headPOS == "NN" && !dict.temporals.contains(m.headString)
                    && (m.originalSpan.size == 1 || m.originalSpan[0].get(PartOfSpeechAnnotation::class.java) == "JJ")
                ) {
                    remove.add(m)
                }

                // remove generic rule
//          if(m.generic==true) remove.add(m);
                if (m.headString == "%") {
                    remove.add(m)
                }
                if (headNE == "PERCENT" || headNE == "MONEY") {
                    remove.add(m)
                }

                // adjective form of nations
                // the [American] policy -> not mention
                // speak in [Japanese] -> mention
                // check if the mention is noun and the next word is not noun
                if (dict.isAdjectivalDemonym(m.spanToString())) {
                    remove.add(m)
                }

                // stop list (e.g., U.S., there)
                if (inStopList(m)) remove.add(m)
            }

            // nested mention with shared headword (except apposition, enumeration): pick larger one
            for (m1 in mentions) {
                for (m2 in mentions) {
                    if (m1 === m2 || remove.contains(m1) || remove.contains(m2)) continue
                    if (m1.sentNum == m2.sentNum && m1.headWord === m2.headWord && m2.insideIn(m1)) {
                        if (m2.endIndex < sent.size && (sent[m2.endIndex].get(
                                PartOfSpeechAnnotation::class.java
                            ) == "," || sent[m2.endIndex].get(
                                PartOfSpeechAnnotation::class.java
                            ) == "CC")
                        ) {
                            continue
                        }
                        remove.add(m2)
                    }
                }
            }
            mentions.removeAll(remove)
        }
    }

    companion object {
        protected fun setBarePlural(mentions: List<Mention>) {
            for (m in mentions) {
                val pos = m.headWord.get(PartOfSpeechAnnotation::class.java)!!
                if (m.originalSpan.size == 1 && pos == "NNS") m.generic = true
            }
        }

        protected fun extractNamedEntityMentions(
            s: CoreMap,
            mentions: MutableList<Mention>,
            mentionSpanSet: MutableSet<IntPair>,
            namedEntitySpanSet: MutableSet<IntPair>
        ) {
            val sent = s.get(
                TokensAnnotation::class.java
            )!!
            val basicDependency = s.get(BasicDependenciesAnnotation::class.java)
            var enhancedDependency = s.get(EnhancedDependenciesAnnotation::class.java)
            if (enhancedDependency == null) {
                enhancedDependency = s.get(BasicDependenciesAnnotation::class.java)
            }
            var preNE = "O"
            var beginIndex = -1
            for (w in sent) {
                val nerString = w.ner()
                if (nerString != preNE) {
                    var endIndex = w.get(IndexAnnotation::class.java)!! - 1
                    if (!preNE.matches("O|QUANTITY|CARDINAL|PERCENT|DATE|DURATION|TIME|SET")) {
                        if (w.get(TextAnnotation::class.java) == "'s" && w.tag() == "POS") {
                            endIndex++
                        }
                        val mSpan = IntPair(beginIndex, endIndex)
                        // Need to check if beginIndex < endIndex because, for
                        // example, there could be a 's mislabeled by the NER and
                        // attached to the previous NER by the earlier heuristic
                        if (beginIndex < endIndex && !mentionSpanSet.contains(mSpan)) {
                            val dummyMentionId = -1
                            val m = Mention(
                                dummyMentionId,
                                beginIndex,
                                endIndex,
                                sent,
                                basicDependency,
                                enhancedDependency,
                                ArrayList(sent.subList(beginIndex, endIndex))
                            )
                            mentions.add(m)
                            mentionSpanSet.add(mSpan)
                            namedEntitySpanSet.add(mSpan)
                        }
                    }
                    beginIndex = endIndex
                    preNE = nerString
                }
            }
            // NE at the end of sentence
            if (!preNE.matches("O|QUANTITY|CARDINAL|PERCENT|DATE|DURATION|TIME|SET")) {
                val mSpan = IntPair(beginIndex, sent.size)
                if (!mentionSpanSet.contains(mSpan)) {
                    val dummyMentionId = -1
                    val m = Mention(
                        dummyMentionId,
                        beginIndex,
                        sent.size,
                        sent,
                        basicDependency,
                        enhancedDependency,
                        ArrayList(sent.subList(beginIndex, sent.size))
                    )
                    mentions.add(m)
                    mentionSpanSet.add(mSpan)
                    namedEntitySpanSet.add(mSpan)
                }
            }
        }

        private fun removeSpuriousMentionsZhSimple(
            doc: Annotation,
            predictedMentions: List<MutableList<Mention>>, dict: Dictionaries
        ) {
            for (i in predictedMentions.indices) {
                val mentions = predictedMentions[i]
                val remove = Generics.newHashSet<Mention>()
                for (m in mentions) {
                    if (m.originalSpan.size == 1 && m.headWord.tag() == "CD") {
                        remove.add(m)
                    }
                    if (m.spanToString().contains("ｑｕｏｔ")) {
                        remove.add(m)
                    }
                }
                mentions.removeAll(remove)
            }
        }
    }
}