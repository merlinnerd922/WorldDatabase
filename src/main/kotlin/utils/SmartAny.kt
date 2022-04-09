package utils

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.lang.reflect.Field

/**
 * A generic version of the "Any" class with smart implementations of equals(), hashCode() and toString().
 */
open class SmartAny {

    /**
     * A String builder that returns a String representation of an object, but only for non-null fields.
     */
    private val toStringBuilder: ReflectionToStringBuilder by lazy {

        // Initialize a StringBuilder that only builds with non-null fields.
        object : ReflectionToStringBuilder(
            this, ToStringStyle.SHORT_PREFIX_STYLE
        ) {
            override fun accept(field: Field): Boolean {
                return try {
                    super.accept(field) && field.get(this) != null
                } catch (e: IllegalAccessException) {
                    super.accept(field)
                }
            }
        }
    }

    override fun toString(): String {
        return toStringBuilder.toString()
    }

    override fun hashCode(): Int {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    override fun equals(other: Any?): Boolean {
        return EqualsBuilder.reflectionEquals(this, other)
    }
}
