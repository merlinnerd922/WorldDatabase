package edu.stanford.nlp.dcoref

import edu.stanford.nlp.util.Pair.Companion.makePair
import kotlin.jvm.JvmOverloads
import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem
import edu.stanford.nlp.io.IOUtils
import edu.stanford.nlp.io.RuntimeIOException
import edu.stanford.nlp.ling.CoreAnnotation
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation
import edu.stanford.nlp.pipeline.ChunkAnnotationUtils
import edu.stanford.nlp.ling.CoreAnnotations.SentenceIndexAnnotation
import java.lang.RuntimeException
import java.lang.StringBuilder
import edu.stanford.nlp.ling.CoreAnnotations.DocIDAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TokenBeginAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TokenEndAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.stats.Counters
import edu.stanford.nlp.stats.IntCounter
import edu.stanford.nlp.trees.*
import edu.stanford.nlp.util.*
import edu.stanford.nlp.util.logging.Redwood.RedwoodChannels
import edu.stanford.nlp.util.logging.Redwood
import java.io.*
import java.util.*
import java.util.logging.Logger
import java.util.regex.Pattern
import kotlin.Throws
import kotlin.jvm.JvmStatic

/**
 * Read _conll file format from CoNLL2011.  See http://conll.bbn.com/index.php/data.html.
 *
 * CoNLL2011 files are in /u/scr/nlp/data/conll-2011/v0/data/
 * dev
 * train
 * Contains *_auto_conll files (auto generated) and _gold_conll (hand labelled), default reads _gold_conll
 * There is also /u/scr/nlp/data/conll-2011/v0/conll.trial which has *.conll files (parse has _ at end)
 *
 * Column 	Type 	Description
 * 1   	Document ID 	This is a variation on the document filename
 * 2   	Part number 	Some files are divided into multiple parts numbered as 000, 001, 002, ... etc.
 * 3   	Word number
 * 4   	Word itself
 * 5   	Part-of-Speech
 * 6   	Parse bit 	This is the bracketed structure broken before the first open parenthesis in the parse, and the word/part-of-speech leaf replaced with a *. The full parse can be created by substituting the asterix with the "([pos] [word])" string (or leaf) and concatenating the items in the rows of that column.
 * 7   	Predicate lemma 	The predicate lemma is mentioned for the rows for which we have semantic role information. All other rows are marked with a "-"
 * 8   	Predicate Frameset ID 	This is the PropBank frameset ID of the predicate in Column 7.
 * 9   	Word sense 	This is the word sense of the word in Column 3.
 * 10   	Speaker/Author 	This is the speaker or author name where available. Mostly in Broadcast Conversation and Web Log data.
 * 11   	Named Entities 	These columns identifies the spans representing various named entities.
 * 12:N   	Predicate Arguments 	There is one column each of predicate argument structure information for the predicate mentioned in Column 7.
 * N   	Coreference 	Coreference chain information encoded in a parenthesis structure.
 *
 * @author Angel Chang
 */
class CoNLL2011DocumentReader @JvmOverloads constructor(filepath: String?, options: Options = Options()) {
    private var docIterator: DocumentIterator? = null

    //  private String filepath;
    protected val fileList: List<File>
    private var curFileIndex: Int
    private val options: Options

    init {
//    this.filepath = filepath;
        fileList = getFiles(filepath, options.filePattern)
        this.options = options
        if (options.sortFiles) {
            Collections.sort(fileList)
        }
        curFileIndex = 0
        logger.info("Reading " + fileList.size + " CoNll2011 files from " + filepath)
    }

    fun reset() {
        curFileIndex = 0
        if (docIterator != null) {
            docIterator!!.close()
            docIterator = null
        }
    }// DONE!

    // DONE!
    val nextDocument: Document?
        get() {
            return try {
                if (curFileIndex >= fileList.size) {
                    return null // DONE!
                }
                var curFile = fileList[curFileIndex]
                if (docIterator == null) {
                    docIterator = DocumentIterator(curFile.absolutePath, options)
                }
                while (!docIterator!!.hasNext()) {
                    logger.info("Processed " + docIterator!!.docCnt + " documents in " + curFile.absolutePath)
                    docIterator!!.close()
                    curFileIndex++
                    if (curFileIndex >= fileList.size) {
                        return null // DONE!
                    }
                    curFile = fileList[curFileIndex]
                    docIterator = DocumentIterator(curFile.absolutePath, options)
                }
                val next = docIterator!!.next()
                SieveCoreferenceSystem.logger.fine("Reading document: " + next.documentID)
                next
            } catch (ex: IOException) {
                throw RuntimeIOException(ex)
            }
        }

    fun close() {
        IOUtils.closeIgnoringExceptions(docIterator)
    }

