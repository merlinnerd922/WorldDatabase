package edu.stanford.nlp.coref.data

import edu.stanford.nlp.util.Pair.Companion.makePair
import edu.stanford.nlp.util.logging.Redwood.RedwoodChannels
import edu.stanford.nlp.util.logging.Redwood
import kotlin.Throws
import edu.stanford.nlp.classify.LogisticClassifier
import edu.stanford.nlp.trees.HeadFinder
import java.lang.RuntimeException
import edu.stanford.nlp.coref.CorefUtils
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation
import java.lang.StringBuilder
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation
import edu.stanford.nlp.semgraph.SemanticGraph
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation
import edu.stanford.nlp.semgraph.SemanticGraphEdge
import edu.stanford.nlp.trees.UniversalEnglishGrammaticalRelations
import edu.stanford.nlp.ling.IndexedWord
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.UtteranceAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.ParagraphAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.UseMarkedDiscourseAnnotation
import edu.stanford.nlp.math.NumberMatchingRegex
import edu.stanford.nlp.coref.CorefRules
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.trees.GrammaticalRelation
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.util.*
import edu.stanford.nlp.util.Pair
import java.lang.Exception
import java.util.*

/**
 * Coref document preprocessor.
 * @author Heeyoung Lee
 * @author Kevin Clark
 */
object DocumentPreprocessor {
    /** A logger for this class  */
    private val log = Redwood.channels(DocumentPreprocessor::class.java)

    /**
     * Fill missing information in document including mention ID, mention attributes, syntactic relation, etc.
     *
     * @throws Exception
     */
    @JvmStatic
    @Throws(Exception::class)
    fun preprocess(
        doc: Document,
        dict: Dictionaries,
        singletonPredictor: LogisticClassifier<String, String>,
        headFinder: HeadFinder
    ) {
        // assign mention IDs, find twin mentions, fill mention positions, sentNum, headpositions
        initializeMentions(doc, dict, singletonPredictor, headFinder)

        // mention reordering
        mentionReordering(doc, headFinder)

        // find syntactic information
        fillSyntacticInfo(doc)

        // process discourse (speaker info etc)
        setParagraphAnnotation(doc)
        processDiscourse(doc, dict)

        // initialize cluster info
        initializeClusters(doc)

        // extract gold clusters if we have
        if (doc.goldMentions != null) {
            extractGoldClusters(doc)
            var foundGoldCount = 0
            for (g in doc.goldMentionsByID!!.values) {
                if (g.hasTwin) foundGoldCount++
            }
            Redwood.log(
                "debug-md", "# of found gold mentions: " + foundGoldCount +
                        " / # of gold mentions: " + doc.goldMentionsByID!!.size
            )
        }

        // assign mention numbers
        assignMentionNumbers(doc)
    }

    /** Extract gold coref cluster information.  */
    fun extractGoldClusters(doc: Document) {
        doc.goldCorefClusters = hashMapOf();
        for (mentions in doc.goldMentions!!) {
            for (m in mentions) {
                val id = m.goldCorefClusterID
                if (id == -1) {
                    throw RuntimeException("No gold info")
                }
                var c = doc.goldCorefClusters!!.get(id)
                if (c == null) {
                    c = CorefCluster(id)
                    doc.goldCorefClusters!![id] = c
                }
                c.corefMentions.add(m)
            }
        }
    }

    private fun assignMentionNumbers(document: Document) {
        val mentionsList = CorefUtils.getSortedMentions(document)
        for (i in mentionsList.indices) {
            mentionsList[i].mentionNum = i
        }
    }

    @Throws(Exception::class)
    private fun mentionReordering(doc: Document, headFinder: HeadFinder) {
        val mentions = doc.orderedMentions
        val sentences = doc.annotation!!.get(
            SentencesAnnotation::class.java
        )!!
        for (i in sentences.indices) {
            val mentionsInSent = mentions!![i]
            mentions.set(i, mentionReorderingBySpan(mentionsInSent))
        }
    }

    internal fun getHeadIndex(t: Tree, headFinder: HeadFinder?): Int {
        // The trees passed in do not have the CoordinationTransformer
        // applied, but that just means the SemanticHeadFinder results are
        // slightly worse.
        val ht = t.headTerminal(headFinder) ?: return -1
        // temporary: a key which is matched to nothing
        val l = ht.label() as CoreLabel
        return l.get(IndexAnnotation::class.java)!!
    }

    private fun mentionReorderingBySpan(mentionsInSent: List<Mention>): List<Mention> {
        val ordering =
            TreeSet<Mention> { m1, m2 -> if (m1.appearEarlierThan(m2)) -1 else if (m2.appearEarlierThan(m1)) 1 else 0 }
        ordering.addAll(mentionsInSent)
        return Generics.newArrayList(ordering)
    }

