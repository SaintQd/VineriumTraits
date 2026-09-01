package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.potion.PotionEffectType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("cancel_entity_target_when_invisible")
class CancelEntityTargetWhenInvisible(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val excludingEntityList = hashSetOf<EntityType>()

    init {
        excludingEntityList.add(EntityType.WITHER)
        excludingEntityList.add(EntityType.ENDER_DRAGON)
        excludingEntityList.add(EntityType.EXPERIENCE_ORB)
        if (config.contains("ExcludingTypes"))
            excludingEntityList.clear()
        config.getStringList("ExcludingTypes").forEach { entityName ->
            val event = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE).getOrThrow(Key.key(entityName.lowercase()))
            excludingEntityList.add(event)
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

        private val actionsMap = hashMapOf<String, CancelEntityTargetWhenInvisible>()

        private val listener = object : Listener {

            @EventHandler
            fun onEntityTarget(event: EntityTargetLivingEntityEvent) {
                event.target?.let { target ->
                    if (target is Player) {
                        if (!target.hasPotionEffect(PotionEffectType.INVISIBILITY)) return
                        val traitOwner = TraitManager.instance.traitOwners[target.uniqueId] ?: return
                        val commonTraits = traitOwner.traits intersect actionsMap.keys
                        if (commonTraits.isEmpty()) return
                        for (traitName in commonTraits) {
                            actionsMap[traitName]?.let { action ->
                                if (!action.excludingEntityList.contains(event.entity.type) && TraitManager.instance.executeAction(traitName, traitOwner)) {
                                    if (event.entity is Mob) {
                                        val mob = event.entity as Mob
                                        mob.target = null
                                    }
                                    event.isCancelled = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}