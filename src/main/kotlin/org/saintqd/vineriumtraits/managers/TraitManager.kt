package org.saintqd.vineriumtraits.managers

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.saintqd.vineriumlib.managers.LangManager
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.traits.BindableAction
import org.saintqd.vineriumtraits.traits.TraitAction
import placeholders.VinTraitsPlaceholders
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
    val namesToTraits = hashMapOf<String, String>()
    private val traitActionTypes = hashMapOf<String,(String, ConfigurationSection) -> TraitAction>()

    val actionsRegistry = hashMapOf<String, TraitAction>()

    val bindHintTask : BukkitTask? = null

    data class VinTrait(
        val name: String,
        val displayName: String,
        val lore: List<String>,
        val model : String,
        val cost : Int,
        val action: TraitAction,
        val executeOnLoad: Boolean,
        val actionOnAdd : String,
        val mmSkillOnAdd : String,
        val actionOnRemove : String,
        val mmSkillOnRemove : String,
        val mmSkillOnRespawn : String,
        val mmSkillOnLoad: String,
        val mmSkillOnUnload : String,
        val canDisable : Boolean,
        val executableViaCommand : Boolean,
        val permission: String,
        val tags: Set<String>,
        val conflictingTags: Set<String>,
        val neededTags: Set<String>,
        val showIfPresent: Boolean,
        val applyCooldownOnSelect: Boolean,
        val linkedTraitNames: Set<String>
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
            if (!filePath.toString().endsWith(".yml")) continue
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
                val mmSkillOnAdd = config.getString("$traitName.MMSkillOnAdd","")!!
                val actionOnRemove = config.getString("$traitName.ActionOnRemove","")!!
                val mmSkillOnRemove = config.getString("$traitName.MMSkillOnRemove","")!!
                val mmSkillOnRespawn = config.getString("$traitName.MMSkillOnRespawn","")!!
                val mmSkillOnLoad = config.getString("$traitName.MMSkillOnLoad","")!!
                val mmSkillOnUnload = config.getString("$traitName.MMSkillOnUnload","")!!
                val canDisable = config.getBoolean("$traitName.CanDisable",true)
                val executableViaCommand = config.getBoolean("$traitName.ExecutableViaCommand",false)
                val permission = config.getString("$traitName.Permission","")!!
                val tags = config.getStringList("$traitName.Tags").toSet()
                val conflictingTags = config.getStringList("$traitName.ConflictingTags").toSet()
                val neededTags = config.getStringList("$traitName.NeededTags").toSet()
                val showIfPresent = config.getBoolean("$traitName.ShowIfPresent",false)
                val applyCooldownOnSelect = config.getBoolean("$traitName.ApplyCooldownOnSelect",true)
                val linkedTraitNames = config.getStringList("$traitName.LinkedTraits").toSet()
                val traitActionFunction = traitActionTypes[type]
                if (traitActionFunction == null) {
                    VineriumTraits.inst().logger.warning { "Traits: There is no action type of $type! Is it registered? Trait $traitName won't be loaded." }
                    continue
                }

                val traitAction = registerAction(traitName,traitConfig,traitActionFunction)
                val trait = VinTrait(traitName.lowercase(), display, lore,model, cost,traitAction,
                    executeOnLoad,actionOnAdd,mmSkillOnAdd, actionOnRemove,mmSkillOnRemove,mmSkillOnRespawn,
                    mmSkillOnLoad,mmSkillOnUnload, canDisable, executableViaCommand,permission,tags,conflictingTags,
                    neededTags,showIfPresent,applyCooldownOnSelect,linkedTraitNames)

                if (config.getBoolean("$traitName.RegisterAsPlaceholder")) {
                    VinTraitsPlaceholders.instance?.let { instance ->
                        instance.registerPlaceholder("trait_$traitName") Function@{ _, _ ->
                            return@Function MiniMessage.miniMessage().stripTags(trait.displayName)
                        }
                    }
                }
                if (config.getBoolean("$traitName.RegisterTagsAsPlaceholders")) {
                    VinTraitsPlaceholders.instance?.let { instance ->
                        for (tag in trait.tags) {
                            if (instance.placeholders.contains("trait_tag_$tag"))
                                continue
                            instance.registerPlaceholder("trait_tag_$tag") Function@{ _, player ->
                                TraitManager.instance.traitOwners[player.uniqueId]?.let { owner ->
                                    for (playerTraitName in owner.traits) {
                                        TraitManager.instance.traits[playerTraitName]?.let { playerTrait ->
                                            if (tag in playerTrait.tags) {
                                                return@Function MiniMessage.miniMessage().stripTags(playerTrait.displayName)
                                            }
                                        }
                                    }
                                }
                                return@Function ""
                            }
                        }
                    }
                }

                namesToTraits[MiniMessage.miniMessage().stripTags(display)] = trait.name
                traits[trait.name] = trait
            }
        }
        bindHintTask?.cancel()
        val bindHintPeriod = VineriumTraits.inst().config.getLong("Traits.BindHintPeriod",6000L)
        if (bindHintPeriod > 0) {
            Bukkit.getScheduler().runTaskTimer(VineriumTraits.inst(), Runnable {
                for (player in Bukkit.getOnlinePlayers()) {
                    val traitOwner = traitOwners[player.uniqueId] ?: continue
                    for (traitName in traitOwner.traits) {
                        val trait = traits[traitName] ?: continue
                        if (trait.action is BindableAction && trait.action.isBindable()) {
                            if (!traitOwner.bindedTraits.contains(traitName)) {
                                traitOwner.player.sendMessage(LangManager.INSTANCE.parseLangString(VineriumTraits.inst(),"binded_traits_hint_message"))
                                break
                            }
                        }
                    }
                }
            }, bindHintPeriod,bindHintPeriod)
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

    // Используется для простой логики обработки действий
    fun executeAction(actionName : String, traitOwner: TraitOwner) : Boolean {
        if (!actionsRegistry.contains(actionName)) {
            VineriumTraits.inst().logger.warning { "Trait action $actionName does not exist." }
        }
        actionsRegistry[actionName]?.let { action ->
            val result = action.executeFunction(traitOwner)
            if (result)
                action.applyCooldown(traitOwner)
            return result
        }
        return false
    }

    // Используется для комплексной логики обработки действий
    fun executeAction(actionName : String, traitOwner: TraitOwner, function : (TraitOwner) -> Boolean) : Boolean {
        if (!actionsRegistry.contains(actionName)) {
            VineriumTraits.inst().logger.warning { "Trait action $actionName does not exist." }
        }
        actionsRegistry[actionName]?.let { action ->
            val result = function(traitOwner)
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