    private fun fillSyntacticInfo(doc: Document) {
        val mentions = doc.orderedMentions
        val sentences = doc.annotation!!.get(
            SentencesAnnotation::class.java
        )!!
        for (i in sentences.indices) {
            val mentionsInSent = mentions!![i]
            findSyntacticRelationsFromDependency(mentionsInSent)
        }
    }

    /** assign mention IDs, find twin mentions, fill mention positions, initialize coref clusters, etc
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun initializeMentions(
        doc: Document,
        dict: Dictionaries,
        singletonPredictor: LogisticClassifier<String, String>,
        headFinder: HeadFinder
    ) {
        val hasGold = doc.goldMentions != null
        assignMentionIDs(doc)
        if (hasGold) findTwinMentions(doc, true)
        fillMentionInfo(doc, dict, singletonPredictor, headFinder)
        doc.allPositions =
            HashMap(doc.positions) // allPositions retain all mentions even after postprocessing
    }

    private fun assignMentionIDs(doc: Document) {
        val hasGold = doc.goldMentions != null
        var maxID = 0
        if (hasGold) {
            for (golds in doc.goldMentions!!) {
                for (g in golds) {
                    g.mentionID = maxID++
                }
            }
        }
        for (predicted in doc.orderedMentions!!) {
            for (p in predicted) {
                p.mentionID = maxID++
            }
        }
    }

    /** Mark twin mentions in gold and predicted mentions  */
    internal fun findTwinMentions(doc: Document, strict: Boolean) {
        if (strict) findTwinMentionsStrict(doc) else findTwinMentionsRelaxed(doc)
    }

    /** Mark twin mentions: All mention boundaries should be matched  */
    private fun findTwinMentionsStrict(doc: Document) {
        for (sentNum in doc.goldMentions!!.indices) {
            val golds = doc.goldMentions!![sentNum]
            val predicts = doc.orderedMentions!![sentNum]

            // For CoNLL training there are some documents with gold mentions with the same position offsets
            // See /scr/nlp/data/conll-2011/v2/data/train/data/english/annotations/nw/wsj/09/wsj_0990.v2_auto_conll
            //  (Packwood - Roth)
            val goldMentionPositions = CollectionValuedMap<IntPair, Mention?>()
            for (g in golds) {
                val ip = IntPair(g.startIndex, g.endIndex)
                if (goldMentionPositions.containsKey(ip)) {
                    val existingMentions = StringBuilder()
                    for (eg in goldMentionPositions[ip]!!) {
                        if (existingMentions.length > 0) {
                            existingMentions.append(",")
                        }
                        existingMentions.append(eg!!.mentionID)
                    }
                    Redwood.log(
                        "debug-preprocessor", "WARNING: gold mentions with the same offsets: " + ip
                                + " mentions=" + g.mentionID + "," + existingMentions + ", " + g.spanToString()
                    )
                }
                //assert(!goldMentionPositions.containsKey(ip));
                goldMentionPositions.add(IntPair(g.startIndex, g.endIndex), g)
            }
            for (p in predicts) {
                val pos = IntPair(p.startIndex, p.endIndex)
                if (goldMentionPositions.containsKey(pos)) {
                    val cm = goldMentionPositions[pos]
                    var minId = Int.MAX_VALUE
                    var g: Mention? = null
                    for (m in cm!!) {
                        if (m!!.mentionID < minId) {
                            g = m
                            minId = m.mentionID
                        }
                    }
                    if (cm.size == 1) {
                        goldMentionPositions.remove(pos)
                    } else {
                        cm.remove(g)
                    }
                    p.mentionID = g!!.mentionID
                    p.hasTwin = true
                    g.hasTwin = true
                }
            }
        }
    }

    /** Mark twin mentions: heads of the mentions are matched  */
    private fun findTwinMentionsRelaxed(doc: Document) {
        for (sentNum in doc.goldMentions!!.indices) {
            val golds = doc.goldMentions!![sentNum]
            val predicts = doc.orderedMentions!![sentNum]
            val goldMentionPositions = Generics.newHashMap<IntPair, Mention>()
            val goldMentionHeadPositions = Generics.newHashMap<Int, LinkedList<Mention?>>()
            for (g in golds) {
                goldMentionPositions[IntPair(g.startIndex, g.endIndex)] = g
                if (!goldMentionHeadPositions.containsKey(g.headIndex)) {
                    goldMentionHeadPositions[g.headIndex] = LinkedList()
                }
                goldMentionHeadPositions[g.headIndex]!!.add(g)
            }
            val remains: MutableList<Mention> = ArrayList()
            for (p in predicts) {
                val pos = IntPair(p.startIndex, p.endIndex)
                if (goldMentionPositions.containsKey(pos)) {
                    val g = goldMentionPositions[pos]
                    p.mentionID = g!!.mentionID
                    p.hasTwin = true
                    g.hasTwin = true
                    goldMentionHeadPositions[g.headIndex]!!.remove(g)
                    if (goldMentionHeadPositions[g.headIndex]!!.isEmpty()) {
                        goldMentionHeadPositions.remove(g.headIndex)
                    }
                } else remains.add(p)
            }
            for (r in remains) {
                if (goldMentionHeadPositions.containsKey(r.headIndex)) {
                    val g = goldMentionHeadPositions[r.headIndex]!!.poll()
                    r.mentionID = g!!.mentionID
                    r.hasTwin = true
                    g.hasTwin = true
                    if (goldMentionHeadPositions[g.headIndex]!!.isEmpty()) {
                        goldMentionHeadPositions.remove(g.headIndex)
                    }
                }
            }
        }
    }

