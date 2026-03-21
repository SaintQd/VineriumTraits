package org.saintqd.vineriumtraits.listeners

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.events.TraitOwnerJoinEvent
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.concurrent.CompletableFuture

class PlayerListener : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event : PlayerJoinEvent) {
        val future = CompletableFuture.runAsync {
            VineriumTraits.inst().storage?.onPlayerJoin(event)
        }.thenAccept {
            val runnable : Runnable = {
                TraitManager.instance.traitOwners[event.player.uniqueId]?.let { traitOwner ->
                    val event = TraitOwnerJoinEvent(traitOwner)
                    Bukkit.getPluginManager().callEvent(event)
                }
            }
            Bukkit.getScheduler().runTask(VineriumTraits.inst(),runnable)
        }
        future.join()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event : PlayerQuitEvent) {
        val future = CompletableFuture.runAsync {
            VineriumTraits.inst().storage?.onPlayerQuit(event)
        }.thenAccept {
            val runnable : Runnable = {
                TraitManager.instance.traitOwners[event.player.uniqueId]?.let { traitOwner ->
                    val event = TraitOwnerJoinEvent(traitOwner)
                    Bukkit.getPluginManager().callEvent(event)
                }
            }
            Bukkit.getScheduler().runTask(VineriumTraits.inst(),runnable)
        }
        future.join()
    }
}