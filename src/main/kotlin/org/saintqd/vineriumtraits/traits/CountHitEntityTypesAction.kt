package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Damageable
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("count_hit_entity_types")
class CountHitEntityTypesAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var entityList = hashSetOf<EntityType>()
    var subtractEntityList = hashSetOf<EntityType>()

    val minAmount = config.getInt("MinAmount",-30)
    val maxAmount = config.getInt("MaxAmount",30)
    val actionBarMessage = config.getString("ActionBarMessage","<red>Жажда крови: <gold>{1}</gold> / <gold>{2}")!!

    init {
        entityList.add(EntityType.PLAYER)
        if (config.contains("Types")) {
            entityList.clear()
            config.getStringList("Types").forEach { entityName ->
                val event = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE)
                    .getOrThrow(Key.key(entityName.lowercase()))
                entityList.add(event)
            }
        }
        subtractEntityList.add(EntityType.COW)
        subtractEntityList.add(EntityType.MOOSHROOM)
        subtractEntityList.add(EntityType.SHEEP)
        subtractEntityList.add(EntityType.PIG)
        subtractEntityList.add(EntityType.CHICKEN)
        subtractEntityList.add(EntityType.HORSE)
        subtractEntityList.add(EntityType.DONKEY)
        subtractEntityList.add(EntityType.MULE)
        if (config.contains("SubtractTypes")) {
            subtractEntityList.clear()
            config.getStringList("SubtractTypes").forEach { entityName ->
                val event = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE)
                    .getOrThrow(Key.key(entityName.lowercase()))
                subtractEntityList.add(event)
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
        val ACTION_KEY = NamespacedKey(VineriumTraits.inst(),"count_hit_entity_types")

        private val actionsMap = hashMapOf<String, CountHitEntityTypesAction>()
        private val listener = object : Listener {

            @EventHandler(priority = EventPriority.HIGH)
            fun onEntitYDamage(event: EntityDamageByEntityEvent) {
                if (event.isCancelled) return
                val damager = event.damageSource.causingEntity
                if (damager !is Player) return
                val traitOwner = TraitManager.instance.traitOwners[damager.uniqueId] ?: return
                val entity = event.entity
                if (entity !is Damageable) return

                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName,traitOwner)) {
                            if (entity.type in action.entityList || entity.type in action.subtractEntityList) {
                                var currentAmount = traitOwner.player.persistentDataContainer.getOrDefault(ACTION_KEY,
                                    PersistentDataType.INTEGER,0)
                                if (entity.type in action.entityList)
                                    currentAmount++
                                if (entity.type in action.subtractEntityList)
                                    currentAmount--
                                currentAmount = currentAmount.coerceIn(action.minAmount,action.maxAmount)
                                traitOwner.player.persistentDataContainer.set(ACTION_KEY,PersistentDataType.INTEGER,currentAmount)
                                traitOwner.player.sendActionBar(MiniMessage.miniMessage().deserialize(
                                    action.actionBarMessage.replace("{1}",currentAmount.toString())
                                        .replace("{2}",action.maxAmount.toString())))
                                //if (currentAmount == action.minAmount || currentAmount == action.maxAmount
                                //    || currentAmount == (action.minAmount + action.maxAmount) / 2) {
                                //    traitOwner.player.sendActionBar(MiniMessage.miniMessage().deserialize(
                                //        action.actionBarMessage.replace("{1}",currentAmount.toString())
                                //            .replace("{2}",action.maxAmount.toString())))
                                //}
                            }
                        }
                    }
                }
            }
        }
    }
}