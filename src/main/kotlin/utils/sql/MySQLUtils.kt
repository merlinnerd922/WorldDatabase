package utils.sql

import java.sql.Connection

/**
 * TODO
 */
public class MySQLUtils {
    companion object {
        public fun toInsertValuesStr(values: List<Any?>): String {
            var builtString = "(";
            for ((index, value) in values.withIndex()) {

                // TODO
                when (value) {
                    is String -> builtString += "'${value}'"
                    null -> builtString += "NULL"
                    else -> builtString += value
                }

                // TODO
                if (index != values.size - 1) {
                    builtString += ", ";
                }

            }
            return "$builtString)";
        }
    }
}

private fun Connection.update(schema: String, table: String, setColumn: String, setValue: String, where: String): Int {
    val query = getPreparedUpdateStatement(
        schema = schema,
        table = table,
        setColumn = setColumn,
        setValue = setValue,
        where = where
    );
    return prepareStatement(query).executeUpdate();
}
