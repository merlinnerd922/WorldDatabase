package utils

enum class SortOrder(private var asSQLString: String) {
    ASCENDING("ASC"),DESCENDING("DESC");

    fun toSQLString(): String {
        return asSQLString;
    }
}
