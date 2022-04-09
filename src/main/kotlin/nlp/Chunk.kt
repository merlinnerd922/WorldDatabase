package nlp

import SmartAny

/**
 * TODO
 */
class Chunk : SmartAny() {

    val tagList : MutableList<String> = mutableListOf();
    val tokenList : MutableList<String> = mutableListOf();

    internal fun addTokenTag(tag: String, token: String) {
        this!!.tagList.add(tag);
        tokenList.add(token);
    }
}

