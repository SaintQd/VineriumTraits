package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.persistence.PersistentDataType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.events.TraitOwnerJoinEvent
import org.saintqd.vineriumtraits.events.TraitOwnerQuitEvent
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("kill_boost")
class KillBoostAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val maxBoost = config.getDouble("MaxBoost",60.0)
    val divideCoef = config.getDouble("DivideCoef",4.0)

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

    override fun onRemove(owner: TraitOwner) {
        owner.player.getAttribute(Attribute.MAX_HEALTH)?.removeModifier(ACTION_KEY)
    }

    private fun setAbsorption(player : Player, amount : Double) {
        val amount = amount.coerceAtMost(maxBoost)
        player.persistentDataContainer.set(ACTION_KEY, PersistentDataType.DOUBLE,amount)
        Bukkit.getScheduler().scheduleSyncDelayedTask(VineriumTraits.inst(), {
            player.getAttribute(Attribute.MAX_HEALTH)?.let { instance ->
                if (player.isDead)
                    return@scheduleSyncDelayedTask
                instance.removeModifier(ACTION_KEY)
                instance.addModifier(AttributeModifier(ACTION_KEY,amount, AttributeModifier.Operation.ADD_NUMBER))
                player.health = instance.value.coerceAtMost(player.health)
            }
        }, 2L)
    }

    companion object {
        const val NAME = "kill_boost"

        val ACTION_KEY = NamespacedKey(VineriumTraits.inst(),"kill_boost")
        val SAVED_HEALTH_KEY = NamespacedKey(VineriumTraits.inst(),"kill_boost_saved_health")

        private val actionsMap = hashMapOf<String, KillBoostAction>()

        private fun getAbsorption(player : Player) : Double {
            return player.persistentDataContainer.getOrDefault(ACTION_KEY, PersistentDataType.DOUBLE,0.0)
        }

        private val listener = object : Listener {

            @EventHandler
            fun onEntityDeath(event: EntityDeathEvent) {
                event.entity.killer?.let { killer ->
                    event.entity.getAttribute(Attribute.MAX_HEALTH)?.let { instance ->
                        val traitOwner = TraitManager.instance.traitOwners[killer.uniqueId] ?: return
                        val commonTraits = traitOwner.traits intersect actionsMap.keys
                        if (commonTraits.isEmpty()) return
                        for (traitName in commonTraits) {
                            actionsMap[traitName]?.let { action ->
                                if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                    action.setAbsorption(killer,getAbsorption(killer) + instance.baseValue / action.divideCoef)
                                }
                            }
                        }
                    }
                }
            }

            @EventHandler(priority = EventPriority.HIGHEST)
            fun onEntityDamage(event: EntityDamageEvent) {
                if (event.isCancelled) return
                if (event.entity is Player) {
                    val player = event.entity as Player
                    val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                action.setAbsorption(player,getAbsorption(player) - event.finalDamage)
                            }
                        }
                    }
                }
            }

            @EventHandler
            fun onPlayerPostRespawn(event : PlayerPostRespawnEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                            action.setAbsorption(traitOwner.player,0.0)
                        }
                    }
                }
            }

            @EventHandler
            fun onTraitOwnerQuit(event : TraitOwnerQuitEvent) {
                val commonTraits = event.traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                event.traitOwner.player.persistentDataContainer.set(SAVED_HEALTH_KEY,
                    PersistentDataType.DOUBLE,event.traitOwner.player.health)
            }

            @EventHandler
            fun onTraitOwnerJoin(event : TraitOwnerJoinEvent) {
                val commonTraits = event.traitOwner.traits intersect actionsMap.keys
                val player = event.traitOwner.player
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName, event.traitOwner)) {
                            action.setAbsorption(player,getAbsorption(player))
                            if (player.persistentDataContainer.has(SAVED_HEALTH_KEY)) {
                                val health = player.persistentDataContainer.get(SAVED_HEALTH_KEY,PersistentDataType.DOUBLE)!!
                                player.getAttribute(Attribute.MAX_HEALTH)?.let {instance ->
                                    player.health = instance.value.coerceAtMost(health)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}