    class NamedEntityAnnotation : CoreAnnotation<CoreMap> {
        override val type: Class<CoreMap>
            get() = CoreMap::class.java
    }

    class CorefMentionAnnotation : CoreAnnotation<CoreMap> {
        override val type: Class<CoreMap>
            get() = CoreMap::class.java
    }

    /** Flags  */
    class Options @JvmOverloads constructor(protected var fileFilter: String = ".*_gold_conll$") {
        var useCorefBIOESEncoding = false // Marks Coref mentions with prefix

        // B- begin, I- inside, E- end, S- single
        @JvmField
        var annotateTokenCoref = true // Annotate token with CorefAnnotation

        // If token belongs to multiple clusters
        // coref clusterid are separted by '|'
        @JvmField
        var annotateTokenSpeaker = true // Annotate token with SpeakerAnnotation
        @JvmField
        var annotateTokenPos = true // Annotate token with PartOfSpeechAnnotation
        @JvmField
        var annotateTokenNer = true // Annotate token with NamedEntityTagAnnotation
        var annotateTreeCoref = false // Annotate tree with CorefMentionAnnotation
        var annotateTreeNer = false // Annotate tree with NamedEntityAnnotation
        var backgroundNerTag = "O" // Background NER tag
        var filePattern: Pattern
        var sortFiles = false

        init {
            filePattern = Pattern.compile(fileFilter)
        }

        fun setFilter(filter: String) {
            fileFilter = filter
            filePattern = Pattern.compile(fileFilter)
        }
    }

    class Document {
        var documentIdPart: String? = null
        var documentID: String? = null
        var partNo: String? = null
        @JvmField
        var sentenceWordLists: MutableList<List<Array<String>>> = ArrayList()
        var annotation: Annotation? = null
        var corefChainMap: CollectionValuedMap<String?, CoreMap>? = null
        var nerChunks: List<CoreMap>? = null
        fun getSentenceWordLists(): List<List<Array<String>>> {
            return sentenceWordLists
        }

        fun addSentence(sentence: List<Array<String>>) {
            sentenceWordLists.add(sentence)
        }
    }

