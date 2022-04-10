package nlp.opennlp

import nlp.opennlp.chunk.*
import nlp.opennlp.chunk.populateListWithNames
import opennlp.tools.namefind.NameFinderME
import opennlp.tools.util.Span

private fun getNamesFromText(text: String): MutableList<String> {
    val detectedSentences =
        ENGLISH_SENTENCE_DETECTOR.detectSentencesAndProcess(text);
    val findNames = ENGLISH_NAME_FINDER.findNames(listOf(detectedSentences[0]));
    return findNames;
}

private fun NameFinderME.findNames(detectedSentences: List<Sentence>): MutableList<String> {
    val mutableListOf = mutableListOf<String>();
    for (sentence in detectedSentences) {
        var foundSpans: Array<Span>? = this.find(sentence.tokens);
        populateListWithNames(mutableListOf, foundSpans!!.toList(), sentence)
    }
    return mutableListOf;
}

private fun NameFinderME.find(tokens: List<Token>?): Array<Span>? {
    return this.find(tokens!!.map { it.tokenString }.toTypedArray());
}