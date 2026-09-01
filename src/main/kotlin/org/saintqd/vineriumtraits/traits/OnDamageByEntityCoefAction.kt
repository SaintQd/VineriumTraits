package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.NoSuchElementException

@VinTraitType("damage_by_entity_coef")
class OnDamageByEntityCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val entityTypes = config.getStringList("Types")
    // Значение Double определяет изменение коэффициента с каждым уровнем зачарования
    val enchantments = hashMapOf<Enchantment, Double>()
    val coef = config.getDouble("Coef",1.0)

    init {
        config.getConfigurationSection("Enchantments")?.let { enchantsConfig ->
            for (enchantName in enchantsConfig.getKeys(false)) {
                try {
                    val enchant = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).getOrThrow(NamespacedKey.fromString(enchantName.lowercase())!!)
                    enchantments[enchant] = enchantsConfig.getDouble(enchantName)
                }
                catch (_ : NoSuchElementException) {
                    VineriumTraits.inst().logger.warning("Traits: Trait $name: There is no Enchantment named ${enchantName.lowercase()}")
                }
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

        private val actionsMap = hashMapOf<String, OnDamageByEntityCoefAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityDamage(event : EntityDamageByEntityEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (action.entityTypes.isNotEmpty() && event.damager.type.name !in action.entityTypes)
                            continue
                        val damager = event.damager
                        var changedCoef = action.coef
                        if (action.enchantments.isNotEmpty() && damager is LivingEntity) {
                            damager.equipment?.let { equipment ->
                                val mainHand = equipment.itemInMainHand
                                for (enchant in action.enchantments.keys) {
                                    val enchantValue = action.enchantments[enchant]!!
                                    if (enchantValue > 0.0) {
                                        val level = mainHand.getEnchantmentLevel(enchant)
                                        changedCoef += enchantValue * level
                                    }
                                }
                            }
                        }
                        val changedDamage = event.damage * changedCoef
                        if (changedDamage <= 0)
                            event.isCancelled = true
                        else
                            event.damage = changedDamage
                    }
                }
            }
        }
    }
}