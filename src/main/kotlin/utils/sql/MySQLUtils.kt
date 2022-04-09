@file:Suppress("unused")

package utils.sql

import java.sql.Connection

/**
 * TODO
 */
public class MySQLUtils {
    companion object {

        /**
         * Convert the given list of objects into a String, the way it is formatted when being provided to an INSERT
         * statement as part of the VALUES clause.
         */
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
