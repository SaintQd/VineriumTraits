package org.saintqd.vineriumtraits.storage

import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.jdbi.v3.core.Jdbi
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.enums.InteractionType
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner
import org.saintqd.vineriumtraits.utils.JDBIUtils
import java.sql.SQLException
import java.util.UUID

class MySqlJdbiStorage : DataStorage {

    val dataSource = HikariDataSource()
    val jdbi : Jdbi
    val ownerTable : String
    val traitsTable : String
    val traitBindsTable : String

    companion object {
        const val OWNER_TABLE_COLUMN_ID_NAME = "id"
        const val OWNER_TABLE_COLUMN_UUID_NAME = "uuid"
        const val OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME = "select_cooldown"

        const val TRAITS_TABLE_COLUMN_ID_NAME = "id"
        const val TRAITS_TABLE_COLUMN_OWNER_ID_NAME = "owner_id"
        const val TRAITS_TABLE_COLUMN_TRAIT_NAME = "trait_name"

        const val TRAIT_BINDS_TABLE_COLUMN_ID_NAME = "id"
        const val TRAIT_BINDS_TABLE_COLUMN_OWNER_ID_NAME = "owner_id"
        const val TRAIT_BINDS_TABLE_COLUMN_TRAIT_NAME = "trait_name"
        const val TRAIT_BINDS_TABLE_COLUMN_INTERACTION_TYPE_NAME = "interaction_type"
        const val TRAIT_BINDS_TABLE_COLUMN_REQUIRE_SNEAKING_NAME = "require_sneaking"
        const val TRAIT_BINDS_TABLE_COLUMN_REQUIRE_EMPTY_HAND_NAME = "require_empty_hand"
    }

    fun setupDataSource() {
        dataSource.jdbcUrl = VineriumTraits.inst().config.getString("Database.Url","")
        dataSource.username = VineriumTraits.inst().config.getString("Database.Username","")
        dataSource.password = VineriumTraits.inst().config.getString("Database.Password","")
    }

