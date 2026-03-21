package org.saintqd.vineriumtraits.managers

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.traits.TraitOwner
import org.saintqd.vineriumtraits.traits.TraitAction
import java.io.File
import java.util.UUID
import java.util.logging.Level
import kotlin.collections.set

class TraitManager {

    companion object {
        val instance : TraitManager = TraitManager()

        val TRAIT_NAME_REGEX = "[a-z0-9_-]+".toRegex()
    }

    val traitOwners = hashMapOf<UUID, TraitOwner>()
    val traits = hashMapOf<String, VinTrait>()
    private val traitActionTypes = hashMapOf<String,(String, ConfigurationSection) -> TraitAction>()

    private val actionsRegistry = hashMapOf<String, TraitAction>()

    data class VinTrait(
        val name: String,
        val displayName: String,
        val lore: List<String>,
        val model : String,
        val cost : Int,
        val action: TraitAction,
        val executeOnLoad: Boolean,
        val actionOnAdd : String,
        val actionOnRemove : String,
        val canDisable : Boolean,
        val permission: String,
        val linkedTraitNames: List<String>
    )

    fun loadParams(plugin : Plugin) {
        traits.clear()
        val traitsDir = File(plugin.dataFolder, "Traits")
        if (!traitsDir.exists()) {
            plugin.logger.log(Level.INFO,"Traits directory does not exist, creating it.")
            if (!traitsDir.mkdir()) {
                plugin.logger.log(Level.SEVERE,"Could not create Traits directory!")
                return
            }
        }
        val filePaths = VinUtils.listFilesInFolder(plugin.dataFolder.path + File.separator + "Traits")
        for (filePath in filePaths) {
            val config = YamlConfiguration.loadConfiguration(filePath.toFile())
            for (traitName in config.getKeys(false)) {

                if (!traitName.matches(TRAIT_NAME_REGEX)) {
                    VineriumTraits.inst().logger.warning { "Traits: Trait name $traitName does not match regex: $TRAIT_NAME_REGEX! Trait won't be loaded." }
                    continue
                }

                val traitConfig = config.getConfigurationSection(traitName)!!
                val display = config.getString("$traitName.Display",traitName)!!
                val lore = config.getStringList("$traitName.Lore")
                val model = config.getString("$traitName.Model","")!!
                val cost = config.getInt("$traitName.Cost",0)
                val type = config.getString("$traitName.ActionType","none")
                val executeOnLoad = config.getBoolean("$traitName.ExecuteOnLoad",true)
                val actionOnAdd = config.getString("$traitName.ActionOnAdd","")!!
                val actionOnRemove = config.getString("$traitName.ActionOnRemove","")!!
                val canDisable = config.getBoolean("$traitName.CanDisable",true)
                val permission = config.getString("$traitName.Permission","")!!
                val linkedTraitNames = config.getStringList("$traitName.LinkedTraits")
                val traitActionFunction = traitActionTypes[type]
                if (traitActionFunction == null) {
                    VineriumTraits.inst().logger.warning { "Traits: There is no trait type for $traitName! Trait $traitActionFunction won't be loaded." }
                    continue
                }

                val traitAction = registerAction(traitName,traitConfig,traitActionFunction)
                val trait = VinTrait(traitName.lowercase(), display, lore,model, cost,traitAction,
                    executeOnLoad,actionOnAdd,actionOnRemove,canDisable,permission,linkedTraitNames)

                traits[trait.name] = trait
            }
        }
    }

    fun registerAction(name : String, config : ConfigurationSection, function : (String, ConfigurationSection) -> TraitAction) : TraitAction {
        val traitAction = function.invoke(name,config)
        traitAction.register()
        actionsRegistry[name] = traitAction
        return traitAction
    }

    fun unregisterAllActions() {
        actionsRegistry.values.forEach { action ->
            action.unregister()
        }
        actionsRegistry.clear()
    }

    fun executeAction(actionName : String, traitOwner: TraitOwner) : Boolean {
        actionsRegistry[actionName]?.let { action ->
            val result = action.executeFunction(traitOwner)
            if (result)
                action.applyCooldown(traitOwner)
            return result
        }
        return false
    }

    fun getActionsRegistrySize() : Int {
        return actionsRegistry.size
    }

    fun registerActionType(actionName : String, function : (String, ConfigurationSection) -> TraitAction) {
        if (VineriumTraits.inst().loadFinished) {
            VineriumTraits.inst().logger.severe("Could not register trait action $actionName: Registering only allowed during server loading phase.")
            return
        }
        else {
            traitActionTypes[actionName] = function
        }
    }

    /*fun getTraitActionType(actionName : String) : ((String, ConfigurationSection) -> TraitAction)? {
        return traitActionTypes[actionName]
    }*/
}