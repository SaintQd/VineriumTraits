package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("forced_resurrect")
class ForcedResurrectAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            traitOwner.player.health = 2.0
            traitOwner.player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 900, 1, false, true, true))
            traitOwner.player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0, false, true, true))
            traitOwner.player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 100, 1, false, true, true))
            traitOwner.player.world.playSound(traitOwner.player.location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1f, 1f)

            return@Function true
        }
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
        const val NAME = "forced_resurrect"

        private val actionsMap = hashMapOf<String, ForcedResurrectAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onPlayerResurrect(event: EntityResurrectEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                            event.isCancelled = false
                        }
                    }
                }
            }
        }
    }
}