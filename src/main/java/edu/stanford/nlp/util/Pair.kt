@file:Suppress("UNCHECKED_CAST")

package edu.stanford.nlp.util

import edu.stanford.nlp.util.logging.PrettyLoggable
import java.io.DataOutputStream
import java.lang.AssertionError
import edu.stanford.nlp.util.logging.Redwood.RedwoodChannels
import edu.stanford.nlp.util.logging.PrettyLogger
import java.io.Serializable
import java.lang.Exception
import java.util.Comparator

/**
 * Pair is a Class for holding mutable pairs of objects.
 *
 *
 * *Implementation note:*
 * On a 32-bit JVM uses ~ 8 (this) + 4 (first) + 4 (second) = 16 bytes.
 * On a 64-bit JVM uses ~ 16 (this) + 8 (first) + 8 (second) = 32 bytes.
 *
 *
 * Many applications use a lot of Pairs so it's good to keep this
 * number small.
 *
 * @author Dan Klein
 * @author Christopher Manning (added stuff from Kristina's, rounded out)
 * @version 2002/08/25
 */
public open class Pair<T1, T2> : Comparable<Pair<T1, T2>>, Serializable, PrettyLoggable {
    /**
     * Direct access is deprecated.  Use first().
     *
     * @serial
     */
    @JvmField
    public var first: T1? = null

    /**
     * Direct access is deprecated.  Use second().
     *
     * @serial
     */
    @JvmField
    public var second: T2? = null

    constructor() {
        // first = null; second = null; -- default initialization
    }

    constructor(first: T1, second: T2) {
        this.first = first
        this.second = second
    }

    public fun first(): T1? {
        return first
    }

    public fun second(): T2? {
        return second
    }

    public fun setFirstTo(newFirst : T1?) {
        first = newFirst;
    }

    public fun setSecondTo(newSecond : T2?) {
        second = newSecond;
    }

    override fun toString(): String {
        return "($first,$second)"
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Pair<*, *>) {
            val p = other
            (if (first == null) p.first() == null else first == p.first()) && if (second == null) p.second() == null else second == p.second()
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        val firstHash = if (first == null) 0 else first.hashCode()
        val secondHash = if (second == null) 0 else second.hashCode()
        return firstHash * 31 + secondHash
    }

    fun asList(): List<Any?> {
        return CollectionUtils.makeList(first, second)
    }

