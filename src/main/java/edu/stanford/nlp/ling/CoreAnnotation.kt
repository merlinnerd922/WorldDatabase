package edu.stanford.nlp.ling

import edu.stanford.nlp.util.TSMKey

/**
 * The base class for any annotation that can be marked on a [CoreMap],
 * parameterized by the type of the value associated with the annotation.
 * Subclasses of this class are the keys in the [CoreMap], so they are
 * instantiated only by utility methods in [CoreAnnotations].
 *
 * @author dramage
 * @author rafferty
 */
interface CoreAnnotation<V> : TSMKey<V> {
    /**
     * Returns the type associated with this annotation.  This method must
     * return the same class type as its value type parameter.  It feels like
     * one should be able to get away without this method, but because Java
     * erases the generic type signature, that info disappears at runtime.
     */
    val type: Class<V>?
}