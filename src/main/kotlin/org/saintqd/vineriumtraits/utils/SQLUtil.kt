package org.saintqd.vineriumtraits.utils

import org.saintqd.vineriumtraits.VineriumTraits.Companion.inst
import java.sql.Connection
import java.sql.SQLException
import java.util.*

class SQLUtil {

    companion object {

        @Throws(SQLException::class)
        @Suppress("Unused")
        fun createDatabaseIfNotExists(connection: Connection, database: String) {
            connection.prepareStatement("CREATE DATABASE IF NOT EXISTS $database").use { statement ->

                statement.executeUpdate()
            }
        }

        fun checkIfTableExists(connection: Connection, table: String): Boolean {
            var tableExists = false
            try {
                connection.prepareStatement("SELECT 1 FROM $table LIMIT 1").use { statement ->
                    statement.executeQuery()
                    tableExists = true
                }
            } catch (e: SQLException) {
                if (!e.message!!.contains("doesn't exist")) e.printStackTrace()
            }
            return tableExists
        }

        @Throws(SQLException::class)
        fun getTableColumns(connection: Connection, table: String): HashMap<String, String> {
            val columns = hashMapOf<String,String>()
            connection.prepareStatement("SHOW COLUMNS FROM $table").use { statement ->
                val result = statement.executeQuery()
                while (result.next()) {
                    columns[result.getString("Field")] = result.getString("Type")
                }
            }
            return columns
        }

        @Throws(SQLException::class)
        fun checkIfTableMatchesStructure(
            connection: Connection,
            table: String,
            expectedColumns: HashMap<String, String>
        ): Boolean {
            return checkIfTableMatchesStructure(connection, table, expectedColumns, true)
        }

        @Throws(SQLException::class)
        fun checkIfTableMatchesStructure(
            connection: Connection,
            table: String,
            expectedColumns: HashMap<String, String>,
            showErrors: Boolean
        ): Boolean {
            val found: MutableList<String?> = LinkedList<String?>()
            for (entry in getTableColumns(connection, table).entries) {
                if (!expectedColumns.containsKey(entry.key)) continue  // only check columns that we're expecting

                val expectedType: String = expectedColumns[entry.key]!!
                val actualType: String = entry.value
                if (expectedType != actualType) {
                    if (showErrors) {
                        inst().logger.severe(
                            "Expected type " + expectedType + " for column " + entry.key + ", got " + actualType
                        )
                    }
                    return false
                }
                found.add(entry.key)
            }

            return HashSet<String?>(found).containsAll(expectedColumns.keys)
        }
    }
}