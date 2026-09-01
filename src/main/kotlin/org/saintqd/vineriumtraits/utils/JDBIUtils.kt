package org.saintqd.vineriumtraits.utils

import org.jdbi.v3.core.Jdbi
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumtraits.VineriumTraits
import java.sql.SQLException
import java.util.*

class JDBIUtils {

    companion object {

        @Throws(SQLException::class)
        fun createDatabaseIfNotExists(jdbi: Jdbi, database: String) {
            jdbi.useHandle<SQLException> { handle ->
                handle.execute("CREATE DATABASE IF NOT EXISTS `" + database + "`")
            }
        }

        fun checkIfTableExists(jdbi: Jdbi, table: String): Boolean {
            var tableExists = false
            jdbi.useHandle<SQLException> { handle ->
                val metadata = handle.connection.metaData
                val resultSet = metadata.getTables(null, null,
                    table, null)
                tableExists = resultSet.next()
            }
            if (!tableExists) {
                VineriumTraits.inst().logger.warning { "Table $table does not exist." }
                return false
            }
            return true
        }

        @Throws(SQLException::class)
        fun getTableColumns(jdbi: Jdbi, table: String): MutableMap<String, String> {
            val columns = jdbi.withHandle<MutableMap<String, String>, SQLException> { handle ->
                handle.createQuery("SHOW COLUMNS FROM " + table)
                    .map {rs, _ ->
                        val columnsMap = hashMapOf<String, String>()
                        while (rs.next()) {
                            columnsMap.put(rs.getString("Field"), rs.getString("Type"))
                        }
                        return@map columnsMap
                    }
                    .findFirst()
                    .orElse(HashMap())
            }
            return columns
        }

        @Throws(SQLException::class)
        fun checkIfTableMatchesStructure(
            jdbi: Jdbi,
            table: String,
            expectedColumns: MutableMap<String, String>
        ): Boolean {
            return checkIfTableMatchesStructure(jdbi, table, expectedColumns, true)
        }

        @Throws(SQLException::class)
        fun checkIfTableMatchesStructure(
            jdbi: Jdbi,
            table: String,
            expectedColumns: MutableMap<String, String>,
            showErrors: Boolean
        ): Boolean {
            val found: MutableList<String> = LinkedList<String>()
            for (entry in getTableColumns(jdbi, table).entries) {
                if (!expectedColumns.containsKey(entry.key)) continue  // only check columns that we're expecting

                val expectedType: String = expectedColumns[entry.key]!!
                val actualType = entry.value
                if (expectedType != actualType) {
                    if (showErrors) {
                        VineriumLib.inst().logger.severe(
                            "Expected type " + expectedType + " for column " + entry.key + ", got " + actualType
                        )
                    }
                    return false
                }
                found.add(entry.key)
            }

            return HashSet(found).containsAll(expectedColumns.keys)
        }
    }
}