    /** initialize several variables for mentions
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun fillMentionInfo(
        doc: Document, dict: Dictionaries,
        singletonPredictor: LogisticClassifier<String, String>, headFinder: HeadFinder
    ) {
        val sentences = doc.annotation!!.get(
            SentencesAnnotation::class.java
        )!!
        for (i in doc.orderedMentions!!.indices) {
            val sentence = sentences[i]
            for (j in doc.orderedMentions!![i].indices) {
                val m = doc.orderedMentions!![i][j]
                doc.predictedMentionsByID[m.mentionID] = m // mentionsByID
                val pos = IntTuple(2)
                pos[0] = i
                pos[1] = j
                doc.positions[m] = pos // positions
                m.sentNum = i // sentNum
                val headPosition = IntTuple(2)
                headPosition[0] = i
                headPosition[1] = m.headIndex
                doc.mentionheadPositions[headPosition] = m // headPositions
                m.contextParseTree = sentence.get(TreeAnnotation::class.java)
                //        m.sentenceWords = sentence.get(TokensAnnotation.class);
                m.basicDependency = sentence.get(BasicDependenciesAnnotation::class.java)
                m.enhancedDependency = sentence.get(EnhancedDependenciesAnnotation::class.java)
                if (m.enhancedDependency == null) {
                    m.enhancedDependency = sentence.get(BasicDependenciesAnnotation::class.java)
                }

                // mentionSubTree (highest NP that has the same head) if constituency tree available
                if (m.contextParseTree != null) {
                    val headTree = m.contextParseTree.getLeaves<Tree>()[m.headIndex]
                        ?: throw RuntimeException("Missing head tree for a mention!")
                    var t = headTree
                    while (t.parent(m.contextParseTree).also { t = it } != null) {
                        if (t.headTerminal(headFinder) === headTree && t.value() == "NP") {
                            m.mentionSubTree = t
                        } else if (m.mentionSubTree != null) {
                            break
                        }
                    }
                    if (m.mentionSubTree == null) {
                        m.mentionSubTree = headTree
                    }
                }
                m.process(dict, null, singletonPredictor)
            }
        }
        val hasGold = doc.goldMentions != null
        if (hasGold) {
            doc.goldMentionsByID = hashMapOf()
            for ((sentNum, golds) in doc.goldMentions!!.withIndex()) {
                for (g in golds) {
                    doc.goldMentionsByID!![g.mentionID] = g
                    g.sentNum = sentNum
                }
            }
        }
    }

    private fun findSyntacticRelationsFromDependency(orderedMentions: List<Mention>) {
        if (orderedMentions.size == 0) return
        markListMemberRelation(orderedMentions)
        val dependency = orderedMentions[0].enhancedDependency

        // apposition
        val appos = Generics.newHashSet<Pair<Int, Int>>()
        val appositions = dependency.findAllRelns(UniversalEnglishGrammaticalRelations.APPOSITIONAL_MODIFIER)
        for (edge in appositions) {
            val sIdx = edge.source.index() - 1
            val tIdx = edge.target.index() - 1
            appos.add(makePair(sIdx, tIdx))
        }
        markMentionRelation(orderedMentions, appos, "APPOSITION")

        // predicate nominatives
        val preNomi = Generics.newHashSet<Pair<Int, Int>>()
        val copula = dependency.findAllRelns(UniversalEnglishGrammaticalRelations.COPULA)
        for (edge in copula) {
            val source = edge.source
            var target = dependency.getChildWithReln(source, UniversalEnglishGrammaticalRelations.NOMINAL_SUBJECT)
            if (target == null) target =
                dependency.getChildWithReln(source, UniversalEnglishGrammaticalRelations.CLAUSAL_SUBJECT)
            // TODO
            if (target == null) continue

            // to handle relative clause: e.g., Tim who is a student,
            if (target.tag().startsWith("W")) {
                val parent = dependency.getParent(source)
                if (parent != null && dependency.reln(
                        parent,
                        source
                    ) == UniversalEnglishGrammaticalRelations.RELATIVE_CLAUSE_MODIFIER
                ) {
                    target = parent
                }
            }
            val sIdx = source.index() - 1
            val tIdx = target.index() - 1
            preNomi.add(makePair(tIdx, sIdx))
        }
        markMentionRelation(orderedMentions, preNomi, "PREDICATE_NOMINATIVE")


        // relative pronouns  TODO
        val relativePronounPairs = Generics.newHashSet<Pair<Int, Int>>()
        markMentionRelation(orderedMentions, relativePronounPairs, "RELATIVE_PRONOUN")
    }

    private fun initializeClusters(doc: Document) {
        for (predicted in doc.orderedMentions!!) {
            for (p in predicted) {
                doc.corefClusters[p.mentionID] = CorefCluster(p.mentionID, Generics.newHashSet(Arrays.asList(p)))
                p.corefClusterID = p.mentionID
            }
        }
        val hasGold = doc.goldMentions != null
        if (hasGold) {
            for (golds in doc.goldMentions!!) {
                for (g in golds) {
                    doc.goldMentionsByID!![g.mentionID] = g
                }
            }
        }
    }

    /** Find document type: Conversation or article   */
    private fun findDocType(doc: Document): Document.DocType {
        var speakerChange = false
        for (sent in doc.annotation!!.get(SentencesAnnotation::class.java)!!) {
            for (w in sent.get(TokensAnnotation::class.java)!!) {
                val utterIndex = w.get(UtteranceAnnotation::class.java)!!
                if (utterIndex != 0) speakerChange = true
                if (speakerChange && utterIndex == 0) return Document.DocType.ARTICLE
                if (doc.maxUtter < utterIndex) doc.maxUtter = utterIndex
            }
        }
        return if (!speakerChange) Document.DocType.ARTICLE else Document.DocType.CONVERSATION
        // in conversation, utter index keep increasing.
    }

