package edu.stanford.nlp.util


/**
 * Type signature for a class that supports the basic operations required
 * of a typesafe heterogeneous map.
 *
 * @author dramage
 */
interface TypesafeMap {

    /**
     * Returns the value associated with the given key or null if
     * none is provided.
     */
    operator fun <VALUE> get(key: Class<out TSMKey<VALUE>?>?): VALUE?

    /**
     * Associates the given value with the given type for future calls
     * to get.  Returns the value removed or null if no value was present.
     */
    operator fun <VALUE> set(key: Class<out TSMKey<VALUE>?>?, value: VALUE): VALUE?

    /**
     * Removes the given key from the map, returning the value removed.
     */
    fun <VALUE> remove(key: Class<out TSMKey<VALUE>?>?): VALUE

    /**
     * Collection of keys currently held in this map.  Some implementations may
     * have the returned set be immutable.
     */
    fun <VALUE> keySet(): Set<Class<out TSMKey<VALUE>?>?>
    // Set<Class<? extends Key<?>>> keySet();
    /**
     * Returns true if contains the given key.
     */
    fun <VALUE> containsKey(key: Class<out TSMKey<VALUE?>?>?): Boolean

    /**
     * Returns the number of keys in the map.
     */
    fun size(): Int
}