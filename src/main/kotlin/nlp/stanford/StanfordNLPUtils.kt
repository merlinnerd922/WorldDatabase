package nlp.stanford

import edu.stanford.nlp.pipeline.CoreDocument
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import java.util.*

fun getStanfordPipeline(): StanfordCoreNLP {
    // set up pipeline properties
    val props = Properties()
    // set the list of annotators to run
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,kbp,quote")
    // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
    props.setProperty("coref.algorithm", "neural")
    // build pipeline
    val pipeline = StanfordCoreNLP(props)
    return pipeline
}

fun getAnnotatedDocument(docText: String): CoreDocument {
    val pipeline = getStanfordPipeline()

    // create a document object
    val document = CoreDocument(docText)
    // annnotate the document
    pipeline.annotate(document)
    return document
}