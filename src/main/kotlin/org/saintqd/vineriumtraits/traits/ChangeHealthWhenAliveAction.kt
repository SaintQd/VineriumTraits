package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.events.TraitOwnerJoinEvent
import org.saintqd.vineriumtraits.events.TraitOwnerQuitEvent
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("change_health_when_alive")
class ChangeHealthWhenAliveAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val maxBoost = config.getDouble("MaxBoost",40.0)
    val increaseAmount = config.getDouble("IncreaseAmount",2.0)
    val increasePeriod = config.getLong("IncreasePeriod",24000L)

    override fun register() {
        if (actionsMap.isEmpty()) {
            Bukkit.getServer().pluginManager.registerEvents(listener, VineriumTraits.inst())
            healthCheckTask?.cancel()
            healthCheckTask = Bukkit.getScheduler().runTaskTimer(VineriumTraits.inst(), Runnable {
                for (player in Bukkit.getOnlinePlayers()) {
                    val traitOwner = TraitManager.instance.traitOwners[player.uniqueId] ?: continue
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) continue
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner))
                                action.setAbsorption(player)
                        }
                    }
                }
            },increasePeriod, increasePeriod)
        }
        actionsMap[traitName] = this
    }

    override fun unregister() {
        actionsMap.remove(traitName)
        if (actionsMap.isEmpty()) {
            HandlerList.unregisterAll(listener)
            healthCheckTask?.cancel()
        }
    }

    override fun onAdd(owner: TraitOwner) {
        owner.player.persistentDataContainer.set(TIME_SINCE_DEATH_KEY, PersistentDataType.LONG, 0L)
    }

    override fun onRemove(owner: TraitOwner) {
        owner.player.persistentDataContainer.remove(TIME_SINCE_DEATH_KEY)
        owner.player.persistentDataContainer.remove(SAVED_HEALTH_KEY)
        owner.player.getAttribute(Attribute.MAX_HEALTH)?.removeModifier(ACTION_KEY)
    }

    private fun setAbsorption(player : Player) {
        var increaseTime = true
        if (VineriumTraits.inst().cmiEnabled) {
            com.Zrips.CMI.CMI.getInstance().playerManager.getUser(player.uniqueId)?.let { user ->
                if (user.isAfk) {
                    increaseTime = false
                }
            }
        }
        var timeSinceDeath = player.persistentDataContainer.getOrDefault(TIME_SINCE_DEATH_KEY,
            PersistentDataType.LONG, 0L)
        if (increaseTime) {
            timeSinceDeath += increasePeriod
        }
        player.persistentDataContainer.set(TIME_SINCE_DEATH_KEY, PersistentDataType.LONG, timeSinceDeath)
        val diffAmount = timeSinceDeath / increasePeriod
        if (diffAmount == 0L) {
            player.getAttribute(Attribute.MAX_HEALTH)?.removeModifier(ACTION_KEY)
        }
        else {
            val newAmount = (increaseAmount * diffAmount).coerceAtMost(maxBoost)
            Bukkit.getScheduler().runTaskLater(VineriumTraits.inst(), Runnable {
                if (!player.isValid || player.isDead)
                    return@Runnable
                player.getAttribute(Attribute.MAX_HEALTH)?.let { instance ->
                    instance.removeModifier(ACTION_KEY)
                    instance.addModifier(AttributeModifier(ACTION_KEY,newAmount, AttributeModifier.Operation.ADD_NUMBER))
                    player.health = instance.value.coerceAtMost(player.health)
                }
            }, 2L)
        }
    }

    companion object {

        private var healthCheckTask : BukkitTask? = null

        val ACTION_KEY = NamespacedKey(VineriumTraits.inst(),"change_health_when_alive")
        val TIME_SINCE_DEATH_KEY = NamespacedKey(VineriumTraits.inst(),"change_health_when_alive_last_death")
        val SAVED_HEALTH_KEY = NamespacedKey(VineriumTraits.inst(),"change_health_when_alive_saved_health")

        private val actionsMap = hashMapOf<String, ChangeHealthWhenAliveAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onEntityDeath(event: EntityDeathEvent) {
                event.entity.getAttribute(Attribute.MAX_HEALTH)?.let { _ ->
                    val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { _ ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                traitOwner.player.persistentDataContainer.set(TIME_SINCE_DEATH_KEY,
                                    PersistentDataType.LONG,  0L)
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
                    actionsMap[traitName]?.let { _ ->
                        if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                            traitOwner.player.getAttribute(Attribute.MAX_HEALTH)?.removeModifier(ACTION_KEY)
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
                    actionsMap[traitName]?.let { _ ->
                        if (TraitManager.instance.executeAction(traitName, event.traitOwner)) {
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