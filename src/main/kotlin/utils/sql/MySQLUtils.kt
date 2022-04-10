@file:Suppress("unused")

package utils.sql

import java.sql.Connection

/**
 * A class of utilities for MySQL.
 */
public class MySQLUtils {
    companion object {

        /**
         * Convert the given list of objects into a String, the way it is formatted when being provided to an INSERT
         * statement as part of the VALUES clause.
         */
        public fun toInsertValuesStr(values: List<Any?>): String {

            // The values string should start and end with parentheses as markers.
            var builtString = "(";
            for ((index, value) in values.withIndex()) {

                // Given the current value to parse, add single quotes to indicate strings and the value "NULL" for null
                // strings; otherwise, just provide the value in its literal form.
                when (value) {
                    is String -> builtString += "'${value}'"
                    null -> builtString += "NULL"
                    else -> builtString += value
                }

                // Values should be separated by commas (no comma needed for the last element).
                if (index != values.size - 1) {
                    builtString += ", ";
                }

            }
            return "$builtString)";
        }
    }
}

/**
 * Update the value of the row/column entry in the table (schema).(table) within the column (setColumn) to (setValue),
 * where the provided condition (where) holds.
 */
private fun Connection.updateColumnValue(schema: String, table: String, setColumn: String, setValue: String, where: String): Int {
    val query = getPreparedUpdateStatement(
        schema = schema,
        table = table,
        setColumn = setColumn,
        setValue = setValue,
        where = where
    );
    return prepareStatement(query).executeUpdate();
}
