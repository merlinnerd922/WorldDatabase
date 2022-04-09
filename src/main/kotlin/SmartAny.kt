import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ReflectionToStringBuilder


open class SmartAny {
    override fun toString(): String {
        return ReflectionToStringBuilder.toString(this);
    }

    override fun hashCode(): Int {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    override fun equals(other: Any?): Boolean {
        return EqualsBuilder.reflectionEquals(this, other)
    }
}
