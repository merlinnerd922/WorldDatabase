package edu.stanford.nlp.util

import kotlin.jvm.JvmOverloads
import java.lang.ArrayIndexOutOfBoundsException
import java.lang.RuntimeException
import java.lang.StringBuilder
import kotlin.Throws
import java.io.IOException
import java.io.ObjectOutputStream
import edu.stanford.nlp.util.logging.Redwood.RedwoodChannels
import edu.stanford.nlp.util.logging.Redwood
import edu.stanford.nlp.util.logging.PrettyLogger
import utils.mutableErasedTypeListOfNulls
import utils.mutableListOfNulls
import java.lang.ThreadLocal
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

typealias TSMKeyClass<T> = Class<out TSMKey<T>?>?

/**
 * Base implementation of [CoreMap] backed by two Java arrays.
 *
 *
 * Reasonable care has been put into ensuring that this class is both fast and
 * has a light memory footprint.
 *
 *
 * Note that like the base classes in the Collections API, this implementation
 * is *not thread-safe*. For speed reasons, these methods are not
 * synchronized. A synchronized wrapper could be developed by anyone so
 * inclined.
 *
 *
 * Equality is defined over the complete set of keys and values currently
 * stored in the map.  Because this class is mutable, it should not be used
 * as a key in a Map.
 *
 * @author dramage
 * @author rafferty
 */
open class ArrayCoreMap : CoreMap /*, Serializable */ {
    /** Array of keys  */
    private var keys: MutableList<TSMKeyClass<Any>>

    /** Array of values  */
    private var values: MutableList<Any?>

    /** Total number of elements actually in keys,values  */
    private var size // = 0;
            = 0
    /**
     * Initializes this ArrayCoreMap, pre-allocating arrays to hold
     * up to capacity key,value pairs.  This array will grow if necessary.
     *
     * @param capacity Initial capacity of object in key,value pairs
     */
    /**
     * Default constructor - initializes with default initial annotation
     * capacity of 4.
     */
    @JvmOverloads
    constructor(capacity: Int = INITIAL_CAPACITY) {
        keys = ErasureUtils.uncheckedCast(arrayOfNulls<Class<*>>(capacity))
        values = mutableListOfNulls(capacity)
        // size starts at 0
    }



    /**
     * Copy constructor.
     * @param other The ArrayCoreMap to copy. It may not be null.
     */
    constructor(other: ArrayCoreMap) {
        size = other.size
        keys = other.keys.toMutableList();
        values = other.values.toMutableList();
    }

