package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.entity.EntityPathfindEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Fox
import org.bukkit.entity.Piglin
import org.bukkit.entity.Player
import org.bukkit.entity.memory.MemoryKey
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PiglinBarterEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootContext
import org.bukkit.loot.LootTables
import org.bukkit.scheduler.BukkitTask
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.UUID

@VinTraitType("better_bartering")
class BetterBarteringAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val minAmount = config.getInt("MinAmount",1)
    val maxAmount = config.getInt("MaxAmount",3)

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
        const val NAME = "better_bartering"
        private var piglinCheckTask : BukkitTask? = null
        private val piglinTraders = hashMapOf<UUID, UUID>()
        private val actionsMap = hashMapOf<String, BetterBarteringAction>()

        val random = java.util.Random()

        private fun getBarterResponseItems(piglin : Piglin) : Collection<ItemStack> {
            return LootTables.PIGLIN_BARTERING.lootTable.populateLoot(random, LootContext.Builder(piglin.location).lootedEntity(piglin).build())
        }

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

            @EventHandler
            fun onPiglinBarter(event: PiglinBarterEvent) {
                if (event.isCancelled) return
                val loc = event.entity.location
                val playerUuid = piglinTraders[event.entity.uniqueId] ?: return
                val player = Bukkit.getPlayer(playerUuid) ?: return
                val traitOwner = TraitManager.instance.traitOwners[player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                            val num = random.nextInt(action.minAmount,action.maxAmount + 1)
                            for (index in 1..<num) {
                                val items = getBarterResponseItems(event.entity)
                                val startLoc = event.entity.location.clone().add(0.0,1.0,0.0)
                                startLoc.yaw = 0f
                                startLoc.pitch = 0f
                                val vector = player.location.toVector().subtract(startLoc.toVector()).normalize().multiply(0.5)
                                for (item in items) {
                                    val droppedItem = loc.world.dropItem(startLoc, item)
                                    droppedItem.velocity = vector
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}