    /** Helper iterator  */
    private class DocumentIterator(// State
        var filename: String, private val options: Options
    ) : AbstractIterator<Document?>(), Closeable {
        var br: BufferedReader
        var nextDoc: Document?
        var lineCnt = 0
        var docCnt = 0
        override fun hasNext(): Boolean {
            return nextDoc != null
        }

        override fun next(): Document {
            if (nextDoc == null) {
                throw NoSuchElementException("DocumentIterator exhausted.")
            }
            val curDoc: Document = nextDoc!!
            nextDoc = readNextDocument()
            return curDoc
        }

        private fun wordsToSentence(sentWords: List<Array<String>>): CoreMap {
            val sentText = concatField(sentWords, FIELD_WORD)
            val sentence = Annotation(sentText)
            val tree = wordsToParse(sentWords)
            sentence.set(TreeAnnotation::class.java, tree)
            val leaves = tree.getLeaves<Tree>()
            assert(leaves.size == sentWords.size)
            val tokens: MutableList<CoreLabel> = ArrayList(leaves.size)
            sentence.set(TokensAnnotation::class.java, tokens)
            for (i in sentWords.indices) {
                val fields = sentWords[i]
                val wordPos = fields[FIELD_WORD_NO].toInt()
                assert(wordPos == i)
                val leaf = leaves[i]
                val token = leaf.label() as CoreLabel
                tokens.add(token)
                if (options.annotateTokenSpeaker) {
                    val speaker = fields[FIELD_SPEAKER_AUTHOR].replace("_", " ")
                    if (HYPHEN != speaker) {
                        token.set(CoreAnnotations.SpeakerAnnotation::class.java, speaker)
                    }
                }
            }
            if (options.annotateTokenPos) {
                for (leaf in leaves) {
                    val token = leaf.label() as CoreLabel
                    token.set(PartOfSpeechAnnotation::class.java, leaf.parent(tree).value())
                }
            }
            if (options.annotateTokenNer) {
                val nerSpans = getNerSpans(sentWords)
                for (nerSpan in nerSpans) {
                    val startToken = nerSpan.first()
                    val endToken = nerSpan.second() /* inclusive */
                    val label = nerSpan.third()
                    for (i in startToken..endToken) {
                        val leaf = leaves[i]
                        val token = leaf.label() as CoreLabel
                        val oldLabel = token.get(NamedEntityTagAnnotation::class.java)
                        if (oldLabel != null) {
                            logger.warning("Replacing old named entity tag $oldLabel with $label")
                        }
                        token.set(NamedEntityTagAnnotation::class.java, label)
                    }
                }
                for (token in tokens) {
                    if (!token.containsKey(NamedEntityTagAnnotation::class.java)) {
                        token.set(NamedEntityTagAnnotation::class.java, options.backgroundNerTag)
                    }
                }
            }
            if (options.annotateTokenCoref) {
                val corefSpans = getCorefSpans(sentWords)
                for (corefSpan in corefSpans) {
                    val startToken = corefSpan.first()
                    val endToken = corefSpan.second() /* inclusive */
                    val label = corefSpan.third()
                    for (i in startToken..endToken) {
                        val leaf = leaves[i]
                        val token = leaf.label() as CoreLabel
                        var curLabel = label
                        if (options.useCorefBIOESEncoding) {
                            var prefix: String
                            prefix = if (startToken == endToken) {
                                "S-"
                            } else if (i == startToken) {
                                "B-"
                            } else if (i == endToken) {
                                "E-"
                            } else {
                                "I-"
                            }
                            curLabel = prefix + label
                        }
                        val oldLabel = token.get(CorefCoreAnnotations.CorefAnnotation::class.java)
                        if (oldLabel != null) {
                            curLabel = "$oldLabel|$curLabel"
                        }
                        token.set(CorefCoreAnnotations.CorefAnnotation::class.java, curLabel)
                    }
                }
            }
            return sentence
        }

        fun annotateDocument(document: Document?) {
            val sentences: MutableList<CoreMap> = ArrayList(document!!.sentenceWordLists.size)
            for (sentWords in document.sentenceWordLists) {
                sentences.add(wordsToSentence(sentWords))
            }
            val docAnnotation = sentencesToDocument(
                document.documentIdPart /*document.documentID + "." + document.partNo */, sentences
            )
            document.annotation = docAnnotation

            // Do this here so we have updated character offsets and all
            val corefChainMap = CollectionValuedMap<String?, CoreMap>(CollectionFactory.arrayListFactory())
            val nerChunks: MutableList<CoreMap> = ArrayList()
            for (i in sentences.indices) {
                val sentence = sentences[i]
                val tree = sentence.get(TreeAnnotation::class.java)
                tree!!.setSpans()
                val sentWords = document.sentenceWordLists[i]

                // Get NER chunks
                val nerSpans = getNerSpans(sentWords)
                for (nerSpan in nerSpans) {
                    val startToken = nerSpan.first()
                    val endToken = nerSpan.second() /* inclusive */
                    val label = nerSpan.third()
                    val nerChunk: CoreMap = ChunkAnnotationUtils.getAnnotatedChunk(sentence, startToken, endToken + 1)
                    nerChunk.set(NamedEntityTagAnnotation::class.java, label)
                    nerChunk.set(
                        SentenceIndexAnnotation::class.java, sentence[SentenceIndexAnnotation::class.java]!!
                    )
                    nerChunks.add(nerChunk)
                    val t = getTreeNonTerminal(tree, startToken, endToken, true)
                    if (t.span.source == startToken && t.span.target == endToken) {
                        nerChunk.set(TreeAnnotation::class.java, t)
                        if (options.annotateTreeNer) {
                            val tlabel = t.label()
                            if (tlabel is CoreLabel) {
                                tlabel.set<CoreMap>(NamedEntityAnnotation::class.java, nerChunk)
                            }
                        }
                    }
                }
                val corefSpans = getCorefSpans(sentWords)
                for (corefSpan in corefSpans) {
                    val startToken = corefSpan.first()
                    val endToken = corefSpan.second() /* inclusive */
                    val corefId = corefSpan.third()
                    val mention: CoreMap = ChunkAnnotationUtils.getAnnotatedChunk(sentence, startToken, endToken + 1)
                    mention.set(CorefCoreAnnotations.CorefAnnotation::class.java, corefId)
                    mention.set(
                        SentenceIndexAnnotation::class.java, sentence.get(
                            SentenceIndexAnnotation::class.java
                        )!!
                    )
                    corefChainMap.add(corefId, mention)
                    val t = getTreeNonTerminal(tree, startToken, endToken, true)
                    mention.set(TreeAnnotation::class.java, t)
                    if (options.annotateTreeCoref) {
                        val tlabel = t.label()
                        if (tlabel is CoreLabel) {
                            tlabel.set<CoreMap>(CorefMentionAnnotation::class.java, mention)
                        }
                    }
                }
            }
            document.corefChainMap = corefChainMap
            document.nerChunks = nerChunks
        }

        init {
            br = IOUtils.readerFromString(filename)
            nextDoc = readNextDocument()
        }

        fun readNextDocument(): Document? {
            try {
                var curSentWords: MutableList<Array<String>> = ArrayList()
                var document: Document? = null
                var line: String
                while (br.readLine().also { line = it } != null) {
                    lineCnt++
                    line = line.trim { it <= ' ' }
                    if (line.length != 0) {
                        if (line.startsWith(docStart)) {
                            // Start of new document
                            if (document != null) {
                                logger.warning("Unexpected begin document at line (\" + filename + \",\" + lineCnt + \")")
                            }
                            document = Document()
                            document.documentIdPart = line.substring(docStartLength)
                        } else if (line.startsWith("#end document")) {
                            annotateDocument(document)
                            docCnt++
                            return document
                            // End of document
                        } else {
                            assert(document != null)
                            val fields = delimiterPattern.split(line)
                            if (fields.size < FIELDS_MIN) {
                                throw RuntimeException(
                                    "Unexpected number of field " + fields.size +
                                            ", expected >= " + FIELDS_MIN + " for line (" + filename + "," + lineCnt + "): " + line
                                )
                            }
                            val curDocId = fields[FIELD_DOC_ID]
                            val partNo = fields[FIELD_PART_NO]
                            if (document!!.documentID == null) {
                                document.documentID = curDocId
                                document.partNo = partNo
                            } else {
                                // Check documentID didn't suddenly change on us
                                assert(document.documentID == curDocId)
                                assert(document.partNo == partNo)
                            }
                            curSentWords.add(fields)
                        }
                    } else {
                        // Current sentence has ended, new sentence is about to be started
                        if (curSentWords.size > 0) {
                            assert(document != null)
                            document!!.addSentence(curSentWords)
                            curSentWords = ArrayList()
                        }
                    }
                }
            } catch (ex: IOException) {
                throw RuntimeIOException(ex)
            }
            return null
        }

        override fun close() {
            IOUtils.closeIgnoringExceptions(br)
        }

        companion object {
            private val delimiterPattern = Pattern.compile("\\s+")
            private val treeReaderFactory = LabeledScoredTreeReaderFactory(null as TreeNormalizer?)
            private val starPattern = Pattern.compile("\\*")
            private fun wordsToParse(sentWords: List<Array<String>>): Tree {
                val sb = StringBuilder()
                for (fields in sentWords) {
                    if (sb.length > 0) {
                        sb.append(' ')
                    }
                    val str = fields[FIELD_PARSE_BIT].replace("NOPARSE", "X")
                    val tagword = "(" + fields[FIELD_POS_TAG] + " " + fields[FIELD_WORD] + ")"
                    // Replace stars
                    var si = str.indexOf('*')
                    sb.append(str.substring(0, si))
                    sb.append(tagword)
                    sb.append(str.substring(si + 1))
                    si = str.indexOf('*', si + 1)
                    if (si >= 0) {
                        logger.warning(" Parse bit with multiple *: $str")
                    }
                }
                val parseStr = sb.toString()
                return Tree.valueOf(parseStr, treeReaderFactory)
            }

            private fun getCorefSpans(sentWords: List<Array<String>>): List<Triple<Int, Int, String>> {
                return getLabelledSpans(sentWords, FIELD_COREF, HYPHEN, true)
            }

            private fun getNerSpans(sentWords: List<Array<String>>): List<Triple<Int, Int, String>> {
                return getLabelledSpans(sentWords, FIELD_NER_TAG, ASTERISK, false)
            }

            private const val ASTERISK = "*"
            private const val HYPHEN = "-"
            private fun getLabelledSpans(
                sentWords: List<Array<String>>, fieldIndex: Int,
                defaultMarker: String, checkEndLabel: Boolean
            ): List<Triple<Int, Int, String>> {
                val spans: MutableList<Triple<Int, Int, String>> = ArrayList()
                val openSpans = Stack<Triple<Int, Int, String>>()
                val removeStar = ASTERISK == defaultMarker
                for (wordPos in sentWords.indices) {
                    val fields = sentWords[wordPos]
                    val `val` = getField(fields, fieldIndex)
                    if (defaultMarker != `val`) {
                        var openParenIndex = -1
                        var lastDelimiterIndex = -1
                        for (j in 0 until `val`.length) {
                            val c = `val`[j]
                            var isDelimiter = false
                            if (c == '(' || c == ')' || c == '|') {
                                if (openParenIndex >= 0) {
                                    var s = `val`.substring(openParenIndex + 1, j)
                                    if (removeStar) {
                                        s = starPattern.matcher(s).replaceAll("")
                                    }
                                    openSpans.push(Triple(wordPos, -1, s))
                                    openParenIndex = -1
                                }
                                isDelimiter = true
                            }
                            if (c == '(') {
                                openParenIndex = j
                            } else if (c == ')') {
                                var t = openSpans.pop()
                                if (checkEndLabel) {
                                    // NOTE: end parens may cross (usually because mention either start or end on the same token
                                    // and it is just an artifact of the ordering
                                    val s = `val`.substring(lastDelimiterIndex + 1, j)
                                    if (s != t.third()) {
                                        val saved = Stack<Triple<Int, Int, String>>()
                                        while (s != t.third()) {
                                            // find correct match
                                            saved.push(t)
                                            if (openSpans.isEmpty()) {
                                                throw RuntimeException("Cannot find matching labelled span for $s")
                                            }
                                            t = openSpans.pop()
                                        }
                                        while (!saved.isEmpty()) {
                                            openSpans.push(saved.pop())
                                        }
                                        assert(s == t.third())
                                    }
                                }
                                t.setSecond(wordPos)
                                spans.add(t)
                            }
                            if (isDelimiter) {
                                lastDelimiterIndex = j
                            }
                        }
                        if (openParenIndex >= 0) {
                            var s = `val`.substring(openParenIndex + 1, `val`.length)
                            if (removeStar) {
                                s = starPattern.matcher(s).replaceAll("")
                            }
                            openSpans.push(Triple(wordPos, -1, s))
                        }
                    }
                }
                if (openSpans.size != 0) {
                    throw RuntimeException(
                        "Error extracting labelled spans for column " + fieldIndex + ": "
                                + concatField(sentWords, fieldIndex)
                    )
                }
                return spans
            }

            fun sentencesToDocument(documentID: String?, sentences: List<CoreMap>): Annotation {
                val docText: String? = null
                val document = Annotation(docText)
                document.set(DocIDAnnotation::class.java, documentID!!)
                document.set(SentencesAnnotation::class.java, sentences)


                // Accumulate docTokens and label sentence with overall token begin/end, and sentence index annotations
                val docTokens: MutableList<CoreLabel> = ArrayList()
                var sentenceIndex = 0
                var tokenBegin = 0
                for (sentenceAnnotation in sentences) {
                    val sentenceTokens = sentenceAnnotation.get(
                        TokensAnnotation::class.java
                    )
                    docTokens.addAll(sentenceTokens!!)
                    val tokenEnd = tokenBegin + sentenceTokens.size
                    sentenceAnnotation.set(TokenBeginAnnotation::class.java, tokenBegin)
                    sentenceAnnotation.set(TokenEndAnnotation::class.java, tokenEnd)
                    sentenceAnnotation.set(SentenceIndexAnnotation::class.java, sentenceIndex)
                    sentenceIndex++
                    tokenBegin = tokenEnd
                }
                document.set(TokensAnnotation::class.java, docTokens)

                // Put in character offsets
                var i = 0
                for (token in docTokens) {
                    val tokenText = token.get(TextAnnotation::class.java)!!
                    token.set(CharacterOffsetBeginAnnotation::class.java, i)
                    i += tokenText.length
                    token.set(CharacterOffsetEndAnnotation::class.java, i)
                    i++ // Skip space
                }
                for (sentenceAnnotation in sentences) {
                    val sentenceTokens = sentenceAnnotation.get(
                        TokensAnnotation::class.java
                    )!!
                    sentenceAnnotation.set(
                        CharacterOffsetBeginAnnotation::class.java,
                        sentenceTokens[0].get(CharacterOffsetBeginAnnotation::class.java)!!
                    )
                    sentenceAnnotation.set(
                        CharacterOffsetEndAnnotation::class.java,
                        sentenceTokens[sentenceTokens.size - 1].get(CharacterOffsetEndAnnotation::class.java)!!
                    )
                }
                return document
            }

            private fun getLowestCommonAncestor(root: Tree?, startToken: Int, endToken: Int): Tree {
                val leftLeaf = Trees.getLeaf(root, startToken)
                val rightLeaf = Trees.getLeaf(root, endToken)
                // todo [cdm 2013]: It might be good to climb certain unaries here, like VP or S under NP, but it's not good to climb all unaries (e.g., NP under FRAG)
                return Trees.getLowestCommonAncestor(leftLeaf, rightLeaf, root)
            }

            private fun getTreeNonTerminal(
                root: Tree?,
                startToken: Int,
                endToken: Int,
                acceptPreTerminals: Boolean
            ): Tree {
                var t = getLowestCommonAncestor(root, startToken, endToken)
                if (t.isLeaf) {
                    t = t.parent(root)
                }
                if (!acceptPreTerminals && t.isPreTerminal) {
                    t = t.parent(root)
                }
                return t
            }

            private const val docStart = "#begin document "
            private const val docStartLength = docStart.length
        }
    } // end static class DocumentIterator