    /**
     * Copy constructor.
     * @param other The ArrayCoreMap to copy. It may not be null.
     */
    constructor(other: CoreMap) {
        val otherKeys = other.keySet<Any>()
        size = otherKeys.size
        keys = mutableListOfNulls(size)
        values = mutableListOfNulls(size)
        for ((i, key) in otherKeys.withIndex()) {
            keys[i] = key
            values[i] = other.get<Any>(key)!!
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun <VALUE> get(key: Class<out TSMKey<VALUE>?>?): VALUE? {
        for (i in 0 until size) {
            if (key == keys[i]) {
                if (listener != null) {
                    listener!!.accept(key) // For tracking which entities were returned by the CoreMap
                }
                return values[i] as VALUE
            }
        }
        return null
    }

    /**
     * {@inheritDoc}
     */
    override fun <VALUE> set(key: TSMKeyClass<VALUE>, value: VALUE): VALUE? {

        // search array for existing value to replace
        for (i in 0 until size) {
            if (keys[i] == key) {
                val rv: VALUE = values[i] as VALUE
                values[i] = value!!
                return rv
            }
        }
        // not found in arrays, add to end ...

        // increment capacity of arrays if necessary
        if (size >= keys.size) {
            val capacity = keys.size + if (keys.size < 16) 4 else 8
            val newKeys = mutableListOfNulls<Class<out TSMKey<Any>?>?>(capacity)
            val newValues = mutableListOfNulls<Any?>(capacity)
            System.arraycopy(keys, 0, newKeys, 0, size)
            System.arraycopy(values, 0, newValues, 0, size)
            keys = newKeys
            values = newValues
        }

        // store value
        keys[size] = key as TSMKeyClass<Any>;
        values[size] = value
        size++
        return null
    }

    /**
     * {@inheritDoc}
     */
    override fun <VALUE> keySet(): Set<TSMKeyClass<VALUE>> {
        val hashSet = HashSet<TSMKeyClass<VALUE>>()
        for (key: TSMKeyClass<Any> in keys) {
            hashSet.add(key as TSMKeyClass<VALUE>);
        }
        return hashSet;
    }

    /**
     * Return a set of keys such that the value of that key is not null.
     *
     * @return A hash set such that each element of the set is a key in this CoreMap that has a
     * non-null value.
     */
    fun keySetNotNull(): Set<Class<*>> {
        val mapKeys: MutableSet<Class<*>> = IdentityHashSet()
        for (i in 0 until size()) {
            if (values[i] != null) {
                mapKeys.add(keys[i]!!)
            }
        }
        return mapKeys
    }

    /**
     * {@inheritDoc}
     */
    override fun <VALUE> remove(key: Class<out TSMKey<VALUE>?>?): VALUE {
        var rv: Any? = null
        for (i in 0 until size) {
            if (keys[i] == key) {
                rv = values[i]
                if (i < size - 1) {
                    System.arraycopy(keys, i + 1, keys, i, size - (i + 1))
                    System.arraycopy(values, i + 1, values, i, size - (i + 1))
                }
                size--
                break
            }
        }
        return rv as VALUE
    }

    /**
     * {@inheritDoc}
     * @param key
     */
    override fun <VALUE> containsKey(key: Class<out TSMKey<VALUE?>?>?): Boolean {
        for (i in 0 until size) {
            if (keys[i] == key) {
                return true
            }
        }
        return false
    }

    /**
     * Reduces memory consumption to the minimum for representing the values
     * currently stored stored in this object.
     */
    fun compact() {
        if (keys.size > size) {
            val newKeys: MutableList<Class<*>?> = mutableListOfNulls<Class<*>?>(size)
            val newValues = mutableListOfNulls<Any?>(size)
            System.arraycopy(keys, 0, newKeys, 0, size)
            System.arraycopy(values, 0, newValues, 0, size)
            keys = ErasureUtils.uncheckedCast(newKeys)
            values = newValues
        }
    }

    fun setCapacity(newSize: Int) {
        if (size > newSize) {
            throw RuntimeException("You cannot set capacity to smaller than the current size.")
        }
        val newKeys: MutableList<Class<*>?> = mutableListOfNulls(newSize)
        val newValues = mutableListOfNulls<Any?>(newSize)
        System.arraycopy(keys, 0, newKeys, 0, size)
        System.arraycopy(values, 0, newValues, 0, size)
        keys = ErasureUtils.uncheckedCast(newKeys)
        values = newValues
    }

    /**
     * Returns the number of elements in this map.
     * @return The number of elements in this map.
     */
    override fun size(): Int {
        return size
    }

    /** Prints a full dump of a CoreMap. This method is robust to
     * circularity in the CoreMap.
     *
     * @return A String representation of the CoreMap
     */
    override fun toString(): String {
        val calledSet = toStringCalled.get()
        val createdCalledSet = calledSet.isEmpty()
        if (calledSet.contains(this)) {
            return "[...]"
        }
        calledSet.add(this)
        val s = StringBuilder("[")
        for (i in 0 until size) {
            s.append(keys[i]!!.simpleName)
            s.append('=')
            s.append(values[i])
            if (i < size - 1) {
                s.append(' ')
            }
        }
        s.append(']')
        if (createdCalledSet) {
            toStringCalled.remove()
        } else {
            // Remove the object from the already called set so that
            // potential later calls in this object graph have something
            // more description than [...]
            calledSet.remove(this)
        }
        return s.toString()
    }

    /**
     * {@inheritDoc}
     */
    override fun toShorterString(vararg what: String?): String? {
        val s = StringBuilder(SHORTER_STRING_CHARSTRING_START_SIZE)
        s.append('[')
        var whatSet: Set<String?>? = null
        if (size > SHORTER_STRING_MAX_SIZE_BEFORE_HASHING && what.size > SHORTER_STRING_MAX_SIZE_BEFORE_HASHING) {
            // if there's a lot of stuff, hash.
            whatSet = HashSet(mutableListOf(*what))
        }
        for (i in 0 until size) {
            val klass: Class<*> = keys[i]!!
            var name = shortNames[klass]
            if (name == null) {
                name = klass.simpleName
                val annoIdx = name.lastIndexOf("Annotation")
                if (annoIdx >= 0) {
                    name = name.substring(0, annoIdx)
                }
                shortNames[klass] = name!!
            }
            var include: Boolean
            if (what.size == 0) {
                include = true
            } else if (whatSet != null) {
                include = whatSet.contains(name)
            } else {
                include = false
                for (item in what) {
                    if (item == name) {
                        include = true
                        break
                    }
                }
            }
            if (include) {
                if (s.length > 1) {
                    s.append(' ')
                }
                s.append(name)
                s.append('=')
                s.append(values[i])
            }
        }
        s.append(']')
        return s.toString()
    }

    /** This gives a very short String representation of a CoreMap
     * by leaving it to the content to reveal what field is being printed.
     *
     * @param what An array (varargs) of Strings that say what annotation keys
     * to print.  These need to be provided in a shortened form where you
     * are just giving the part of the class name without package and up to
     * "Annotation". That is,
     * edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation
     * ➔ PartOfSpeech . As a special case, an empty array means
     * to print everything, not nothing.
     * @return Brief string where the field values are just separated by a
     * character. If the string contains spaces, it is wrapped in "{...}".
     */
    fun toShortString(vararg what: String?): String? {
        return toShortString('/', *what)
    }

    /** This gives a very short String representation of a CoreMap
     * by leaving it to the content to reveal what field is being printed.
     *
     * @param separator Character placed between fields in output
     * @param what An array (varargs) of Strings that say what annotation keys
     * to print.  These need to be provided in a shortened form where you
     * are just giving the part of the class name without package and up to
     * "Annotation". That is,
     * edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation
     * ➔ PartOfSpeech . As a special case, an empty array means
     * to print everything, not nothing.
     * @return Brief string where the field values are just separated by a
     * character. If the string contains spaces, it is wrapped in "{...}".
     */
    fun toShortString(separator: Char, vararg what: String?): String? {
        val s = StringBuilder()
        for (i in 0 until size) {
            var include: Boolean
            if (what.isNotEmpty()) {
                var name = keys[i]!!.simpleName
                val annoIdx = name.lastIndexOf("Annotation")
                if (annoIdx >= 0) {
                    name = name.substring(0, annoIdx)
                }
                include = false
                for (item in what) {
                    if (item == name) {
                        include = true
                        break
                    }
                }
            } else {
                include = true
            }
            if (include) {
                if (s.isNotEmpty()) {
                    s.append(separator)
                }
                s.append(values[i])
            }
        }
        val answer = s.toString()
        return if (answer.indexOf(' ') < 0) {
            answer
        } else {
            "{$answer}"
        }
    }

    /**
     * Two CoreMaps are equal iff all keys and values are .equal.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is CoreMap) {
            return false
        }
        if (other is HashableCoreMap) {
            // overridden behavior for HashableCoreMap
            return other == this
        }
        if (other is ArrayCoreMap) {
            // specialized equals for ArrayCoreMap
            return equals(other)
        }

        // TODO: make the general equality work in the situation of loops in the object graph

        // general equality
        if (keySet<Any?>() != (other as TypesafeMap).keySet<Any?>()) {
            return false
        }
        for (key in keySet<Any?>()) {
            if (!(other as TypesafeMap).containsKey<Any?>(key)) {
                return false
            }
            val thisV = this.get<Any?>(key)
            val otherV = (other as TypesafeMap).get<Any?>(key)
            if (thisV === otherV) {
                continue
            }
            // the two values must be unequal, so if either is null, the other isn't
            if (thisV == null || otherV == null) {
                return false
            }
            if (thisV != otherV) {
                return false
            }
        }
        return true
    }

    private fun equals(other: ArrayCoreMap): Boolean {
        var calledMap = equalsCalled.get()
        val createdCalledMap = calledMap == null
        if (createdCalledMap) {
            calledMap = TwoDimensionalMap.identityHashMap()
            equalsCalled.set(calledMap)
        }

        // Note that for the purposes of recursion, we assume the two maps
        // are equals.  The two maps will therefore be equal if they
        // encounter each other again during the recursion unless there is
        // some other key that causes the equality to fail.
        // We do not need to later put false, as the entire call to equals
        // will unwind with false if any one equality check returns false.
        // TODO: since we only ever keep "true", we would rather use a
        // TwoDimensionalSet, but no such thing exists
        if (calledMap!!.contains(this, other)) {
            return true
        }
        var result = true
        calledMap.put(this, other, true)
        calledMap.put(other, this, true)
        if (size != other.size) {
            result = false
        } else {
            for (i in 0 until size) {
                // test if other contains this key,value pair
                var matched = false
                for (j in 0 until other.size) {
                    if (keys[i] == other.keys[j]) {
                        if (values[i] == null && other.values[j] != null ||
                            values[i] != null && other.values[j] == null
                        ) {
                            // matched = false; // must be true
                            break
                        }
                        if (values[i] == other.values[j]) {
                            matched = true
                            break
                        }
                    }
                }
                if (!matched) {
                    result = false
                    break
                }
            }
        }
        if (createdCalledMap) {
            equalsCalled.set(null)
        }
        return result
    }

    /**
     * Returns a composite hashCode over all the keys and values currently
     * stored in the map.  Because they may change over time, this class
     * is not appropriate for use as map keys.
     */
    override fun hashCode(): Int {
        var calledSet = hashCodeCalled.get()
        val createdCalledSet = calledSet == null
        if (createdCalledSet) {
            calledSet = IdentityHashSet()
            hashCodeCalled.set(calledSet)
        }
        if (calledSet!!.contains(this)) {
            return 0
        }
        calledSet.add(this)
        var keysCode = 0
        var valuesCode = 0
        for (i in 0 until size) {
            keysCode += if (i < keys.size && values[i] != null) keys[i].hashCode() else 0
            valuesCode += if (i < values.size && values[i] != null) values[i].hashCode() else 0
        }
        if (createdCalledSet) {
            hashCodeCalled.set(null)
        } else {
            // Remove the object after processing is complete so that if
            // there are multiple instances of this CoreMap in the overall
            // object graph, they each have their hash code calculated.
            // TODO: can we cache this for later?
            calledSet.remove(this)
        }
        return keysCode * 37 + valuesCode
    }

    /**
     * Overridden serialization method: compacts our map before writing.
     *
     * @param out Stream to write to
     * @throws IOException If IO error
     */
    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        compact()
        out.defaultWriteObject()
    }
    // TODO: make prettyLog work in the situation of loops in the object graph
    /**
     * {@inheritDoc}
     */
    override fun prettyLog(channels: RedwoodChannels, description: String) {
        Redwood.startTrack(description)

        // sort keys by class name
        val sortedKeys: ArrayList<Class<out TSMKey<Any>?>?> = ArrayList(keySet())
        sortedKeys.sortWith(Comparator.comparing { obj: Class<out TSMKey<Any>?>? -> obj!!.canonicalName })

        // log key/value pairs
        for (key in sortedKeys) {
            val keyName = key!!.canonicalName.replace("class ", "")
            val value = this.get(key)
            if (PrettyLogger.dispatchable(value)) {
                PrettyLogger.log<Any>(channels, keyName, value)
            } else {
                channels.logf("%s = %s", keyName, value)
            }
        }
        Redwood.endTrack(description)
    }

    companion object {
        /**
         * A listener for when a key is retrieved by the CoreMap.
         * This should only be used for testing.
         */
        @JvmField
        var listener // = null;
                : Consumer<Class<out TSMKey<*>?>?>? = null

        /** Initial capacity of the array  */
        private const val INITIAL_CAPACITY = 4

        /**
         * Keeps track of which ArrayCoreMaps have had toString called on
         * them.  We do not want to loop forever when there are cycles in
         * the annotation graph.  This is kept on a per-thread basis so that
         * each thread where toString gets called can keep track of its own
         * state.  When a call to toString is about to return, this is reset
         * to null for that particular thread.
         */
        private val toStringCalled = ThreadLocal.withInitial<IdentityHashSet<CoreMap>> { IdentityHashSet() }

        // support caching of String form of keys for speedier printing
        private val shortNames = ConcurrentHashMap<Class<*>, String?>(12, 0.75f, 1)
        private const val SHORTER_STRING_CHARSTRING_START_SIZE = 64
        private const val SHORTER_STRING_MAX_SIZE_BEFORE_HASHING = 5

        /**
         * Keeps track of which pairs of ArrayCoreMaps have had equals
         * called on them.  We do not want to loop forever when there are
         * cycles in the annotation graph.  This is kept on a per-thread
         * basis so that each thread where equals gets called can keep
         * track of its own state.  When a call to toString is about to
         * return, this is reset to null for that particular thread.
         */
        private val equalsCalled = ThreadLocal<TwoDimensionalMap<CoreMap, CoreMap, Boolean>?>()

        /**
         * Keeps track of which ArrayCoreMaps have had hashCode called on
         * them.  We do not want to loop forever when there are cycles in
         * the annotation graph.  This is kept on a per-thread basis so that
         * each thread where hashCode gets called can keep track of its own
         * state.  When a call to toString is about to return, this is reset
         * to null for that particular thread.
         */
        private val hashCodeCalled = ThreadLocal<IdentityHashSet<CoreMap>?>()
        //
        // serialization magic
        //
        /** Serialization version id  */
        private const val serialVersionUID = 1L
    }
}