    init {
        setupDataSource()
        jdbi = Jdbi.create(dataSource)

        var tablePrefix = VineriumTraits.inst().config.getString("Database.TablePrefix","")!!
        tablePrefix = if (tablePrefix.isBlank()) "" else tablePrefix + "_"
        ownerTable = tablePrefix + "trait_owners"
        traitsTable = tablePrefix + "traits"
        traitBindsTable = tablePrefix + "trait_binds"

        if (JDBIUtils.checkIfTableExists(jdbi,ownerTable)) {
            val expected = hashMapOf(
                Pair(OWNER_TABLE_COLUMN_UUID_NAME,"varchar(36)"),
                Pair(OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME,"bigint unsigned")
            )
            if (!JDBIUtils.checkIfTableMatchesStructure(jdbi,ownerTable,expected)) {
                throw SQLException("JDBC table $ownerTable does not match expected structure")
            }
        }
        else {
            jdbi.useHandle<SQLException> { handle ->
                handle.execute("create table $ownerTable\n" +
                        "(\n" +
                        "    $OWNER_TABLE_COLUMN_ID_NAME    int unsigned auto_increment primary key,\n" +
                        "    $OWNER_TABLE_COLUMN_UUID_NAME    varchar(36) not null,\n" +
                        "    $OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME    bigint unsigned not null default 0,\n" +
                        "    constraint owner_traits_uuid_uindex unique ($OWNER_TABLE_COLUMN_UUID_NAME)\n" +
                        ");")
            }
        }

        if (JDBIUtils.checkIfTableExists(jdbi,traitsTable)) {
            val expected = hashMapOf(
                Pair(TRAITS_TABLE_COLUMN_OWNER_ID_NAME,"int unsigned"),
                Pair(TRAITS_TABLE_COLUMN_TRAIT_NAME,"varchar(64)")
            )
            if (!JDBIUtils.checkIfTableMatchesStructure(jdbi,traitsTable,expected)) {
                throw SQLException("JDBC table $traitsTable does not match expected structure")
            }
        }
        else {
            jdbi.useHandle<SQLException> { handle ->
                handle.execute(
                    "create table $traitsTable\n" +
                            "(\n" +
                            "    $TRAITS_TABLE_COLUMN_ID_NAME    bigint unsigned auto_increment primary key,\n" +
                            "    $TRAITS_TABLE_COLUMN_OWNER_ID_NAME    int unsigned,\n" +
                            "    $TRAITS_TABLE_COLUMN_TRAIT_NAME    varchar(64) not null,\n" +
                            "    foreign key    ($TRAITS_TABLE_COLUMN_OWNER_ID_NAME) references $ownerTable($OWNER_TABLE_COLUMN_ID_NAME) on delete cascade on update cascade\n" +
                            ");"
                )
            }
        }

        if (JDBIUtils.checkIfTableExists(jdbi,traitBindsTable)) {
            val expected = hashMapOf(
                Pair(TRAIT_BINDS_TABLE_COLUMN_OWNER_ID_NAME,"int unsigned"),
                Pair(TRAIT_BINDS_TABLE_COLUMN_TRAIT_NAME,"varchar(64)"),
                Pair(TRAIT_BINDS_TABLE_COLUMN_INTERACTION_TYPE_NAME,"varchar(32)"),
                Pair(TRAIT_BINDS_TABLE_COLUMN_REQUIRE_SNEAKING_NAME,"tinyint(1)"),
                Pair(TRAIT_BINDS_TABLE_COLUMN_REQUIRE_EMPTY_HAND_NAME,"tinyint(1)")
            )
            if (!JDBIUtils.checkIfTableMatchesStructure(jdbi,traitBindsTable,expected)) {
                throw SQLException("JDBC table $traitBindsTable does not match expected structure")
            }
        }
        else {
            jdbi.useHandle<SQLException> { handle ->
                handle.execute(
                    "create table $traitBindsTable\n" +
                            "(\n" +
                            "    $TRAIT_BINDS_TABLE_COLUMN_ID_NAME    bigint unsigned auto_increment primary key,\n" +
                            "    $TRAIT_BINDS_TABLE_COLUMN_OWNER_ID_NAME    int unsigned,\n" +
                            "    $TRAIT_BINDS_TABLE_COLUMN_TRAIT_NAME    varchar(64) not null,\n" +
                            "    $TRAIT_BINDS_TABLE_COLUMN_INTERACTION_TYPE_NAME    varchar(32) not null,\n" +
                            "    $TRAIT_BINDS_TABLE_COLUMN_REQUIRE_SNEAKING_NAME    tinyint(1) not null,\n" +
                            "    $TRAIT_BINDS_TABLE_COLUMN_REQUIRE_EMPTY_HAND_NAME    tinyint(1) not null,\n" +
                            "    foreign key    ($TRAIT_BINDS_TABLE_COLUMN_OWNER_ID_NAME) references $ownerTable($OWNER_TABLE_COLUMN_ID_NAME) on delete cascade on update cascade\n" +
                            ");"
                )
            }
        }
    }

    override fun save() {
    }

    override fun saveOnlinePlayersData() {
        for (player in Bukkit.getOnlinePlayers()) {
            removeTraitOwnerData(player.uniqueId)
            saveTraitOwnerData(player.uniqueId,TraitManager.instance.traitOwners[player.uniqueId])
        }
    }

    private fun getOwnerId(uuid : UUID) : Long {
        return jdbi.withHandle<Long, SQLException> { handle ->
            handle.createQuery("select * from $ownerTable where `$OWNER_TABLE_COLUMN_UUID_NAME` = :uuid")
                .bind("uuid",uuid.toString())
                .mapTo(Long::class.java)
                .findFirst()
                .orElse(-1L)
        }
    }