    class CorpusStats {
        var mentionTreeLabelCounter = IntCounter<String>()
        var mentionTreeNonPretermLabelCounter = IntCounter<String>()
        var mentionTreePretermNonPretermNoMatchLabelCounter = IntCounter<String>()
        var mentionTreeMixedLabelCounter = IntCounter<String>()
        var mentionTokenLengthCounter = IntCounter<Int>()
        var nerMentionTokenLengthCounter = IntCounter<Int>()
        var mentionExactTreeSpan = 0
        var nonPretermSpanMatches = 0
        var totalMentions = 0
        var nestedNerMentions = 0
        var nerMentions = 0
        fun process(doc: Document) {
            val sentences = doc.annotation!!.get(
                SentencesAnnotation::class.java
            )!!
            for (id in doc.corefChainMap!!.keys) {
                val mentions = doc.corefChainMap!![id]
                for (m in mentions!!) {
                    val sent = sentences[m.get(SentenceIndexAnnotation::class.java)!!]
                    val root = sent.get(TreeAnnotation::class.java)
                    val t = m.get(TreeAnnotation::class.java)
                    var npt = t
                    var npt2 = t
                    if (npt!!.isPreTerminal) {
                        npt = npt.parent(root)
                    }
                    val sentTokenStart = sent.get(TokenBeginAnnotation::class.java)!!
                    val tokenStart = m.get(TokenBeginAnnotation::class.java)!! - sentTokenStart
                    val tokenEnd = m.get(TokenEndAnnotation::class.java)!! - sentTokenStart
                    val length = tokenEnd - tokenStart
                    mentionTokenLengthCounter.incrementCount(length)
                    // Check if exact span
                    val span = t!!.span
                    if (span != null) {
                        if (span.source == tokenStart && span.target == tokenEnd - 1) {
                            mentionExactTreeSpan++
                        } else {
                            logger.info("Tree span is $span, tree node is $t")
                            logger.info("Mention span is " + tokenStart + " " + (tokenEnd - 1) + ", mention is " + m)
                        }
                    } else {
                        logger.warning("No span for $t")
                    }
                    val nptSpan = npt!!.span
                    if (nptSpan.source == tokenStart && nptSpan.target == tokenEnd - 1) {
                        nonPretermSpanMatches++
                        npt2 = npt
                    } else {
                        mentionTreePretermNonPretermNoMatchLabelCounter.incrementCount(t.label().value())
                        logger.info("NPT: Tree span is $span, tree node is $npt")
                        logger.info("NPT: Mention span is " + tokenStart + " " + (tokenEnd - 1) + ", mention is " + m)
                        val tlabel = t.label()
                        if (tlabel is CoreLabel) {
                            val mention: CoreMap = tlabel.get<CoreMap>(CorefMentionAnnotation::class.java)!!
                            val corefClusterId = mention.get(CorefCoreAnnotations.CorefAnnotation::class.java)
                            val clusteredMentions = doc.corefChainMap!![corefClusterId]
                            for (m2 in clusteredMentions!!) {
                                logger.info(
                                    "NPT: Clustered mention " + m2.get(
                                        TextAnnotation::class.java
                                    )
                                )
                            }
                        }
                    }
                    totalMentions++
                    mentionTreeLabelCounter.incrementCount(t.label().value())
                    mentionTreeNonPretermLabelCounter.incrementCount(npt.label().value())
                    mentionTreeMixedLabelCounter.incrementCount(npt2!!.label().value())
                    val tlabel = t.label()
                    if (tlabel is CoreLabel) {
                        if (tlabel.containsKey(NamedEntityAnnotation::class.java)) {
                            // walk up tree
                            nerMentions++
                            nerMentionTokenLengthCounter.incrementCount(length)
                            var parent = t.parent(root)
                            while (parent != null) {
                                val plabel = parent.label()
                                if (plabel is CoreLabel) {
                                    if (plabel.containsKey(NamedEntityAnnotation::class.java)) {
                                        logger.info("NER Mention: $m")
                                        val parentNerChunk = plabel.get<CoreMap>(NamedEntityAnnotation::class.java)!!
                                        logger.info("Nested inside NER Mention: $parentNerChunk")
                                        logger.info("Nested inside NER Mention parent node: $parent")
                                        nestedNerMentions++
                                        break
                                    }
                                }
                                parent = parent.parent(root)
                            }
                        }
                    }
                }
            }
        }

