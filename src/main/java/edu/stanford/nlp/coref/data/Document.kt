//
// StanfordCoreNLP -- a suite of NLP tools
// Copyright (c) 2009-2010 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//
package edu.stanford.nlp.coref.data

import edu.stanford.nlp.util.Pair.Companion.makePair
import edu.stanford.nlp.coref.docreader.CoNLLDocumentReader.CoNLLDocument
import edu.stanford.nlp.util.IntTuple
import edu.stanford.nlp.util.Generics
import edu.stanford.nlp.coref.data.InputDoc
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.DocIDAnnotation
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.util.Pair
import edu.stanford.nlp.util.Triple
import java.io.Serializable
import java.lang.RuntimeException
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min

class Document() : Serializable {
    enum class DocType {
        CONVERSATION, ARTICLE
    }

    /** The type of document: conversational or article  */
    @JvmField
    var docType: DocType? = null

    /** Document annotation  */
    @JvmField
    var annotation: Annotation? = null

    /** for conll shared task 2011   */
    @JvmField
    var conllDoc: CoNLLDocument? = null

    /** The list of gold mentions  */
    @JvmField
    var goldMentions: List<List<Mention>>? = null
    /** return the list of predicted mentions  */
    /** The list of predicted mentions  */
    var orderedMentions: MutableList<List<Mention>>? = null

    /** Clusters for coreferent mentions  */
    @JvmField
    var corefClusters: HashMap<Int, CorefCluster>

    /** Gold Clusters for coreferent mentions  */
    @JvmField
    var goldCorefClusters: HashMap<Int, CorefCluster>?

    /** All mentions in a document mentionID -&gt; mention  */
    @JvmField
    var predictedMentionsByID: HashMap<Int, Mention>
    @JvmField
    var goldMentionsByID: HashMap<Int, Mention>? = null

    /** Set of roles (in role apposition) in a document   */
    @JvmField
    var roleSet: Set<Mention>

    /**
     * Position of each mention in the input matrix
     * Each mention occurrence with sentence # and position within sentence
     * (Nth mention, not Nth token)
     */
    @JvmField
    var positions // mentions may be removed from this due to post processing
            : HashMap<Mention, IntTuple>
    @JvmField
    var allPositions // all mentions (mentions will not be removed from this)
            : HashMap<Mention, IntTuple>? = null
    @JvmField
    val mentionheadPositions: HashMap<IntTuple, Mention>

    /** List of gold links in a document by positions  */
    private var goldLinks: List<Pair<IntTuple?, IntTuple?>>? = null

    /** UtteranceAnnotation -&gt; String (speaker): mention ID or speaker string
     * e.g., the value can be "34" (mentionID), "Larry" (speaker string), or "PER3" (autoassigned speaker string)
     */
    @JvmField
    var speakers: HashMap<Int, String>

    /** Pair of mention id, and the mention's speaker id
     * the second value is the "speaker mention"'s id.
     * e.g., Larry said, "San Francisco is a city.": (id(Larry), id(San Francisco))
     */
    @JvmField
    var speakerPairs: HashSet<Pair<Int, Int>>
    @JvmField
    var speakerInfoGiven = false
    @JvmField
    var maxUtter = 0
    @JvmField
    var numParagraph = 0
    @JvmField
    var numSentences = 0

    /** Set of incompatible clusters pairs  */
    private val incompatibles: MutableSet<Pair<Int, Int>>
    private val incompatibleClusters: MutableSet<Pair<Int, Int>>
    @JvmField
    var acronymCache: MutableMap<Pair<Int, Int>, Boolean?>

    /** Map of speaker name/id to speaker info
     * the key is the value of the variable 'speakers'
     */
    @JvmField
    var speakerInfoMap = Generics.newHashMap<String, SpeakerInfo>()
    // public Counter<String> properNouns = new ClassicCounter<>();
    // public Counter<String> phraseCounter = new ClassicCounter<>();
    // public Counter<String> headwordCounter = new ClassicCounter<>();
    /** Additional information about the document. Can be used as features  */
    @JvmField
    var docInfo: Map<String, String>? = null
    @JvmField
    var filterMentionSet: Set<Triple<Int, Int, Int>>? = null