    override fun saveTraitOwnerData(uuid : UUID, traitOwner: TraitOwner?) {
        traitOwner ?: return
        if (traitOwner.traits.isEmpty())
            return
        var ownerId = getOwnerId(uuid)
        if (ownerId == -1L) {
            ownerId = jdbi.withHandle<Long, SQLException> { handle ->
                handle.createUpdate("insert into $ownerTable ($OWNER_TABLE_COLUMN_UUID_NAME,$OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME) VALUES (:uuid,:cooldown)")
                    .bind("uuid",uuid.toString())
                    .bind("cooldown",traitOwner.lastTraitChangeTimestamp.toString())
                    .executeAndReturnGeneratedKeys()
                    .mapTo(Long::class.java)
                    .one()
            }
        }
        if (ownerId != -1L) {
            if (traitOwner.traits.isNotEmpty()) {
                jdbi.useHandle<SQLException> { handle ->
                    val batch =
                        handle.prepareBatch("insert into $traitsTable ($TRAITS_TABLE_COLUMN_OWNER_ID_NAME,$TRAITS_TABLE_COLUMN_TRAIT_NAME) VALUES (:id, :trait_name)")
                    for (traitName in traitOwner.traits) {
                        if (!TraitManager.instance.traits.containsKey(traitName))
                            continue
                        batch.bind("id", ownerId.toString())
                            .bind("trait_name", traitName)
                            .add()
                    }
                    batch.execute()
                }
            }
            if (traitOwner.bindedTraits.isNotEmpty()) {
                jdbi.useHandle<SQLException> { handle ->
                    val batch = handle.prepareBatch("insert into $traitBindsTable ($TRAIT_BINDS_TABLE_COLUMN_OWNER_ID_NAME," +
                            "$TRAIT_BINDS_TABLE_COLUMN_TRAIT_NAME,$TRAIT_BINDS_TABLE_COLUMN_INTERACTION_TYPE_NAME,$TRAIT_BINDS_TABLE_COLUMN_REQUIRE_SNEAKING_NAME," +
                            "$TRAIT_BINDS_TABLE_COLUMN_REQUIRE_EMPTY_HAND_NAME) VALUES (:id, :trait_name, :interaction_type, :require_sneaking, :require_empty_hand)")
                    for (bindedTraitData in traitOwner.bindedTraits) {
                        if (!TraitManager.instance.traits.containsKey(bindedTraitData.key))
                            continue
                        if (!traitOwner.traits.contains(bindedTraitData.key))
                            continue
                        batch.bind("id",ownerId.toString())
                            .bind("trait_name",bindedTraitData.key)
                            .bind("interaction_type",bindedTraitData.value.interactionType.name)
                            .bind("require_sneaking",bindedTraitData.value.requireSneaking)
                            .bind("require_empty_hand",bindedTraitData.value.requireEmptyHand)
                            .add()
                    }
                    batch.execute()
                }
            }
        }
    }

    override fun removeTraitOwnerData(uuid : UUID, purge : Boolean) {
        if (purge) {
            jdbi.useHandle<SQLException> { handle ->
                handle.createUpdate("delete from $ownerTable where `$OWNER_TABLE_COLUMN_UUID_NAME` = :uuid")
                    .bind("uuid",uuid.toString())
                    .execute()
            }
        }
        else {
            val ownerId = getOwnerId(uuid)
            jdbi.useHandle<SQLException> { handle ->
                handle.createUpdate("delete from $traitsTable where `$TRAITS_TABLE_COLUMN_OWNER_ID_NAME` = :id")
                    .bind("id",ownerId.toString())
                    .execute()
            }
            jdbi.useHandle<SQLException> { handle ->
                handle.createUpdate("delete from $traitBindsTable where `$TRAITS_TABLE_COLUMN_OWNER_ID_NAME` = :id")
                    .bind("id",ownerId.toString())
                    .execute()
            }
        }
    }

