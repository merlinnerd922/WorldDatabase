package utils.sql

import com.mysql.cj.jdbc.MysqlDataSource
import java.sql.Connection

class MySQLDB {

    companion object {
        private val MY_SQL_DB_REFERENCE : MySQLDB by lazy { MySQLDB() }
        public val MY_SQL_DB_CONNECTION : Connection by lazy { MY_SQL_DB_REFERENCE.dbConnection!! }
    }

    public var dbConnection: Connection? = null;

    init {
        val source = MysqlDataSource()
        source.user = "root"
        source.serverName = "localhost"
        source.port = 3306
        source.password = "";

        dbConnection = source.connection;
    }
}