    init {
        positions = hashMapOf();
        mentionheadPositions = hashMapOf()
        roleSet = Generics.newHashSet()
        corefClusters = Generics.newHashMap()
        goldCorefClusters = null
        predictedMentionsByID = hashMapOf()
        //    goldMentionsByID = Generics.newHashMap();
        speakers = hashMapOf()
        speakerPairs = Generics.newHashSet()
        incompatibles = Generics.newHashSet()
        incompatibleClusters = Generics.newHashSet()
        acronymCache = Generics.newHashMap()
    }

    constructor(
        anno: Annotation?,
        predictedMentions: MutableList<List<Mention>>?,
        goldMentions: List<List<Mention>>?
    ) : this() {
        annotation = anno
        orderedMentions = predictedMentions
        this.goldMentions = goldMentions
    }

    constructor(input: InputDoc, mentions: MutableList<List<Mention>>?) : this() {
        annotation = input.annotation
        orderedMentions = mentions
        goldMentions = input.goldMentions
        docInfo = input.docInfo
        numSentences = input.annotation.get(SentencesAnnotation::class.java)!!.size
        conllDoc = input.conllDoc // null if it's not conll input
        filterMentionSet = input.filterMentionSet
    }// Last column is coreference

    /**
     * Returns list of sentences, where token in the sentence is a list of strings (tags) associated with the sentence
     * @return
     */
    val sentenceWordLists: MutableList<MutableList<Array<String>>>
        get() = if (conllDoc != null) {
            conllDoc!!.sentenceWordLists
        } else {
            val sentWordLists: MutableList<MutableList<Array<String>>> = ArrayList()
            val sentences = annotation!!.get(
                SentencesAnnotation::class.java
            )!!
            val docId: String = annotation!![DocIDAnnotation::class.java]!!
            for (sentence in sentences) {
                val sentWordList: MutableList<Array<String>> = ArrayList()
                val tokens = sentence.get(
                    TokensAnnotation::class.java
                )!!
                for (token in tokens) {
                    // Last column is coreference
                    val strs: Array<String> = arrayOf(
                        docId, "-", token.index().toString(), token.word(), token.tag(), "-", "-",
                        token.getString(CoreAnnotations.SpeakerAnnotation::class.java, ""), token.ner(), "-"
                    )
                    sentWordList.add(strs)
                }
                sentWordLists.add(sentWordList)
            }
            sentWordLists
        }

    fun isIncompatible(c1: CorefCluster, c2: CorefCluster): Boolean {
        // Was any of the pairs of mentions marked as incompatible
        val cid1 = min(c1.clusterID, c2.clusterID)
        val cid2 = max(c1.clusterID, c2.clusterID)
        return incompatibleClusters.contains(makePair(cid1, cid2))
    }

    // Update incompatibles for two clusters that are about to be merged
    fun mergeIncompatibles(to: CorefCluster, from: CorefCluster) {
        val replacements: MutableList<Pair<Pair<Int, Int>, Pair<Int, Int>>> = ArrayList()
        for (p in incompatibleClusters) {
            var other: Int? = null
            if (p.first == from.clusterID) {
                other = p.second
            } else if (p.second == from.clusterID) {
                other = p.first
            }
            if (other != null && other != to.clusterID) {
                val cid1 = min(other, to.clusterID)
                val cid2 = max(other, to.clusterID)
                replacements.add(makePair(p, makePair(cid1, cid2)))
            }
        }
        for (r in replacements) {
            incompatibleClusters.remove(r.first)
            incompatibleClusters.add(r.second!!)
        }
    }

    fun mergeAcronymCache(to: CorefCluster, from: CorefCluster) {
        val replacements = Generics.newHashMap<Pair<Int, Int>, Boolean>()
        for (p in acronymCache.keys) {
            if (acronymCache[p]!!) {
                var other: Int? = null
                if (p.first == from.clusterID) {
                    other = p.second
                } else if (p.second == from.clusterID) {
                    other = p.first
                }
                if (other != null && other != to.clusterID) {
                    val cid1 = min(other, to.clusterID)
                    val cid2 = max(other, to.clusterID)
                    replacements[makePair(cid1, cid2)] = true
                }
            }
        }
        for (p in replacements.keys) {
            acronymCache[p] = replacements[p]
        }
    }

