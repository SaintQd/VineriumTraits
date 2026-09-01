package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("deny_enchantments")
class DenyEnchantmentsAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            for (equipmentSlot in EquipmentSlot.entries) {
                traitOwner.player.inventory.getItem(equipmentSlot).let { item ->
                    if (item.type != Material.AIR && item.enchantments.isNotEmpty()) {
                        traitOwner.player.location.world.playSound(
                            traitOwner.player.location,
                            Sound.ENTITY_ARMOR_STAND_BREAK, 1.0f, 1.0f
                        )
                        traitOwner.player.damage(0.01, DamageSource.builder(DamageType.HOT_FLOOR).build())
                        traitOwner.player.dropItem(equipmentSlot)
                    }
                }
            }

            return@Function true
        }
        else
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
        const val NAME = "deny_enchantments"

        private val actionsMap = hashMapOf<String, DenyEnchantmentsAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onServerTickEnd(event : ServerTickEndEvent) {
                if (event.tickNumber % 5 != 0)
                    return
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.isDead) continue
                    val traitOwner = TraitManager.instance.traitOwners[player.uniqueId] ?: continue
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) continue
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            TraitManager.instance.executeAction(traitName, traitOwner)
                        }
                    }
                }
            }
        }
    }
}