package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemDamageEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.events.TraitOwnerJoinEvent
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("better_gold_armor")
class BetterGoldArmorAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

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
        var attributeInstance = owner.player.getAttribute(Attribute.ARMOR) ?: return
        attributeInstance.removeModifier(ATTRIBUTE_KEY)

        attributeInstance = owner.player.getAttribute(Attribute.ARMOR_TOUGHNESS) ?: return
        attributeInstance.removeModifier(ATTRIBUTE_KEY)
    }

    companion object {
        const val NAME = "better_gold_armor"

        val ATTRIBUTE_KEY = NamespacedKey(VineriumTraits.inst(), "better_gold_armor")
        val armorSet = hashSetOf(Material.GOLDEN_HELMET,Material.GOLDEN_CHESTPLATE,Material.GOLDEN_LEGGINGS,Material.GOLDEN_BOOTS)

        private val actionsMap = hashMapOf<String, BetterGoldArmorAction>()

        fun addAttribute(player : Player) {
            var amountArmor = 0.0
            var amountArmorToughness = 0.0

            var attributeInstance = player.getAttribute(Attribute.ARMOR) ?: return
            attributeInstance.removeModifier(ATTRIBUTE_KEY)

            attributeInstance = player.getAttribute(Attribute.ARMOR_TOUGHNESS) ?: return
            attributeInstance.removeModifier(ATTRIBUTE_KEY)

            for (item in player.equipment.armorContents) {
                if (item == null) continue
                when (item.type) {
                    Material.GOLDEN_HELMET -> {
                        amountArmor += 1
                    }
                    Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS -> {
                        amountArmor += 3
                        amountArmorToughness += 2
                    }
                    Material.GOLDEN_BOOTS -> {
                        amountArmor += 2
                    }
                    else -> {}
                }
            }

            attributeInstance = player.getAttribute(Attribute.ARMOR) ?: return
            var modifier = AttributeModifier(ATTRIBUTE_KEY, amountArmor, AttributeModifier.Operation.ADD_NUMBER)
            attributeInstance.addTransientModifier(modifier)

            attributeInstance = player.getAttribute(Attribute.ARMOR_TOUGHNESS) ?: return
            modifier = AttributeModifier(ATTRIBUTE_KEY, amountArmor, AttributeModifier.Operation.ADD_NUMBER)
            attributeInstance.addTransientModifier(modifier)
        }

        private val listener = object : Listener {

            @EventHandler
            fun onTraitOwnerJoin(event : TraitOwnerJoinEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.traitOwner.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isNotEmpty()) {
                    addAttribute(event.traitOwner.player)
                }
            }

            @EventHandler
            fun onArmorEquip(event: PlayerArmorChangeEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isNotEmpty()) {
                    addAttribute(event.player)
                }
            }

            @EventHandler
            fun onItemDamage(event : PlayerItemDamageEvent) {
                if (event.item.type !in armorSet) return
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isNotEmpty()) {
                    event.isCancelled = true
                }
            }
        }
    }
}