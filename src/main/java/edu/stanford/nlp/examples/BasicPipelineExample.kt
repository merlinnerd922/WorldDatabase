package edu.stanford.nlp.examples

import edu.stanford.nlp.pipeline.CoreDocument
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import java.util.*

object BasicPipelineExample {
    var text = "Joe Smith was born in California. " +
            "In 2017, he went to Paris, France in the summer. " +
            "His flight left at 3:00pm on July 10th, 2017. " +
            "After eating some escargot for the first time, Joe said, \"That was delicious!\" " +
            "He sent a postcard to his sister Jane Smith. " +
            "After hearing about Joe's trip, Jane decided she might go to France one day."

    @JvmStatic
    fun main(args: Array<String>) {
        // set up pipeline properties
        val props = Properties()
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,kbp,quote")
        // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
        props.setProperty("coref.algorithm", "neural")
        // build pipeline
        val pipeline = StanfordCoreNLP(props)
        // create a document object
        val document = CoreDocument(text)
        // annnotate the document
        pipeline.annotate(document)
        // examples

        // 10th token of the document
        val token = document.tokens()[9]
        println("Example: token")
        println(token)
        println()

        // text of the first sentence
        val sentenceText = document.sentences()!![0].text()
        println("Example: sentence")
        println(sentenceText)
        println()

        // second sentence
        val sentence = document.sentences()!![1]

        // list of the part-of-speech tags for the second sentence
        val posTags = sentence.posTags()
        println("Example: pos tags")
        println(posTags)
        println()

        // list of the ner tags for the second sentence
        val nerTags = sentence.nerTags()
        println("Example: ner tags")
        println(nerTags)
        println()

        // constituency parse for the second sentence
        val constituencyParse = sentence.constituencyParse()
        println("Example: constituency parse")
        println(constituencyParse)
        println()

        // dependency parse for the second sentence
        val dependencyParse = sentence.dependencyParse()
        println("Example: dependency parse")
        println(dependencyParse)
        println()

        // kbp relations found in fifth sentence
        val relations = document.sentences()!![4].relations()
        println("Example: relation")
        println(relations[0])
        println()

        // entity mentions in the second sentence
        val entityMentions = sentence.entityMentions()
        println("Example: entity mentions")
        println(entityMentions)
        println()

        // coreference between entity mentions
        val originalEntityMention = document.sentences()!![3].entityMentions()[1]
        println("Example: original entity mention")
        println(originalEntityMention)
        println("Example: canonical entity mention")
        println(originalEntityMention.canonicalEntityMention().get())
        println()

        // get document wide coref info
        val corefChains = document.corefChains()
        println("Example: coref chains for document")
        println(corefChains)
        println()

        // get quotes in document
        val quotes = document.quotes()
        val quote = quotes!![0]
        println("Example: quote")
        println(quote)
        println()

        // original speaker of quote
        // note that quote.speaker() returns an Optional
        println("Example: original speaker of quote")
        println(quote.speaker()!!)
        println()

        // canonical speaker of quote
        println("Example: canonical speaker of quote")
        println(quote.canonicalSpeaker()!!)
        println()
    }
}