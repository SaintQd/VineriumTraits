package org.saintqd.vineriumtraits.traits

import com.google.common.base.Enums
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.potion.PotionEffectType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.NoSuchElementException

@VinTraitType("effect_duration_coef")
class EffectDurationCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val coef = config.getDouble("Coef",1.0)
    val effectsWhitelist = hashSetOf<NamespacedKey>()
    val causes = hashSetOf<EntityPotionEffectEvent.Cause>()

    init {
        val possiblePotionEffects = config.getStringList("PotionEffects")
        for (potionEffectKey in possiblePotionEffects) {
            try {
                val effect = Registry.MOB_EFFECT.getOrThrow(Key.key(Key.MINECRAFT_NAMESPACE, potionEffectKey.lowercase()))
                effectsWhitelist.add(effect.key)
            }
            catch (ex : NoSuchElementException) {
                VineriumTraits.inst().logger.warning("Traits: Trait $name: There is no PotionEffectType named ${potionEffectKey.lowercase()}")
            }
        }
        val possibleCauses = config.getStringList("Causes")
        for (causeKey in possibleCauses) {
            val possibleCause = Enums.getIfPresent(EntityPotionEffectEvent.Cause::class.java,causeKey.uppercase())
            if (possibleCause.isPresent) {
                causes.add(possibleCause.get())
            }
            else
                VineriumTraits.inst().logger.warning("Traits: Trait $name: There is no EntityPotionEffectEvent Cause named ${causeKey.uppercase()}")
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
        const val NAME = "effect_duration_coef"

        private val actionsMap = hashMapOf<String, EffectDurationCoefAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onPotionEffect(event: EntityPotionEffectEvent) {
                if (event.cause == EntityPotionEffectEvent.Cause.POTION_DRINK && event.entity is Player) {
                    val player = event.entity as Player
                    event.newEffect?.let { newEffect ->
                        val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                        val commonTraits = traitOwner.traits intersect actionsMap.keys
                        if (commonTraits.isEmpty()) return
                        for (traitName in commonTraits) {
                            actionsMap[traitName]?.let { action ->
                                if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                    if (action.effectsWhitelist.isNotEmpty() && !action.effectsWhitelist.contains(newEffect.type.key)) {
                                        continue
                                    }
                                    if (action.causes.isNotEmpty() && !action.causes.contains(event.cause)) {
                                        continue
                                    }
                                    val potionEffect =
                                        newEffect.withDuration((newEffect.duration * action.coef).toInt())
                                    event.isCancelled = true
                                    player.addPotionEffect(potionEffect)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}