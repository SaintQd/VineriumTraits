package org.saintqd.vineriumtraits.storage

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.saintqd.vineriumtraits.managers.TraitOwner
import java.util.UUID

interface DataStorage {

    fun onPlayerJoin(event : PlayerJoinEvent)

    fun onPlayerQuit(event : PlayerQuitEvent)

    fun save()

    fun saveOnlinePlayersData()

    fun saveTraitOwnerData(uuid : UUID, traitOwner: TraitOwner?)

    fun removeTraitOwnerData(uuid : UUID, purge : Boolean = false)

    fun removeTraitOwnerData(uuid : UUID, vararg traitNames : String)

    fun resetTraitOwnerSelectCooldown(uuid: UUID)

    fun loadTraitOwnerData(player: Player) : TraitOwner
}