    /** Set paragraph index  */
    private fun setParagraphAnnotation(doc: Document) {
        var paragraphIndex = 0
        var previousOffset = -10
        for (sent in doc.annotation!!.get(SentencesAnnotation::class.java)!!) {
            for (w in sent.get(TokensAnnotation::class.java)!!) {
                if (w.containsKey(CharacterOffsetBeginAnnotation::class.java)) {
                    if (w.get(CharacterOffsetBeginAnnotation::class.java)!! > previousOffset + 2) paragraphIndex++
                    w.set(ParagraphAnnotation::class.java, paragraphIndex)
                    previousOffset = w.get(CharacterOffsetEndAnnotation::class.java)!!
                } else {
                    w.set(ParagraphAnnotation::class.java, -1)
                }
            }
        }
        for (l in doc.orderedMentions!!) {
            for (m in l) {
                m.paragraph = m.headWord.get(ParagraphAnnotation::class.java)!!
            }
        }
        doc.numParagraph = paragraphIndex
    }

    /** Process discourse information  */
    internal fun processDiscourse(doc: Document, dict: Dictionaries) {
        val useMarkedDiscourse = doc.annotation!!.get(UseMarkedDiscourseAnnotation::class.java)
        if (useMarkedDiscourse == null || !useMarkedDiscourse) {
            for (l in doc.annotation!!.get(TokensAnnotation::class.java)!!) {
                l.remove(CoreAnnotations.SpeakerAnnotation::class.java)
                l.remove(UtteranceAnnotation::class.java)
            }
        }
        setUtteranceAndSpeakerAnnotation(doc)
        //    markQuotations(this.annotation.get(CoreAnnotations.SentencesAnnotation.class), false);

        // mention utter setting
        for (m in doc.predictedMentionsByID.values) {
            m.utter = m.headWord.get(UtteranceAnnotation::class.java)!!
        }
        doc.docType = findDocType(doc)
        findSpeakers(doc, dict)
        val debug = false
        if (debug) {
            for (sent in doc.annotation!!.get(SentencesAnnotation::class.java)!!) {
                for (cl in sent.get(TokensAnnotation::class.java)!!) {
                    log.info(
                        "   " + cl.word() + "-" + cl.get(
                            UtteranceAnnotation::class.java
                        ) + "-" + cl.get(CoreAnnotations.SpeakerAnnotation::class.java)
                    )
                }
            }
            for (utter in doc.speakers.keys) {
                val speakerID = doc.speakers[utter]
                log.info("utterance: $utter")
                log.info("speakers value: $speakerID")
                log.info(
                    "mention for it: " +
                            if (NumberMatchingRegex.isDecimalInteger(speakerID)) doc.predictedMentionsByID[doc.speakers[utter]!!.toInt()] else "no mention for this speaker yet"
                )
            }
            log.info("AA SPEAKERS: " + doc.speakers)
        }

        // build 'speakerInfo' from 'speakers'
        for (utter in doc.speakers.keys) {
            val speaker = doc.speakers[utter]
            var speakerInfo = doc.speakerInfoMap[speaker]
            if (speakerInfo == null) {
                doc.speakerInfoMap[speaker] = SpeakerInfo(speaker).also { speakerInfo = it }
            }
        }
        if (debug) {
            log.info("BB SPEAKER INFO MAP: " + doc.speakerInfoMap)
        }

        // mention -> to its speakerID: m.headWord.get(SpeakerAnnotation.class)
        // speakerID -> more info: speakerInfoMap.get(speakerID)
        // if exists, set(mentionID, its speakerID pair): speakerPairs

        // for speakerInfo with real speaker name, find corresponding mention by strict/loose matching
        val speakerConversion = Generics.newHashMap<String?, Int>()
        for (speaker in doc.speakerInfoMap.keys) {
            val speakerInfo = doc.speakerInfoMap[speaker]
            if (speakerInfo!!.hasRealSpeakerName()) {   // do only for real name speaker, not mention ID
                var found = false
                for (m in doc.predictedMentionsByID.values) {
                    if (CorefRules.mentionMatchesSpeaker(m, speakerInfo, true)) {
                        speakerConversion[speaker] = m.mentionID
                        found = true
                        break
                    }
                }
                if (!found) {
                    for (m in doc.predictedMentionsByID.values) {
                        if (CorefRules.mentionMatchesSpeaker(m, speakerInfo, false)) {
                            speakerConversion[speaker] = m.mentionID
                            break
                        }
                    }
                }
            }
        }
        if (debug) log.info("CC speaker conversion: $speakerConversion")

        // convert real name speaker to speaker mention id
        for (utter in doc.speakers.keys) {
            val speaker = doc.speakers[utter]
            if (speakerConversion.containsKey(speaker)) {
                val speakerID = speakerConversion[speaker]!!
                doc.speakers[utter] = Integer.toString(speakerID)
            }
        }
        for (speaker in speakerConversion.keys) {
            doc.speakerInfoMap[Integer.toString(speakerConversion[speaker]!!)] = doc.speakerInfoMap[speaker]
            doc.speakerInfoMap.remove(speaker)
        }

        // fix SpeakerAnnotation
        for (cl in doc.annotation!!.get(TokensAnnotation::class.java)!!) {
            val utter = cl.get(UtteranceAnnotation::class.java)!!
            if (doc.speakers.containsKey(utter)) {
                cl.set(CoreAnnotations.SpeakerAnnotation::class.java, doc.speakers[utter]!!)
            }
        }

        // find speakerPairs
        for (m in doc.predictedMentionsByID.values) {
            val speaker = m.headWord.get(CoreAnnotations.SpeakerAnnotation::class.java)!!
            if (debug) log.info("DD: $speaker")
            // if this is not a CoNLL doc, don't treat a number username as a speakerMentionID
            // conllDoc == null indicates not a CoNLL doc
            if (doc.conllDoc != null) {
                if (NumberMatchingRegex.isDecimalInteger(speaker)) {
                    val speakerMentionID = speaker.toInt()
                    doc.speakerPairs.add(Pair(m.mentionID, speakerMentionID))
                }
            }
        }
        if (debug) {
            log.info("==========================================================================")
            for (utter in doc.speakers.keys) {
                val speakerID = doc.speakers[utter]
                log.info("utterance: $utter")
                log.info("speakers value: $speakerID")
                log.info(
                    "mention for it: " +
                            if (NumberMatchingRegex.isDecimalInteger(speakerID)) doc.predictedMentionsByID[doc.speakers[utter]!!.toInt()] else "no mention for this speaker yet"
                )
            }
            log.info(doc.speakers)
        }
    }