        override fun toString(): String {
            val sb = StringBuilder()
            appendIntCountStats(sb, "Mention Tree Labels (no preterminals)", mentionTreeNonPretermLabelCounter)
            sb.append("\n")
            appendIntCountStats(sb, "Mention Tree Labels (with preterminals)", mentionTreeLabelCounter)
            sb.append("\n")
            appendIntCountStats(
                sb,
                "Mention Tree Labels (preterminals with parent span not match)",
                mentionTreePretermNonPretermNoMatchLabelCounter
            )
            sb.append("\n")
            appendIntCountStats(sb, "Mention Tree Labels (mixed)", mentionTreeMixedLabelCounter)
            sb.append("\n")
            appendIntCountStats(sb, "Mention Lengths", mentionTokenLengthCounter)
            sb.append("\n")
            appendFrac(sb, "Mention Exact Non Preterm Tree Span", nonPretermSpanMatches, totalMentions)
            sb.append("\n")
            appendFrac(sb, "Mention Exact Tree Span", mentionExactTreeSpan, totalMentions)
            sb.append("\n")
            appendFrac(sb, "NER", nerMentions, totalMentions)
            sb.append("\n")
            appendFrac(sb, "Nested NER", nestedNerMentions, totalMentions)
            sb.append("\n")
            appendIntCountStats(sb, "NER Mention Lengths", nerMentionTokenLengthCounter)
            return sb.toString()
        }

