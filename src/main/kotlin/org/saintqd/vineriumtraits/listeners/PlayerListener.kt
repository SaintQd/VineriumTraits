package org.saintqd.vineriumtraits.listeners

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.events.TraitOwnerJoinEvent
import org.saintqd.vineriumtraits.events.TraitOwnerQuitEvent
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.concurrent.CompletableFuture

class PlayerListener : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerJoin(event : PlayerJoinEvent) {
        val future = CompletableFuture.runAsync {
            VineriumTraits.inst().storage?.onPlayerJoin(event)
        }.thenAccept {
            val runnable : Runnable = {
                TraitManager.instance.traitOwners[event.player.uniqueId]?.let { traitOwner ->
                    for (traitName in traitOwner.cachedTraits)
                        TraitManager.instance.traits[traitName]?.let { trait ->
                            traitOwner.addTrait(trait,true)
                        }
                    val event = TraitOwnerJoinEvent(traitOwner)
                    Bukkit.getPluginManager().callEvent(event)
                }
            }
            Bukkit.getScheduler().runTask(VineriumTraits.inst(),runnable)
        }
        future.join()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerQuit(event : PlayerQuitEvent) {
        val future = CompletableFuture.runAsync {
            VineriumTraits.inst().storage?.onPlayerQuit(event)
        }.thenAccept {
            val runnable : Runnable = {
                TraitManager.instance.traitOwners[event.player.uniqueId]?.let { traitOwner ->
                    val event = TraitOwnerQuitEvent(traitOwner)
                    Bukkit.getPluginManager().callEvent(event)
                }
            }
            Bukkit.getScheduler().runTask(VineriumTraits.inst(),runnable)
        }
        future.join()
    }

    @EventHandler
    fun onPlayerRespawn(event : PlayerPostRespawnEvent) {
        TraitManager.instance.traitOwners[event.player.uniqueId]?.let { traitOwner ->
            for (traitName in traitOwner.traits) {
                val trait = TraitManager.instance.traits[traitName] ?: continue
                if (trait.mmSkillOnRespawn.isEmpty()) continue
                val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
                MMAbilityData.executeMMSkill(trait.mmSkillOnRespawn,skillMetadata)
            }
        }
    }
}