    private fun setUtteranceAndSpeakerAnnotation(doc: Document) {
        doc.speakerInfoGiven = false
        var utterance = 0
        var outsideQuoteUtterance = 0 // the utterance of outside of quotation
        var insideQuotation = false
        val tokens = doc.annotation!!.get(
            TokensAnnotation::class.java
        )!!
        var preSpeaker = if (tokens.size > 0) tokens[0].get(CoreAnnotations.SpeakerAnnotation::class.java) else null
        for (l in tokens) {
            val curSpeaker = l.get(CoreAnnotations.SpeakerAnnotation::class.java)
            val w = l.get(TextAnnotation::class.java)!!
            if (curSpeaker != null && curSpeaker != "-") doc.speakerInfoGiven = true
            val speakerChange = doc.speakerInfoGiven && curSpeaker != null && curSpeaker != preSpeaker
            val quoteStart = w == "``" || !insideQuotation && w == "\""
            val quoteEnd = w == "''" || insideQuotation && w == "\""
            if (speakerChange) {
                if (quoteStart) {
                    utterance = doc.maxUtter + 1
                    outsideQuoteUtterance = utterance + 1
                } else {
                    utterance = doc.maxUtter + 1
                    outsideQuoteUtterance = utterance
                }
                preSpeaker = curSpeaker
            } else {
                if (quoteStart) {
                    utterance = doc.maxUtter + 1
                }
            }
            if (quoteEnd) {
                utterance = outsideQuoteUtterance
                insideQuotation = false
            }
            if (doc.maxUtter < utterance) doc.maxUtter = utterance
            l.set(UtteranceAnnotation::class.java, utterance)
            if (quoteStart) l.set(
                UtteranceAnnotation::class.java,
                outsideQuoteUtterance
            ) // quote start got outside utterance idx
            val noSpeakerInfo = (!l.containsKey(CoreAnnotations.SpeakerAnnotation::class.java)
                    || l.get(CoreAnnotations.SpeakerAnnotation::class.java) == "" || l.get(
                CoreAnnotations.SpeakerAnnotation::class.java
            )!!.startsWith("PER"))
            if (noSpeakerInfo || insideQuotation) {
                l.set(CoreAnnotations.SpeakerAnnotation::class.java, "PER$utterance")
            }
            if (quoteStart) insideQuotation = true
        }
    }

