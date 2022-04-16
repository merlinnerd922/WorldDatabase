//
// Annotation -- annotation protocol used by StanfordCoreNLP
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
package edu.stanford.nlp.pipeline

import edu.stanford.nlp.util.ArrayCoreMap
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.ling.CoreLabel
import java.lang.StringBuilder
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.ling.SentenceUtils
import kotlinx.serialization.Serializable
import java.util.ArrayList

/**
 * An annotation representing a span of text in a document.
 *
 * Basically just an implementation of CoreMap that knows about text.
 * You're meant to use the annotation keys in CoreAnnotations for common
 * cases, but can define bespoke ones for unusual annotations.
 *
 * @author Jenny Finkel
 * @author Anna Rafferty
 * @author bethard
 */
@Serializable
open class Annotation : ArrayCoreMap {
    /** Copy constructor.
     * @param map The new Annotation copies this one.
     */
    constructor(map: Annotation?) : super(map) {}

    /** Copies the map, but not a deep copy.
     * @return The copy
     */
    fun copy(): Annotation {
        return Annotation(this)
    }

    /**
     * The text becomes the CoreAnnotations.TextAnnotation of the newly
     * created Annotation.
     */
    constructor(text: String?) {
        this[TextAnnotation::class.java] = text
    }

    /** The basic toString() method of an Annotation simply
     * prints out the text over which any annotations have
     * been made (TextAnnotation). To print all the
     * Annotation keys, use `toShorterString();`.
     *
     * @return The text underlying this Annotation
     */
    override fun toString(): String {
        return this[TextAnnotation::class.java]
    }

    /** Make a new Annotation from a List of tokenized sentences.  */
    constructor(sentences: List<CoreMap>) : super() {
        this[SentencesAnnotation::class.java] = sentences
        val tokens: MutableList<CoreLabel> = ArrayList()
        val text = StringBuilder()
        for (sentence in sentences) {
            val sentenceTokens = sentence[TokensAnnotation::class.java]
            tokens.addAll(sentenceTokens)
            if (sentence.containsKey(TextAnnotation::class.java)) {
                text.append(sentence[TextAnnotation::class.java])
            } else {
                // If there is no text in the sentence, fake it as best as we can
                if (text.length > 0) {
                    text.append('\n')
                }
                text.append(SentenceUtils.listToString(sentenceTokens))
            }
        }
        this[TokensAnnotation::class.java] = tokens
        this[TextAnnotation::class.java] = text.toString()
    }

    // ==================
    // Old Deprecated API. This shouldn't be used. It's currently only used in old RTE code.
    // ==================
    @Deprecated("")
    constructor() : super(12) {
    }

    companion object {
        /**
         * SerialUID
         */
        private const val serialVersionUID = 1L
    }
}