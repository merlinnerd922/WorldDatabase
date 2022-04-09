import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.lang.reflect.Field

/**
 * TODO
 */
open class SmartAny {
    override fun toString(): String {
        val myself: Any = this
        val builder: ReflectionToStringBuilder = object : ReflectionToStringBuilder(
            this, ToStringStyle.SHORT_PREFIX_STYLE
        ) {
            override fun accept(field: Field): Boolean {
                return try {
                    super.accept(field) && field.get(myself) != null
                } catch (e: IllegalAccessException) {
                    super.accept(field)
                }
            }
        }

        return builder.toString()
    }

    override fun hashCode(): Int {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    override fun equals(other: Any?): Boolean {
        return EqualsBuilder.reflectionEquals(this, other)
    }
}
