package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.potion.PotionEffectType
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("cancel_potion_effect_event")
class CancelPotionEffectEventAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var effectList = hashSetOf<NamespacedKey>()

    init {
        val effectNamesList = config.getStringList("Types")
        if ("ANY" in effectNamesList) {
            RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).forEach { effect ->
                effectList.add(effect.key)
            }
        }
        else {
            config.getStringList("Types").forEach { effectTypeName ->
                val effect = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT)
                    .getOrThrow(Key.key(effectTypeName.lowercase()))
                effectList.add(effect.key)
            }
        }
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
        const val NAME = "cancel_potion_effect_event"

        private val actionsMap = hashMapOf<String, CancelPotionEffectEventAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onPotionEffect(event: EntityPotionEffectEvent) {
                event.newEffect?.let { newEffect ->
                    val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (newEffect.type.key in action.effectList && TraitManager.instance.executeAction(traitName, traitOwner)) {
                                event.isCancelled = true
                            }
                        }
                    }
                }
            }
        }
    }
}