        companion object {
            private fun appendFrac(sb: StringBuilder, label: String, num: Int, den: Int) {
                val frac = num.toDouble() / den
                sb.append(label).append("\t").append(frac).append("\t(").append(num).append("/").append(den).append(")")
            }

            private fun <E> appendIntCountStats(sb: StringBuilder, label: String, counts: IntCounter<E>) {
                sb.append(label).append("\n")
                val sortedKeys = Counters.toSortedList(counts)
                val total = counts.totalIntCount()
                for (key in sortedKeys) {
                    val count = counts.getIntCount(key)
                    appendFrac(sb, key.toString(), count, total)
                    sb.append("\n")
                }
            }
        }
    }

    companion object {
        /** A logger for this class  */
        private val log = Redwood.channels(CoNLL2011DocumentReader::class.java)
        private const val FIELD_LAST = -1
        private const val FIELD_DOC_ID = 0
        private const val FIELD_PART_NO = 1
        private const val FIELD_WORD_NO = 2
        private const val FIELD_WORD = 3
        private const val FIELD_POS_TAG = 4
        private const val FIELD_PARSE_BIT = 5

        //  private static final int FIELD_PRED_LEMMA = 6;
        //  private static final int FIELD_PRED_FRAMESET_ID = 7;
        //  private static final int FIELD_WORD_SENSE = 8;
        private const val FIELD_SPEAKER_AUTHOR = 9
        private const val FIELD_NER_TAG = 10

        //  private static final int FIELD_PRED_ARGS = 11;  // Predicate args follow...
        private const val FIELD_COREF = FIELD_LAST // Last field
        private const val FIELDS_MIN = 12 // There should be at least 13 fields
        val logger = Logger.getLogger(CoNLL2011DocumentReader::class.java.name)
        private fun getFiles(filepath: String?, filter: Pattern): List<File> {
            val iter = IOUtils.iterFilesRecursive(File(filepath), filter)
            val fileList: MutableList<File> = ArrayList()
            for (f in iter) {
                fileList.add(f)
            }
            Collections.sort(fileList)
            return fileList
        }

        private fun getField(fields: Array<String>, pos: Int): String {
            return if (pos == FIELD_LAST) {
                fields[fields.size - 1]
            } else {
                fields[pos]
            }
        }

        private fun concatField(sentWords: List<Array<String>>, pos: Int): String {
            val sb = StringBuilder()
            for (fields in sentWords) {
                if (sb.length > 0) {
                    sb.append(' ')
                }
                sb.append(getField(fields, pos))
            }
            return sb.toString()
        }

        fun usage() {
            log.info("java edu.stanford.nlp.dcoref.CoNLL2011DocumentReader [-ext <extension to match>] -i <inputpath> -o <outputfile>")
        }

        fun getMention(index: Int, corefG: String, sentenceAnno: List<CoreLabel>): Pair<Int, Int> {
            var i = -1
            var end = index
            for (newAnno in sentenceAnno) {
                i += 1
                if (i > index) {
                    val corefS = newAnno.get(CorefCoreAnnotations.CorefAnnotation::class.java)
                    end = if (corefS != null) {
                        val allC = corefS.split("\\|").toTypedArray()
                        if (Arrays.asList(*allC).contains(corefG)) {
                            i
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
            }
            return makePair(index, end)
        }

        fun include(
            sentenceInfo: Map<Pair<Int, Int>, String?>,
            mention: Pair<Int, Int>,
            corefG: String
        ): Boolean {
            val keys = sentenceInfo.keys
            for (key in keys) {
                val corefS = sentenceInfo[key]
                if (corefS != null && corefS == corefG) {
                    if (key.first!! < mention.first!! && key.second == mention.second) {
                        return true
                    }
                }
            }
            return false
        }

        fun writeTabSep(pw: PrintWriter, sentence: CoreMap, chainmap: CollectionValuedMap<String?, CoreMap>?) {
            val headFinder: HeadFinder = ModCollinsHeadFinder()
            val sentenceAnno = sentence.get(
                TokensAnnotation::class.java
            )!!
            val sentenceTree = sentence.get(TreeAnnotation::class.java)
            val sentenceInfo = Generics.newHashMap<Pair<Int, Int>, String?>()
            val sentenceSubTrees = sentenceTree!!.subTrees()
            sentenceTree.setSpans()
            val treeSpanMap = Generics.newHashMap<Pair<Int, Int>, Tree>()
            val wordSpanMap = Generics.newHashMap<Pair<Int, Int>, List<Tree>>()
            for (ctree in sentenceSubTrees) {
                val span = ctree.span
                if (span != null) {
                    treeSpanMap[makePair(span.source, span.target)] = ctree
                    wordSpanMap[makePair(span.source, span.target)] = ctree.getLeaves()
                }
            }
            val finalSentence: Array<Array<String?>?> = arrayOfNulls(sentenceAnno.size)
            val allHeads = Generics.newHashMap<Pair<Int, Int>, String?>()
            var index = -1
            for (newAnno in sentenceAnno) {
                index += 1
                val word = newAnno.word()
                val tag = newAnno.tag()
                val cat = newAnno.ner()
                val coref = newAnno.get(CorefCoreAnnotations.CorefAnnotation::class.java)
                finalSentence[index] = arrayOfNulls(4)
                val strings = finalSentence[index]!!
                strings[0] = word
                strings[1] = tag
                strings[2] = cat
                strings[3] = coref
                if (coref == null) {
                    sentenceInfo[makePair(index, index)] = coref
                    strings[3] = "O"
                } else {
                    val allC = coref.split("\\|").toTypedArray()
                    for (corefG in allC) {
                        val mention = getMention(index, corefG, sentenceAnno)
                        if (!include(sentenceInfo, mention, corefG)) {
                            // find largest NP in mention
                            sentenceInfo[mention] = corefG
                            val mentionTree = treeSpanMap[mention]
                            var head: String? = null
                            if (mentionTree != null) {
                                head = mentionTree.headTerminal(headFinder).nodeString()
                            } else if (mention.first == mention.second) {
                                head = word
                            }
                            allHeads[mention] = head
                        }
                    }
                    if (allHeads.values.contains(word)) {
                        strings[3] = "MENTION"
                    } else {
                        strings[3] = "O"
                    }
                }
            }
            for (i in finalSentence.indices) {
                val wordInfo = finalSentence[i]
                val wordInfo1 = wordInfo!!
                if (i < finalSentence.size - 1) {
                    val nextWordInfo = finalSentence[i + 1]
                    val nextWordInfo1 = nextWordInfo!!
                    if (nextWordInfo1[3] == "MENTION" && nextWordInfo1[0] == "'s") {
                        wordInfo1[3] = "MENTION"
                        finalSentence[i + 1]!![3] = "O"
                    }
                }
                pw.println(wordInfo1[0] + "\t" + wordInfo1[1] + "\t" + wordInfo1[2] + "\t" + wordInfo1[3])
            }
            pw.println("")
        }

        /** Reads and dumps output, mainly for debugging.  */
        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val props = StringUtils.argsToProperties(*args)
            val debug = java.lang.Boolean.parseBoolean(props.getProperty("debug", "false"))
            val filepath = props.getProperty("i")
            val outfile = props.getProperty("o")
            if (filepath == null || outfile == null) {
                usage()
                System.exit(-1)
            }
            val fout = PrintWriter(outfile)
            logger.info("Writing to $outfile")
            val ext = props.getProperty("ext")
            val options: Options
            options = if (ext != null) {
                Options(".*$ext$")
            } else {
                Options()
            }
            options.annotateTreeCoref = true
            options.annotateTreeNer = true
            val corpusStats = CorpusStats()
            val reader = CoNLL2011DocumentReader(filepath, options)
            var docCnt = 0
            var sentCnt = 0
            var tokenCnt = 0
            var doc: Document
            while (reader.nextDocument.also { doc = it!! } != null) {
                corpusStats.process(doc)
                docCnt++
                val anno = doc.annotation
                if (debug) println("Document " + docCnt + ": " + anno!!.get(DocIDAnnotation::class.java))
                for (sentence in anno!!.get(SentencesAnnotation::class.java)!!) {
                    if (debug) println("Parse: " + sentence.get(TreeAnnotation::class.java))
                    if (debug) println(
                        "Sentence Tokens: " + StringUtils.join(
                            sentence.get(
                                TokensAnnotation::class.java
                            ), ","
                        )
                    )
                    writeTabSep(fout, sentence, doc.corefChainMap)
                    sentCnt++
                    tokenCnt += sentence.get(TokensAnnotation::class.java)!!.size
                }
                if (debug) {
                    for (ner in doc.nerChunks!!) {
                        println("NER Chunk: $ner")
                    }
                    for (id in doc.corefChainMap!!.keys) {
                        println("Coref: " + id + " = " + StringUtils.join(doc.corefChainMap!![id], ";"))
                    }
                }
            }
            fout.close()
            println("Total document count: $docCnt")
            println("Total sentence count: $sentCnt")
            println("Total token count: $tokenCnt")
            println(corpusStats)
        }
    }
}