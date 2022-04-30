package utils

/**
 * TODO
 */
internal fun String.matches(regex: String): Boolean {
    return this.matches(Regex(regex));
}