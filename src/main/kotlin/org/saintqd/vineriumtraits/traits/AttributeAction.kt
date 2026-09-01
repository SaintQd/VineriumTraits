package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.events.TraitOwnerJoinEvent
import org.saintqd.vineriumtraits.events.TraitOwnerQuitEvent
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner
import java.util.UUID

@VinTraitType("attribute")
class AttributeAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val attributes = linkedMapOf<String, Pair<AttributeModifier.Operation, Double>>()

    init {
        val attributeConfig = config.getConfigurationSection("Attributes")
        if (attributeConfig != null) {
            for (attributeType in attributeConfig.getKeys(false)) {
                val attributeData = attributeConfig.getString(attributeType)!!.split(",")
                val operation = AttributeModifier.Operation.valueOf(attributeData[0])
                val value = attributeData[1].toDouble()
                attributes[attributeType] = Pair(operation, value)
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
        const val NAME = "attribute"

        private val actionsMap = hashMapOf<String, AttributeAction>()
        // True - вход, False - выход
        private val sessionType = hashMapOf<UUID, Boolean>()
        private val listener = object : Listener {
            @EventHandler
            fun onTraitOwnerJoin(event : TraitOwnerJoinEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.traitOwner.player.uniqueId] ?: return
                for (actionName in actionsMap.keys) {
                    actionsMap[actionName]?.let { action ->
                        sessionType[traitOwner.player.uniqueId] = true
                        TraitManager.instance.executeAction(actionName,traitOwner)
                        sessionType.remove(traitOwner.player.uniqueId)
                    }
                }
            }

            @EventHandler
            fun onTraitOwnerLeave(event : TraitOwnerQuitEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.traitOwner.player.uniqueId] ?: return
                for (actionName in actionsMap.keys) {
                    actionsMap[actionName]?.let { action ->
                        sessionType[traitOwner.player.uniqueId] = false
                        TraitManager.instance.executeAction(actionName,traitOwner)
                        sessionType.remove(traitOwner.player.uniqueId)
                    }
                }
            }
        }
    }

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            val sessionState = sessionType[traitOwner.player.uniqueId] ?: return@Function false
            var index = 0
            attributes.forEach { (name, pair) ->
                val attributeNamespacedKey = NamespacedKey.fromString(name) ?: return@forEach
                val attribute = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).get(attributeNamespacedKey) ?: return@forEach

                val attributeInstance = traitOwner.player.getAttribute(attribute) ?: return@forEach

                val parsedAttributeName = NamespacedKey(VineriumTraits.inst(), "${this.traitName}_attribute_${name}_$index")
                if (sessionState)
                    attributeInstance.addModifier(AttributeModifier(parsedAttributeName,pair.second,pair.first))
                else
                    attributeInstance.removeModifier(parsedAttributeName)

                index++
            }
            return@Function true
        }
        return@Function false
    }
}