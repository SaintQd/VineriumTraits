package org.saintqd.vineriumtraits.traits

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner
import java.util.*

@VinTraitType("mm_skill_on_item_consume")
class MMSkillOnItemConsumeAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val items = config.getStringList("Items").map { foodName -> Material.valueOf(foodName.uppercase()) }
    private var skillName = config.getString("SkillName","vintrait_$name")!!
    private val shouldCancelEvent = config.getBoolean("CancelEvent",false)
    private val potionEffects = hashSetOf<NamespacedKey>()

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
    }

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner) && skillName.isNotEmpty()) {
            val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
            consumedItems[traitOwner.player.uniqueId]?.let { material ->
                skillMetadata.variables.putString("consumed_material", material.name)
            }
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
        const val NAME = "mm_skill_on_item_consume"
        private val actionsMap = hashMapOf<String, MMSkillOnItemConsumeAction>()
        private val consumedItems = hashMapOf<UUID, Material>()
        private val listener = object : Listener {

            @EventHandler
            fun onItemConsume(event: PlayerItemConsumeEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (event.item.type in action.items) {
                            var potionCheck = true
                            if (action.potionEffects.isNotEmpty()) {
                                potionCheck = false
                                if (event.item.type == Material.POTION) {
                                    val itemMeta = event.item.itemMeta
                                    if (itemMeta is PotionMeta) {
                                        for (potionEffect in itemMeta.allEffects) {
                                            if (potionEffect.type.key in action.potionEffects) {
                                                potionCheck = true
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                            if (potionCheck) {
                                TraitManager.instance.executeAction(traitName,traitOwner) Function@ { traitOwner ->
                                    if (action.canExecute(traitOwner) && action.skillName.isNotEmpty()) {
                                        if (action.shouldCancelEvent)
                                            event.isCancelled = true
                                        val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
                                        skillMetadata.variables.putString("equipment_slot", event.hand.name)
                                        skillMetadata.variables.putString("consumed_material", event.item.type.name)
                                        return@Function MMAbilityData.executeMMSkill(action.skillName, skillMetadata)
                                    }
                                    return@Function false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}