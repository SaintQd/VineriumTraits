package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Piglin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PiglinBarterEvent
import org.bukkit.scheduler.BukkitTask
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.UUID

@VinTraitType("cancel_barter_event")
class CancelBarterEvent(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    override fun register() {
        if (actionsMap.isEmpty()) {
            Bukkit.getServer().pluginManager.registerEvents(listener, VineriumTraits.inst())
            piglinCheckTask?.cancel()
            piglinCheckTask = Bukkit.getScheduler().runTaskTimer(VineriumTraits.inst(), Runnable {
                piglinTraders.keys.removeIf { piglinUuid ->
                    val entity = Bukkit.getEntity(piglinUuid) ?: return@removeIf true
                    if (!entity.isValid) return@removeIf true
                    return@removeIf false
                }
            },12000L, 12000L)
        }
        actionsMap[traitName] = this
    }

    override fun unregister() {
        actionsMap.remove(traitName)
        if (actionsMap.isEmpty()) {
            HandlerList.unregisterAll(listener)
            piglinCheckTask?.cancel()
        }
    }

    companion object {
        private var piglinCheckTask : BukkitTask? = null
        private val piglinTraders = hashMapOf<UUID, UUID>()

        private val actionsMap = hashMapOf<String, CancelBarterEvent>()

        private val listener = object : Listener {

            @EventHandler
            fun onPiglinPickup(event: EntityPickupItemEvent) {
                val entity = event.entity
                if (entity is Piglin) {
                    val item = event.item
                    item.thrower?.let { throwerUuid ->
                        Bukkit.getPlayer(throwerUuid)?.let { player ->
                            piglinTraders[entity.uniqueId] = player.uniqueId
                        }
                    }
                }
            }

            @EventHandler(priority = EventPriority.LOW)
            fun onPiglinBarter(event: PiglinBarterEvent) {
                val playerUuid = piglinTraders[event.entity.uniqueId] ?: return
                val player = Bukkit.getPlayer(playerUuid) ?: return
                val traitOwner = TraitManager.instance.traitOwners[player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isNotEmpty())
                    event.isCancelled = true
            }
        }
    }
}