    fun isIncompatible(m1: Mention, m2: Mention): Boolean {
        val mid1 = min(m1.mentionID, m2.mentionID)
        val mid2 = max(m1.mentionID, m2.mentionID)
        return incompatibles.contains(makePair(mid1, mid2))
    }

    fun addIncompatible(m1: Mention, m2: Mention) {
        val mid1 = min(m1.mentionID, m2.mentionID)
        val mid2 = max(m1.mentionID, m2.mentionID)
        incompatibles.add(makePair(mid1, mid2))
        val cid1 = min(m1.corefClusterID, m2.corefClusterID)
        val cid2 = max(m1.corefClusterID, m2.corefClusterID)
        incompatibleClusters.add(makePair(cid1, cid2))
    }

    fun getGoldLinks(): List<Pair<IntTuple?, IntTuple?>>? {
        if (goldLinks == null) extractGoldLinks()
        return goldLinks
    }

    /** Extract gold coref link information  */
    protected fun extractGoldLinks() {
        //    List<List<Mention>> orderedMentionsBySentence = this.getOrderedMentions();
        val links: MutableList<Pair<IntTuple?, IntTuple?>> = ArrayList()

        // position of each mention in the input matrix, by id
        val positions = Generics.newHashMap<Int, IntTuple>()
        // positions of antecedents
        val antecedents = Generics.newHashMap<Int, MutableList<IntTuple?>?>()
        for (i in goldMentions!!.indices) {
            for (j in goldMentions!![i].indices) {
                val m = goldMentions!![i][j]
                val id = m.mentionID
                val pos = IntTuple(2)
                pos[0] = i
                pos[1] = j
                positions[id] = pos
                antecedents[id] = ArrayList()
            }
        }

//    SieveCoreferenceSystem.debugPrintMentions(System.err, "", goldOrderedMentionsBySentence);
        for (mentions in goldMentions!!) {
            for (m in mentions) {
                val id = m.mentionID
                val src = positions[id]!!
                if (m.originalRef >= 0) {
                    var dst: IntTuple? = positions[m.originalRef]
                        ?: throw RuntimeException("Cannot find gold mention with ID=" + m.originalRef)

                    // to deal with cataphoric annotation
                    while (dst!![0] > src[0] || dst[0] == src[0] && dst[1] > src[1]) {
                        val dstMention = goldMentions!![dst[0]][dst[1]]
                        m.originalRef = dstMention.originalRef
                        dstMention.originalRef = id
                        if (m.originalRef < 0) break
                        dst = positions[m.originalRef]
                    }
                    if (m.originalRef < 0) continue

                    // A B C: if A<-B, A<-C => make a link B<-C
                    for (k in dst[0]..src[0]) {
                        for (l in goldMentions!![k].indices) {
                            if (k == dst[0] && l < dst[1]) continue
                            if (k == src[0] && l > src[1]) break
                            val missed = IntTuple(2)
                            missed[0] = k
                            missed[1] = l
                            if (links.contains(Pair(missed, dst))) {
                                antecedents[id]!!.add(missed)
                                links.add(Pair(src, missed))
                            }
                        }
                    }
                    links.add(Pair(src, dst))
                    assert(antecedents[id] != null)
                    antecedents[id]!!.add(dst)
                    val ants: List<IntTuple?> = antecedents[m.originalRef]!!
                    for (ant in ants) {
                        antecedents[id]!!.add(ant)
                        links.add(Pair(src, ant))
                    }
                }
            }
        }
        goldLinks = links
    }

    fun getSpeakerInfo(speaker: String): SpeakerInfo? {
        return speakerInfoMap[speaker]
    }

    fun numberOfSpeakers(): Int {
        return speakerInfoMap.size
    }

    fun isCoref(m1: Mention, m2: Mention): Boolean {
        return (goldMentionsByID!!.containsKey(m1.mentionID)
                && goldMentionsByID!!.containsKey(m2.mentionID)
                && goldMentionsByID!![m1.mentionID]!!.goldCorefClusterID == goldMentionsByID!![m2.mentionID]!!.goldCorefClusterID)
    }

    companion object {
        private const val serialVersionUID = -4139866807494603953L
    }
}