package org.saintqd.vineriumtraits.storage

import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.jdbi.v3.core.Jdbi
import org.saintqd.vineriumlib.utils.SQLUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.UUID

@Deprecated("Deprecated in favor of MySqlJdbiStorage")
class MySQLStorage : DataStorage {

    val dataSource = HikariDataSource()
    val connection: Connection
    val ownerTable : String
    val traitsTable : String

    companion object {
        const val OWNER_TABLE_COLUMN_ID_NAME = "id"
        const val OWNER_TABLE_COLUMN_UUID_NAME = "uuid"
        const val OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME = "select_cooldown"
        const val TRAITS_TABLE_COLUMN_ID_NAME = "id"
        const val TRAITS_TABLE_COLUMN_OWNER_ID_NAME = "owner_id"
        const val TRAITS_TABLE_COLUMN_TRAIT_NAME = "trait_name"
    }

    fun setupDataSource() {
        dataSource.jdbcUrl = VineriumTraits.inst().config.getString("Database.Url","")
        dataSource.username = VineriumTraits.inst().config.getString("Database.Username","")
        dataSource.password = VineriumTraits.inst().config.getString("Database.Password","")
    }

    init {
        setupDataSource()

        connection = dataSource.connection
        var tablePrefix = VineriumTraits.inst().config.getString("Database.TablePrefix","")!!
        tablePrefix = if (tablePrefix.isBlank()) "" else tablePrefix + "_"
        ownerTable = tablePrefix + "trait_owners"
        traitsTable = tablePrefix + "traits"

        if (SQLUtils.checkIfTableExists(connection,ownerTable)) {
            val expected = hashMapOf(
                Pair(OWNER_TABLE_COLUMN_UUID_NAME,"varchar(36)"),
                Pair(OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME,"bigint unsigned")
            )
            if (!SQLUtils.checkIfTableMatchesStructure(connection,ownerTable,expected)) {
                throw SQLException("JDBC table $ownerTable does not match expected structure")
            }
        }
        else {
            connection.prepareStatement(
                "create table $ownerTable\n" +
                        "(\n" +
                        "    $OWNER_TABLE_COLUMN_ID_NAME    int unsigned auto_increment primary key,\n" +
                        "    $OWNER_TABLE_COLUMN_UUID_NAME    varchar(36) not null,\n" +
                        "    $OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME    bigint unsigned not null default 0,\n" +
                        "    constraint owner_traits_uuid_uindex unique ($OWNER_TABLE_COLUMN_UUID_NAME)\n" +
                        ");")
                .use { statement -> statement.executeUpdate() }
        }

        if (SQLUtils.checkIfTableExists(connection,traitsTable)) {
            val expected = hashMapOf(
                Pair(TRAITS_TABLE_COLUMN_OWNER_ID_NAME,"int unsigned"),
                Pair(TRAITS_TABLE_COLUMN_TRAIT_NAME,"varchar(64)")
            )
            if (!SQLUtils.checkIfTableMatchesStructure(connection,traitsTable,expected)) {
                throw SQLException("JDBC table $traitsTable does not match expected structure")
            }
        }
        else {
            connection.prepareStatement(
                "create table $traitsTable\n" +
                        "(\n" +
                        "    $TRAITS_TABLE_COLUMN_ID_NAME    bigint unsigned auto_increment primary key,\n" +
                        "    $TRAITS_TABLE_COLUMN_OWNER_ID_NAME    int unsigned,\n" +
                        "    $TRAITS_TABLE_COLUMN_TRAIT_NAME    varchar(64) not null,\n" +
                        "    foreign key    ($TRAITS_TABLE_COLUMN_OWNER_ID_NAME) references $ownerTable($OWNER_TABLE_COLUMN_ID_NAME) on delete cascade on update cascade\n" +
                        ");")
                .use { statement -> statement.executeUpdate() }
        }
    }

    override fun save() {
        if (!connection.autoCommit)
            connection.commit()
    }

    override fun saveOnlinePlayersData() {
        if (!connection.autoCommit) {
            connection.autoCommit = true
            for (player in Bukkit.getOnlinePlayers()) {
                removeTraitOwnerData(player.uniqueId)
                saveTraitOwnerData(player.uniqueId,TraitManager.instance.traitOwners[player.uniqueId])
            }
            connection.commit()
            connection.autoCommit = false
        }
        else {
            for (player in Bukkit.getOnlinePlayers()) {
                removeTraitOwnerData(player.uniqueId)
                saveTraitOwnerData(player.uniqueId,TraitManager.instance.traitOwners[player.uniqueId])
            }
        }
    }

    private fun getOwnerId(uuid : UUID) : Long {
        var ownerId = -1L
        connection.prepareStatement(
            "select * from $ownerTable where `$OWNER_TABLE_COLUMN_UUID_NAME` = ?")
            .use { statement ->
                statement.setString(1, uuid.toString())
                val result = statement.executeQuery()
                if (result.next()) {
                    ownerId = result.getLong(1)
                }
            }
        return ownerId
    }