    /** Speaker extraction  */
    private fun findSpeakers(doc: Document, dict: Dictionaries) {
        val useMarkedDiscourseBoolean = doc.annotation!![UseMarkedDiscourseAnnotation::class.java]
        val useMarkedDiscourse = useMarkedDiscourseBoolean ?: false
        if (!useMarkedDiscourse) {
            if (doc.docType === Document.DocType.CONVERSATION) findSpeakersInConversation(
                doc,
                dict
            ) else if (doc.docType === Document.DocType.ARTICLE) findSpeakersInArticle(doc, dict)
        }
        for (sent in doc.annotation!![SentencesAnnotation::class.java]!!) {
            for (w in sent.get(TokensAnnotation::class.java)!!) {
                val utterIndex = w.get(UtteranceAnnotation::class.java)!!
                if (!doc.speakers.containsKey(utterIndex)) {
                    doc.speakers[utterIndex] = w[CoreAnnotations.SpeakerAnnotation::class.java] as String
                }
            }
        }
    }

    private fun findSpeakersInArticle(doc: Document, dict: Dictionaries) {
        val sentences = doc.annotation!!.get(
            SentencesAnnotation::class.java
        )!!
        var beginQuotation: IntPair? = null
        var endQuotation: IntPair? = null
        var insideQuotation = false
        var utterNum = -1
        for (i in sentences.indices) {
            val coreMap = sentences[i]
            val sent = coreMap.get(
                TokensAnnotation::class.java
            )!!
            for (j in sent.indices) {
                val utterIndex = sent[j].get(UtteranceAnnotation::class.java)!!
                if (utterIndex != 0 && !insideQuotation) {
                    utterNum = utterIndex
                    insideQuotation = true
                    beginQuotation = IntPair(i, j)
                } else if (utterIndex == 0 && insideQuotation) {
                    insideQuotation = false
                    endQuotation = IntPair(i, j)
                    findQuotationSpeaker(doc, utterNum, sentences, beginQuotation, endQuotation, dict)
                }
            }
        }
        if (insideQuotation) {
            endQuotation = IntPair(
                sentences.size - 1, sentences[sentences.size - 1].get(
                    TokensAnnotation::class.java
                )!!.size - 1
            )
            findQuotationSpeaker(doc, utterNum, sentences, beginQuotation, endQuotation, dict)
        }
    }

    private fun findQuotationSpeaker(
        doc: Document, utterNum: Int, sentences: List<CoreMap>,
        beginQuotation: IntPair?, endQuotation: IntPair, dict: Dictionaries
    ) {
        if (findSpeaker(doc, utterNum, beginQuotation!![0], sentences, 0, beginQuotation[1], dict)) return
        if (findSpeaker(
                doc, utterNum, endQuotation[0], sentences, endQuotation[1],
                sentences[endQuotation[0]].get(TokensAnnotation::class.java)!!.size, dict
            )
        ) return
        if (beginQuotation[1] <= 1 && beginQuotation[0] > 0) {
            if (findSpeaker(
                    doc, utterNum, beginQuotation[0] - 1, sentences, 0,
                    sentences[beginQuotation[0] - 1].get(TokensAnnotation::class.java)!!.size, dict
                )
            ) return
        }
        if (endQuotation[1] >= sentences[endQuotation[0]].size() - 2
            && sentences.size > endQuotation[0] + 1
        ) {
            if (findSpeaker(
                    doc, utterNum, endQuotation[0] + 1, sentences, 0,
                    sentences[endQuotation[0] + 1].get(TokensAnnotation::class.java)!!.size, dict
                )
            ) return
        }
    }

