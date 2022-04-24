package utils


/**
 * TODO
 */
public fun <T> mutableListOfNulls(capacity: Int): MutableList<T?> {
    return MutableList(capacity) {null};
}

/**
 * TODO
 */
public fun mutableErasedTypeListOfNulls(capacity: Int): MutableList<*> {
    return MutableList(capacity) {null};
}