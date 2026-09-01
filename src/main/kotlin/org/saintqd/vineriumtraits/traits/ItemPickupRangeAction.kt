package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("item_pickup_range")
class ItemPickupRangeAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val radius = config.getDouble("PickupRadius",2.5)

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
        const val NAME = "item_pickup_range"

        private val actionsMap = hashMapOf<String, ItemPickupRangeAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onServerTickEnd(event : ServerTickEndEvent) {
                if (event.tickNumber % 5 != 0)
                    return
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.isDead) continue
                    val traitOwner = TraitManager.instance.traitOwners[player.uniqueId] ?: continue
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) continue
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (TraitManager.instance.executeAction(traitName,traitOwner)) {
                                val radius = action.radius
                                player.world.getNearbyEntitiesByType(Item::class.java,
                                    player.location, radius, radius, radius).forEach { item ->
                                        if (!item.canPlayerPickup() || item.pickupDelay > 0)
                                            return@forEach

                                    if (player.inventory.firstEmpty() == -1) {
                                        if (!player.inventory.containsAtLeast(item.itemStack, 1)) {
                                            return@forEach
                                        }
                                        var fail = true
                                        for (it in player.inventory) {
                                            if (it == null
                                                || it.itemMeta == null
                                                || it.type != item.itemStack.type
                                                || it.itemMeta != item.itemStack.itemMeta
                                                || it.amount >= it.maxStackSize
                                            ) continue
                                            fail = false
                                        }
                                        if (fail)
                                            return@forEach
                                    }
                                    if (!EntityPickupItemEvent(player, item, 0).callEvent()) {
                                        return
                                    }
                                    val remainingItems = player.inventory.addItem(item.itemStack)
                                    if (remainingItems.isEmpty()) {
                                        player.playPickupItemAnimation(item)
                                        item.remove()
                                    } else {
                                        for (index in remainingItems.keys) {
                                            remainingItems[index]?.let { itemStack ->
                                                val remaining = item.itemStack.amount - itemStack.amount
                                                player.playPickupItemAnimation(item, remaining)
                                                item.itemStack = itemStack
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}