    private fun findSpeaker(
        doc: Document, utterNum: Int, sentNum: Int, sentences: List<CoreMap>,
        startIndex: Int, endIndex: Int, dict: Dictionaries
    ): Boolean {
        val sent = sentences[sentNum].get(
            TokensAnnotation::class.java
        )!!
        for (i in startIndex until endIndex) {
            val cl = sent[i]
            if (cl.get(UtteranceAnnotation::class.java) != 0) continue
            val lemma = cl.lemma()
            val word = cl.word()
            if (dict.reportVerb.contains(lemma) && cl.tag().startsWith("V")) {
                // find subject
                var dependency = sentences[sentNum].get(EnhancedDependenciesAnnotation::class.java)
                if (dependency == null) {
                    dependency = sentences[sentNum].get(BasicDependenciesAnnotation::class.java)
                }
                val w = dependency!!.getNodeByWordPattern(word)
                if (w != null) {
                    if (findSubject(doc, dependency, w, sentNum, utterNum)) return true
                    for (p in dependency.getPathToRoot(w)) {
                        if (!p.tag().startsWith("V") && !p.tag().startsWith("MD")) break
                        if (findSubject(
                                doc,
                                dependency,
                                p,
                                sentNum,
                                utterNum
                            )
                        ) return true // handling something like "was talking", "can tell"
                    }
                } else {
                    Redwood.log("debug-preprocessor", "Cannot find node in dependency for word $word")
                }
            }
        }
        return false
    }

    private fun findSubject(
        doc: Document,
        dependency: SemanticGraph?,
        w: IndexedWord,
        sentNum: Int,
        utterNum: Int
    ): Boolean {
        for (child in dependency!!.childPairs(w)) {
            if (child.first()!!.shortName == "nsubj") {
                val subjectString = child.second()!!.word()
                val subjectIndex = child.second()!!.index() // start from 1
                val headPosition = IntTuple(2)
                headPosition[0] = sentNum
                headPosition[1] = subjectIndex - 1
                val speaker: String
                speaker = if (doc.mentionheadPositions.containsKey(headPosition)) {
                    Integer.toString(doc.mentionheadPositions[headPosition]!!.mentionID)
                } else {
                    subjectString
                }
                doc.speakers[utterNum] = speaker
                return true
            }
        }
        return false
    }

    private fun findSpeakersInConversation(doc: Document, dict: Dictionaries) {
        for (l in doc.orderedMentions!!) {
            for (m in l) {
                if (m.predicateNominatives == null) continue
                for (a in m.predicateNominatives) {
                    if (a.spanToString().lowercase(Locale.getDefault()) == "i") {
                        val get = m.headWord.get(UtteranceAnnotation::class.java)
                        doc.speakers[get] = m.mentionID.toString()
                    }
                }
            }
        }
        var paragraph: MutableList<CoreMap> = ArrayList()
        var paragraphUtterIndex = 0
        var nextParagraphSpeaker = ""
        var paragraphOffset = 0
        for (sent in doc.annotation!!.get(SentencesAnnotation::class.java)!!) {
            paragraph.add(sent)
            val currentUtter = sent.get(TokensAnnotation::class.java)!![0].get(
                UtteranceAnnotation::class.java
            )!!
            if (paragraphUtterIndex != currentUtter) {
                nextParagraphSpeaker = findParagraphSpeaker(
                    doc,
                    paragraph,
                    paragraphUtterIndex,
                    nextParagraphSpeaker,
                    paragraphOffset,
                    dict
                )
                paragraphUtterIndex = currentUtter
                paragraphOffset += paragraph.size
                paragraph = ArrayList()
            }
        }
        findParagraphSpeaker(doc, paragraph, paragraphUtterIndex, nextParagraphSpeaker, paragraphOffset, dict)
    }

    private fun findParagraphSpeaker(
        doc: Document, paragraph: List<CoreMap>,
        paragraphUtterIndex: Int, nextParagraphSpeaker: String, paragraphOffset: Int, dict: Dictionaries
    ): String {
        if (!doc.speakers.containsKey(paragraphUtterIndex)) {
            if (!nextParagraphSpeaker.isEmpty()) {
                doc.speakers[paragraphUtterIndex] = nextParagraphSpeaker
            } else {  // find the speaker of this paragraph (John, nbc news)
                // cdm [Sept 2015] added this check to try to avoid crash
                if (paragraph.isEmpty()) {
                    Redwood.log("debug-preprocessor", "Empty paragraph; skipping findParagraphSpeaker")
                    return ""
                }
                val lastSent = paragraph[paragraph.size - 1]
                var speaker = ""
                var hasVerb = false
                for (i in lastSent.get(TokensAnnotation::class.java)!!.indices) {
                    val w = lastSent.get(TokensAnnotation::class.java)!![i]
                    val pos = w.get(PartOfSpeechAnnotation::class.java)!!
                    val ner = w.get(NamedEntityTagAnnotation::class.java)!!
                    if (pos.startsWith("V")) {
                        hasVerb = true
                        break
                    }
                    if (ner.startsWith("PER")) {
                        val headPosition = IntTuple(2)
                        headPosition[0] = paragraph.size - 1 + paragraphOffset
                        headPosition[1] = i
                        if (doc.mentionheadPositions.containsKey(headPosition)) {
                            speaker = Integer.toString(doc.mentionheadPositions[headPosition]!!.mentionID)
                        }
                    }
                }
                if (!hasVerb && speaker != "") {
                    doc.speakers[paragraphUtterIndex] = speaker
                }
            }
        }
        return findNextParagraphSpeaker(doc, paragraph, paragraphOffset, dict)
    }

