package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.entity.EvokerFangs
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("summon_evoker_fangs")
class SummonEvokerFangsAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val amount = config.getInt("Amount",16)

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {

            val currentLoc = traitOwner.player.location
            for (index in 0..<amount) {
                currentLoc.add(currentLoc.direction.setY(0))
                if (!currentLoc.block.getRelative(BlockFace.DOWN).isSolid) continue
                val fangs = currentLoc.world.spawnEntity(currentLoc, EntityType.EVOKER_FANGS) as EvokerFangs
                fangs.persistentDataContainer.set(SENT_FROM_PLAYER_KEY, PersistentDataType.STRING,traitOwner.player.uniqueId.toString())
                fangs.owner = traitOwner.player
            }
            return@Function true
        }
        else
            return@Function false
    }

    override fun register() {
        if (actionsMap.isEmpty())
            Bukkit.getServer().pluginManager.registerEvents(listener, VineriumTraits.inst())
        actionsMap[traitName] = this
    }

    override fun unregister() {
        actionsMap.remove(traitName)
        if (actionsMap.isEmpty())
            HandlerList.unregisterAll(listener)
    }

    companion object {
        const val NAME = "summon_evoker_fangs"

        val SENT_FROM_PLAYER_KEY = NamespacedKey(VineriumTraits.inst(),"sent_from_player")

        private val actionsMap = hashMapOf<String, SummonEvokerFangsAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
                event.damager.persistentDataContainer.get(SENT_FROM_PLAYER_KEY, PersistentDataType.STRING)?.let { id ->
                    if (id == event.entity.uniqueId.toString()) {
                        event.isCancelled = true
                    }
                }
            }
        }
    }
}