package webBrowsing

import utils.sql.MySQLDB
import utils.sql.executeQuery
import utils.sql.insert
import java.sql.Connection

class RPDRSqlUtils {
    companion object {
        private val connection: Connection get() = MySQLDB.MY_SQL_DB_CONNECTION

        fun insertQueen(name: String, ageAtTimeOfCompeting: Int, hometown: String) {
//            if (!queenExists(name)) {
                var splitName = name.split(" ");
                connection.insert("people", "people", listOf(null, splitName[0].replace("'", "''"), (if (splitName.size > 1) splitName[splitName.size - 1].replace("'", "''") else null), ageAtTimeOfCompeting, hometown))
//            }
        }

        private fun queenExists(queenName: String): Boolean {
            var splitName = queenName.split(" ");
            val countRow = connection.executeQuery(
                select = "count(*)",
                from = "people.people",
                where = "likelyFirstName = '${splitName[0]}' and likelyLastName = '${splitName[1]}'"
            )!!;
            countRow.next();
            return countRow.getInt(1) > 0;
        }

    }
}