    /**
     * Write a string representation of a Pair to a DataStream.
     * The `toString()` method is called on each of the pair
     * of objects and a `String` representation is written.
     * This might not allow one to recover the pair of objects unless they
     * are of type `String`.
     */
    fun save(out: DataOutputStream) {
        try {
            out.writeUTF(first.toString())
            out.writeUTF(second.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Compares this `Pair` to another object.
     * If the object is a `Pair`, this function will work providing
     * the elements of the `Pair` are themselves comparable.
     * It will then return a value based on the pair of objects, where
     * `p > q iff p.first() > q.first() ||
     * (p.first().equals(q.first()) && p.second() > q.second())`.
     * If the other object is not a `Pair`, it throws a
     * `ClassCastException`.
     *
     * @param other the `Object` to be compared.
     * @return the value `0` if the argument is a
     * `Pair` equal to this `Pair`; a value less than
     * `0` if the argument is a `Pair`
     * greater than this `Pair`; and a value
     * greater than `0` if the argument is a
     * `Pair` less than this `Pair`.
     * @throws ClassCastException if the argument is not a
     * `Pair`.
     * @see java.lang.Comparable
     */
    override fun compareTo(other: Pair<T1, T2>): Int {
        if (first() is Comparable<*>) {
            val comp = (first() as Comparable<T1>?)!!.compareTo(other.first()!!)
            if (comp != 0) {
                return comp
            }
        }
        if (second() is Comparable<*>) {
            return (second() as Comparable<T2>?)!!.compareTo(other.second()!!)
        }
        if (first() !is Comparable<*> && second() !is Comparable<*>) {
            throw AssertionError("Neither element of pair comparable")
        }
        return 0
    }

    internal class MutableInternedPair : Pair<String?, String?> {
        constructor(p: Pair<String?, String?>) : super(p.first, p.second) {
            internStrings()
        }

        constructor(first: String, second: String) : super(first, second) {
            internStrings()
        }

        protected fun readResolve(): Any {
            internStrings()
            return this
        }

        private fun internStrings() {
            if (first != null) {
                first = first!!.intern()
            }
            if (second != null) {
                second = second!!.intern()
            }
        }

        companion object {
            // use serialVersionUID for cross version serialization compatibility
            private const val serialVersionUID = 1360822168806852922L
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun prettyLog(channels: RedwoodChannels, description: String) {
        PrettyLogger.log<Any>(channels, description, this.asList())
    }

    /**
     * Compares a `Pair` to another `Pair` according to the first object of the pair only
     * This function will work providing
     * the first element of the `Pair` is comparable, otherwise will throw a
     * `ClassCastException`
     * @author jonathanberant
     */
    class ByFirstPairComparator<T1, T2> : Comparator<Pair<T1, T2>> {
        override fun compare(pair1: Pair<T1, T2>, pair2: Pair<T1, T2>): Int {
            return (pair1.first() as Comparable<T1>).compareTo(pair2.first()!!)
        }
    }

    /**
     * Compares a `Pair` to another `Pair` according to the first object of the pair only in decreasing order
     * This function will work providing
     * the first element of the `Pair` is comparable, otherwise will throw a
     * `ClassCastException`
     * @author jonathanberant
     */
    class ByFirstReversePairComparator<T1, T2> : Comparator<Pair<T1, T2>> {
        override fun compare(pair1: Pair<T1, T2>, pair2: Pair<T1, T2>): Int {
            return -(pair1.first() as Comparable<T1>).compareTo(pair2.first()!!)
        }
    }

    /**
     * Compares a `Pair` to another `Pair` according to the second object of the pair only
     * This function will work providing
     * the first element of the `Pair` is comparable, otherwise will throw a
     * `ClassCastException`
     * @author jonathanberant
     */
    class BySecondPairComparator<T1, T2> : Comparator<Pair<T1, T2>> {
        override fun compare(pair1: Pair<T1, T2>, pair2: Pair<T1, T2>): Int {
            return (pair1.second() as Comparable<T2>).compareTo(pair2.second()!!)
        }
    }

    /**
     * Compares a `Pair` to another `Pair` according to the second object of the pair only in decreasing order
     * This function will work providing
     * the first element of the `Pair` is comparable, otherwise will throw a
     * `ClassCastException`
     * @author jonathanberant
     */
    class BySecondReversePairComparator<T1, T2> : Comparator<Pair<T1, T2>> {
        override fun compare(pair1: Pair<T1, T2>, pair2: Pair<T1, T2>): Int {
            return -(pair1.second() as Comparable<T2>).compareTo(pair2.second()!!)
        }
    }

    companion object {
        /**
         * Returns a Pair constructed from X and Y.  Convenience method; the
         * compiler will disambiguate the classes used for you so that you
         * don't have to write out potentially long class names.
         */
        @JvmStatic
        fun <X, Y> makePair(x: X, y: Y): Pair<X, Y> {
            return Pair(x, y)
        }

        /**
         * If first and second are Strings, then this returns an MutableInternedPair
         * where the Strings have been interned, and if this Pair is serialized
         * and then deserialized, first and second are interned upon
         * deserialization.
         *
         * @param p A pair of Strings
         * @return MutableInternedPair, with same first and second as this.
         */
        fun stringIntern(p: Pair<String?, String?>): Pair<String?, String?> {
            return MutableInternedPair(p)
        }

        /**
         * Returns an MutableInternedPair where the Strings have been interned.
         * This is a factory method for creating an
         * MutableInternedPair.  It requires the arguments to be Strings.
         * If this Pair is serialized
         * and then deserialized, first and second are interned upon
         * deserialization.
         *
         * *Note:* I put this in thinking that its use might be
         * faster than calling `x = new Pair(a, b).stringIntern()`
         * but it's not really clear whether this is true.
         *
         * @param first  The first object
         * @param second The second object
         * @return An MutableInternedPair, with given first and second
         */
        fun internedStringPair(first: String, second: String): Pair<String?, String?> {
            return MutableInternedPair(first, second)
        }

        /**
         * use serialVersionUID for cross version serialization compatibility
         */
        private const val serialVersionUID = 1360822168806852921L
    }
}