    override fun saveTraitOwnerData(uuid : UUID, traitOwner: TraitOwner?) {
        traitOwner ?: return
        if (traitOwner.traits.isEmpty())
            return
        var ownerId = -1L
        connection.prepareStatement(
            "select * from $ownerTable where `$OWNER_TABLE_COLUMN_UUID_NAME` = ?")
            .use { statement ->
                statement.setString(1, uuid.toString())
                val result = statement.executeQuery()
                if (!result.next()) {
                    connection.prepareStatement(
                        "insert into $ownerTable ($OWNER_TABLE_COLUMN_UUID_NAME,$OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME) VALUES (?,?)",
                        Statement.RETURN_GENERATED_KEYS)
                        .use { statement ->
                            statement.setString(1, uuid.toString())
                            statement.setLong(2,traitOwner.lastTraitChangeTimestamp)
                            val affectedRows = statement.executeUpdate()
                            if (affectedRows > 0) {
                                val newResult = statement.generatedKeys
                                newResult.next()
                                ownerId = newResult.getLong(1)
                            }
                        }
                }
                else {
                    ownerId = result.getLong(1)
                }
            }
        if (ownerId != -1L) {
            connection.prepareStatement(
                "insert into $traitsTable ($TRAITS_TABLE_COLUMN_OWNER_ID_NAME,$TRAITS_TABLE_COLUMN_TRAIT_NAME) VALUES (?, ?)")
                .use { statement ->
                    for (traitName in traitOwner.traits) {
                        if (!TraitManager.instance.traits.containsKey(traitName))
                            continue
                        statement.setString(1, ownerId.toString())
                        statement.setString(2, traitName)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
        }
    }

    override fun removeTraitOwnerData(uuid : UUID, purge : Boolean) {
        if (purge) {
            connection.prepareStatement(
                "delete from $ownerTable where `$OWNER_TABLE_COLUMN_UUID_NAME` = ?")
                .use { statement ->
                    statement.setString(1, uuid.toString())
                    statement.executeUpdate()
                }
        }
        else {
            val ownerId = getOwnerId(uuid)
            connection.prepareStatement(
                "delete from $traitsTable where `$TRAITS_TABLE_COLUMN_OWNER_ID_NAME` = ?")
                .use { statement ->
                    statement.setString(1, ownerId.toString())
                    statement.executeUpdate()
                }
        }
    }

    override fun removeTraitOwnerData(uuid : UUID, vararg traitNames : String) {
        val ownerId = getOwnerId(uuid)
        val parsedTraitNames = traitNames.joinToString(", ")
        connection.prepareStatement(
            "delete from $traitsTable where `$TRAITS_TABLE_COLUMN_OWNER_ID_NAME` = ? and `$TRAITS_TABLE_COLUMN_TRAIT_NAME` in (?)")
            .use { statement ->
                statement.setString(1, ownerId.toString())
                statement.setString(2, parsedTraitNames)
                statement.executeUpdate()
            }
    }

    override fun resetTraitOwnerSelectCooldown(uuid : UUID) {

        connection.prepareStatement(
            "update $ownerTable set `$OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME` = ? where `$OWNER_TABLE_COLUMN_UUID_NAME` = ?")
            .use { statement ->
                statement.setLong(1, 0)
                statement.setString(2,uuid.toString())
                statement.executeUpdate()
            }
    }

    override fun loadTraitOwnerData(player: Player) : TraitOwner {
        val traitOwner = TraitOwner(player)
        val ownerId = getOwnerId(player.uniqueId)
        if (ownerId != -1L) {
            connection.prepareStatement(
                "select $OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME from $ownerTable where `$OWNER_TABLE_COLUMN_ID_NAME` = ?")
                .use { statement ->
                    statement.setString(1, ownerId.toString())
                    statement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            traitOwner.lastTraitChangeTimestamp = resultSet.getLong(1)
                        }
                    }
                }
            connection.prepareStatement(
                "select $TRAITS_TABLE_COLUMN_TRAIT_NAME from $traitsTable where `$TRAITS_TABLE_COLUMN_OWNER_ID_NAME` = ?")
                .use { statement ->
                    statement.setString(1, ownerId.toString())
                    statement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            val traitName = resultSet.getString(1)
                            traitOwner.cachedTraits.add(traitName)
                        }
                    }
                }
        }
        return traitOwner
    }

    override fun onPlayerJoin(event : PlayerJoinEvent) {
        val traitOwner = loadTraitOwnerData(event.player)
        TraitManager.instance.traitOwners[event.player.uniqueId] = traitOwner
    }

    override fun onPlayerQuit(event : PlayerQuitEvent) {
        val traitOwner = TraitManager.instance.traitOwners.remove(event.player.uniqueId)
        removeTraitOwnerData(event.player.uniqueId)
        saveTraitOwnerData(event.player.uniqueId,traitOwner)
    }

}