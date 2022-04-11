package exe.example

import com.google.common.base.Stopwatch
import edu.stanford.nlp.pipeline.CoreDocument
import nlp.stanford.getAnnotatedDocument
import org.threeten.extra.AmountFormats
import utils.JSONUtils
import utils.getResourceText
import utils.toJSONString
import utils.writeToMainResourceFile
import java.time.Duration
import java.util.*

fun main() {

    val stopwatch = Stopwatch.createStarted();
    var jasmineKennedyDocument: CoreDocument = getAnnotatedDocument(getResourceText("jasmineKennedie.txt"));
    var jsonString = jasmineKennedyDocument.toJSONString();
    writeToMainResourceFile("jasmineKennedyCoreDocumentJSON.json", jsonString)
    var coreDocument = JSONUtils.mapper.readValue(jsonString, CoreDocument::class.java);

    println("The processing took ${stopwatch.stop().elapsed().toPrettyString()}.");
}

private fun Duration.toPrettyString(): String {
    return AmountFormats.wordBased(this, Locale.getDefault());
}