    private fun findNextParagraphSpeaker(
        doc: Document,
        paragraph: List<CoreMap>,
        paragraphOffset: Int,
        dict: Dictionaries
    ): String {
        if (paragraph.isEmpty()) {
            return ""
        }
        val lastSent = paragraph[paragraph.size - 1]
        var speaker = ""
        for (w in lastSent.get(TokensAnnotation::class.java)!!) {
            if (w.get(LemmaAnnotation::class.java) == "report" || w.get(LemmaAnnotation::class.java) == "say") {
                val word: String = w.get(TextAnnotation::class.java)!!
                var dependency = lastSent.get(EnhancedDependenciesAnnotation::class.java)
                if (dependency == null) {
                    dependency = lastSent.get(BasicDependenciesAnnotation::class.java)
                }
                val t = dependency!!.getNodeByWordPattern(word)
                for (child in dependency.childPairs(t)) {
                    if (child.first()!!.shortName == "nsubj") {
                        val subjectIndex = child.second()!!.index() // start from 1
                        val headPosition = IntTuple(2)
                        headPosition[0] = paragraph.size - 1 + paragraphOffset
                        headPosition[1] = subjectIndex - 1
                        if (doc.mentionheadPositions.containsKey(headPosition)
                            && doc.mentionheadPositions[headPosition]!!.nerString.startsWith("PER")
                        ) {
                            speaker = Integer.toString(doc.mentionheadPositions[headPosition]!!.mentionID)
                        }
                    }
                }
            }
        }
        return speaker
    }

    /** Check one mention is the speaker of the other mention  */
    fun isSpeaker(m: Mention, ant: Mention, dict: Dictionaries): Boolean {
        if (!dict.firstPersonPronouns.contains(ant.spanToString().lowercase(Locale.getDefault()))
            || ant.number == Dictionaries.Number.PLURAL || ant.sentNum != m.sentNum
        ) return false
        var countQuotationMark = 0
        for (i in Math.min(m.headIndex, ant.headIndex) + 1 until Math.max(m.headIndex, ant.headIndex)) {
            val word = m.sentenceWords[i].get(TextAnnotation::class.java)!!
            if (word == "``" || word == "''") countQuotationMark++
        }
        if (countQuotationMark != 1) return false
        val w = m.enhancedDependency.getNodeByWordPattern(
            m.sentenceWords[m.headIndex].get(
                TextAnnotation::class.java
            )
        ) ?: return false
        for (parent in m.enhancedDependency.parentPairs(w)) {
            if (parent.first()!!.shortName == "nsubj" && dict.reportVerb.contains(
                    parent.second()!!.get(
                        LemmaAnnotation::class.java
                    )
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun markListMemberRelation(orderedMentions: List<Mention>) {
        for (m1 in orderedMentions) {
            for (m2 in orderedMentions) {
                // Mark if m2 and m1 are in list relationship
                if (m1.isListMemberOf(m2)) {
                    m2.addListMember(m1)
                    m1.addBelongsToList(m2)
                } else if (m2.isListMemberOf(m1)) {
                    m1.addListMember(m2)
                    m2.addBelongsToList(m1)
                }
            }
        }
    }

    private fun markMentionRelation(orderedMentions: List<Mention>, foundPairs: Set<Pair<Int, Int>>, flag: String) {
        for (m1 in orderedMentions) {
            for (m2 in orderedMentions) {
                if (m1 === m2) continue
                // Ignore if m2 and m1 are in list relationship
                if (m1.isListMemberOf(m2) || m2.isListMemberOf(m1) || m1.isMemberOfSameList(m2)) {
                    //Redwood.log("debug-preprocessor", "Not checking '" + m1 + "' and '" + m2 + "' for " + flag + ": in list relationship");
                    continue
                }
                for (foundPair in foundPairs) {
                    if (foundPair.first() == m1.headIndex && foundPair.second() == m2.headIndex) {
                        if (flag == "APPOSITION") {
                            if (foundPair.first() != foundPair.second() || m2.insideIn(m1)) {
                                m2.addApposition(m1)
                            }
                        } else if (flag == "PREDICATE_NOMINATIVE") {
                            m2.addPredicateNominatives(m1)
                        } else if (flag == "RELATIVE_PRONOUN") m2.addRelativePronoun(m1) else throw RuntimeException("check flag in markMentionRelation (dcoref/MentionExtractor.java)")
                    }
                }
            }
        }
    } //  private static final TregexPattern relativePronounPattern = TregexPattern.compile("NP < (NP=m1 $.. (SBAR < (WHNP < WP|WDT=m2)))");
    //  private static void findRelativePronouns(Tree tree, Set<Pair<Integer, Integer>> relativePronounPairs) {
    //    findTreePattern(tree, relativePronounPattern, relativePronounPairs);
    //  }
}
