package utils

enum class SortOrder(var asSQLString: String) {
    ASCENDING("ASC"),DESCENDING("DESC");

    fun toSQLString(): String {
        return asSQLString;
    }
}
