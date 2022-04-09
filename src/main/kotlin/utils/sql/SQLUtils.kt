@file:Suppress("unused")

package utils.sql

import utils.sql.SQLUtils.Companion.appendGroupByIfNonNull
import utils.sql.SQLUtils.Companion.appendLimitIfNonNull
import utils.sql.SQLUtils.Companion.appendOrderByIfNonNull
import utils.SortOrder
import utils.sql.SQLUtils.Companion.appendWhereIfNonNull
import java.sql.Connection
import java.sql.ResultSet

/**
 * Return the current row of this result set as a list.
 */
fun ResultSet.getCurrentRowAsArray(): List<Any> {
    val returnList = ArrayList<Any>();
    for (i in 1 until metaData.columnCount + 1) {
        returnList.add(this.getObject(i))
    }
    return returnList;
}

/**
 * A class of SQL-related helper methods.
 */
class SQLUtils {

    companion object {
        /**
         * Append a clause to the provided SQL query string that imposes a limit on the number of rows returned,
         * if (limit) is provided.
         */
        fun appendLimitIfNonNull(sqlString: String, limit: Int?): String {

            if (limit == null) {
                return sqlString;
            }
            return "$sqlString LIMIT $limit";
        }

        /**
         * If (orderBy) is provided, append that String as an ORDER BY subclause to the provided SQL String.
         * Then, if (sortOrder) is provided, use that sort order in the provided (sqlString).
         *
         * If providing a (sortOrder), the (orderBy) clause must also be provided.
         */
        internal fun appendOrderByIfNonNull(sqlString: String, orderBy: String?, sortOrder: SortOrder?): String {

            var retStr = sqlString;

            // Append info on "ORDER BY" criteria, if provided.
            if (orderBy != null) {
                retStr = "$retStr ORDER BY $orderBy";

                // Append the order in which things should be sorted, if provided.
                if (sortOrder != null) {
                    retStr = "$retStr ${sortOrder.toSQLString()}"
                }
            }

            // Raise an issue if a sort order is provided without providing criteria.
            else if (sortOrder != null) {
                throw IllegalArgumentException("Cannot sort without criteria!")
            }
            return retStr;
        }

        /**
         * Append the provided (groupBy) string as a GROUP BY clause to the given (sqlString), if it is non-null.
         */
        fun appendGroupByIfNonNull(sqlString: String, groupBy: String?): String {
            if (groupBy == null) {
                return sqlString;
            }
            return "$sqlString GROUP BY $groupBy";
        }


        /**
         * Append the provided (where) string as a WHERE clause to the given string (sqlString), if it is non-null.
         */
        fun appendWhereIfNonNull(sqlString: String, where: String?): String {
            if (where == null) {
                return sqlString;
            }
            return "$sqlString WHERE $where";
        }


    }

}


/**
 * Navigate to the next row and return that row as an array.
 */
internal fun ResultSet?.skipAndGetNextRowAsArray(): List<Any> {
    this!!.next();
    return getCurrentRowAsArray()
}

/**
 * Select ALL rows from the given table.
 */
fun Connection?.selectAllFrom(tableName: String): ResultSet? = this!!.executeQuery(
    select = "*",
    from = tableName
)

internal fun getPreparedUpdateStatement(
    schema: String,
    table: String,
    setColumn: String,
    setValue: String,
    where: String
) = "update `${schema}`.`${table}` SET `${setColumn}` = '${setValue.replace("'", "\\'")}' WHERE $where"

/**
 * Insert the given (values) into the provided schema's table.
 */
fun Connection.insert(schema: String, table: String, values: List<Any?>) {
    val queryStr = "insert into ${schema}.${table} values ${MySQLUtils.toInsertValuesStr(values)}"
    this.prepareStatement(queryStr).execute();
}


/**
 * Execute the query specified by the given parameters corresponding to various SQL keywords.
 */
fun Connection.executeQuery(
    select: String,
    from: String,
    groupBy: String? = null,
    orderBy: String? = null,
    sortOrder: SortOrder? = null,
    limit: Int? = null,
    where: String? = null
): ResultSet? {

    // Form the base query, with all required elements.
    var builtString = "select $select from $from";

    // Append all query parameters.
    builtString = appendWhereIfNonNull(builtString, where);
    builtString = appendGroupByIfNonNull(builtString, groupBy);
    builtString = appendOrderByIfNonNull(builtString, orderBy, sortOrder);
    builtString = appendLimitIfNonNull(builtString, limit)
    return this.prepareStatement(builtString).executeQuery();
}

/**
 * Delete all contents from the given table within the given schema, then turn on safe update mode to prevent any
 * further deletions.
 */
fun Connection.deleteContentsThenSecure(schema: String, table: String) {
    executeStatement("SET SQL_SAFE_UPDATES = 0");
    executeStatement("DELETE FROM $schema.$table");
    executeStatement("SET SQL_SAFE_UPDATES = 1");
}

/**
 * Execute the provided statement.
 */
private fun Connection.executeStatement(statement: String) {
    this.prepareStatement(statement).execute()
}