    override fun removeTraitOwnerData(uuid : UUID, vararg traitNames : String) {
        val ownerId = getOwnerId(uuid)
        val parsedTraitNames = traitNames.joinToString(", ")
        jdbi.useHandle<SQLException> { handle ->
            handle.createUpdate("delete from $traitsTable where `$TRAITS_TABLE_COLUMN_OWNER_ID_NAME` = :id and `$TRAITS_TABLE_COLUMN_TRAIT_NAME` in (:trait_name)")
                .bind("id",ownerId.toString())
                .bind("trait_name",parsedTraitNames)
                .execute()
        }
        jdbi.useHandle<SQLException> { handle ->
            handle.createUpdate("delete from $traitBindsTable where `$TRAIT_BINDS_TABLE_COLUMN_OWNER_ID_NAME` = :id and `$TRAIT_BINDS_TABLE_COLUMN_TRAIT_NAME` in (:trait_name)")
                .bind("id",ownerId.toString())
                .bind("trait_name",parsedTraitNames)
                .execute()
        }
    }

    override fun resetTraitOwnerSelectCooldown(uuid : UUID) {

        jdbi.useHandle<SQLException> { handle ->
            handle.createUpdate("update $ownerTable set `$OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME` = :cooldown where `$OWNER_TABLE_COLUMN_UUID_NAME` = :uuid")
                .bind("cooldown",0)
                .bind("uuid",uuid.toString())
                .execute()
        }
    }

    override fun loadTraitOwnerData(player: Player) : TraitOwner {
        val traitOwner = TraitOwner(player)
        val ownerId = getOwnerId(player.uniqueId)
        if (ownerId != -1L) {
            traitOwner.lastTraitChangeTimestamp = jdbi.withHandle<Long,SQLException> { handle ->
                handle.createQuery("select $OWNER_TABLE_COLUMN_SELECT_COOLDOWN_NAME from $ownerTable where `$OWNER_TABLE_COLUMN_ID_NAME` = :id")
                    .bind("id",ownerId.toString())
                    .mapTo(Long::class.java)
                    .findFirst()
                    .orElse(0)
            }
            traitOwner.cachedTraits.addAll(jdbi.withHandle<List<String>, SQLException> { handle ->
                handle.createQuery("select $TRAITS_TABLE_COLUMN_TRAIT_NAME from $traitsTable where `$TRAITS_TABLE_COLUMN_OWNER_ID_NAME` = :id")
                    .bind("id",ownerId.toString())
                    .mapTo(String::class.java)
                    .list()
            })
            val rowsList = jdbi.withHandle<List<Map<String, Any>>, SQLException> { handle ->
                handle.createQuery("select $TRAIT_BINDS_TABLE_COLUMN_TRAIT_NAME,$TRAIT_BINDS_TABLE_COLUMN_INTERACTION_TYPE_NAME," +
                        "$TRAIT_BINDS_TABLE_COLUMN_REQUIRE_SNEAKING_NAME,$TRAIT_BINDS_TABLE_COLUMN_REQUIRE_EMPTY_HAND_NAME" +
                        " from $traitBindsTable where `$TRAIT_BINDS_TABLE_COLUMN_OWNER_ID_NAME` = :id")
                    .bind("id",ownerId.toString())
                    .mapToMap()
                    .list()
            }
            for (row in rowsList) {
                val traitName = row[TRAIT_BINDS_TABLE_COLUMN_TRAIT_NAME].toString()

                val interactionType = InteractionType.valueOf(row[TRAIT_BINDS_TABLE_COLUMN_INTERACTION_TYPE_NAME].toString())
                val requireSneaking = row[TRAIT_BINDS_TABLE_COLUMN_REQUIRE_SNEAKING_NAME].toString().toBoolean()
                val requireEmptyHand = row[TRAIT_BINDS_TABLE_COLUMN_REQUIRE_EMPTY_HAND_NAME].toString().toBoolean()

                val traitBindData = TraitOwner.TraitBindData(interactionType,requireSneaking,requireEmptyHand)
                traitOwner.bindedTraits[traitName] = traitBindData
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
        removeTraitOwnerData(event.player.uniqueId,true)
        saveTraitOwnerData(event.player.uniqueId,traitOwner)
    }

}