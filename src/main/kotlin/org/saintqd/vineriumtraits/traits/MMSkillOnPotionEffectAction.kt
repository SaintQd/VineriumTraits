package org.saintqd.vineriumtraits.traits

import com.google.common.base.Enums
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner
import java.util.NoSuchElementException

@VinTraitType("mm_skill_on_potion_effect")
class MMSkillOnPotionEffectAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private var skillName = config.getString("SkillName","vintrait_$name")!!
    private val shouldCancelEvent = config.getBoolean("CancelEvent",false)
    private val potionEffects = hashSetOf<NamespacedKey>()
    private val causes = hashSetOf<EntityPotionEffectEvent.Cause>()

    init {
        val possiblePotionEffects = config.getStringList("PotionEffects")
        for (potionEffectKey in possiblePotionEffects) {
            try {
                val effect = Registry.MOB_EFFECT.getOrThrow(Key.key(Key.MINECRAFT_NAMESPACE, potionEffectKey.lowercase()))
                potionEffects.add(effect.key)
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

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner) && skillName.isNotEmpty()) {
            val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
            return@Function MMAbilityData.executeMMSkill(skillName,skillMetadata)
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
        private val actionsMap = hashMapOf<String, MMSkillOnPotionEffectAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onPotionEffect(event: EntityPotionEffectEvent) {
                event.newEffect?.let { newEffect ->
                    val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (action.potionEffects.isNotEmpty() && newEffect.type.key !in action.potionEffects)
                                continue
                            if (action.causes.isNotEmpty() && event.cause !in action.causes)
                                continue
                            if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                if (action.shouldCancelEvent)
                                    event.isCancelled = true
                            }
                        }
                    }
                }
            }
        }
    }
}