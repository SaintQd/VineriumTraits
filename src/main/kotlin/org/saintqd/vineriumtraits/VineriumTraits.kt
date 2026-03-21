package org.saintqd.vineriumtraits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.utils.ResourceUtils
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.commands.VinTraitCommands
import org.saintqd.vineriumtraits.listeners.MythicMobsListener
import org.saintqd.vineriumtraits.listeners.PlayerListener
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.storage.DataStorage
import org.saintqd.vineriumtraits.storage.MySQLStorage
import org.saintqd.vineriumtraits.traits.AttributeAction
import org.saintqd.vineriumtraits.traits.CommandAction
import org.saintqd.vineriumtraits.traits.FoodVegetarianAction
import org.saintqd.vineriumtraits.traits.OnInteractAction
import org.saintqd.vineriumtraits.traits.MMSkillAction
import org.saintqd.vineriumtraits.traits.MMSkillOnAttackAction
import org.saintqd.vineriumtraits.traits.MMSkillOnDamagedAction
import org.saintqd.vineriumtraits.traits.MultipleAction
import org.saintqd.vineriumtraits.traits.NoneAction
import org.saintqd.vineriumtraits.traits.OnSessionAction
import org.saintqd.vineriumtraits.traits.OnTimerAction
import java.io.File
import java.util.concurrent.TimeUnit

class VineriumTraits : JavaPlugin() {

    var storage : DataStorage? = null
    var loadFinished = false

    var mythicMobsEnabled = false

    companion object {
        private var plugin : VineriumTraits? = null

        fun inst() : VineriumTraits {
            return plugin!!
        }
    }

    override fun onLoad() {
        plugin = this

        TraitManager.instance.registerActionType(NoneAction.NAME) {
                name : String, config : ConfigurationSection -> NoneAction(name,config) }
        TraitManager.instance.registerActionType(OnSessionAction.NAME) {
                name : String, config : ConfigurationSection -> OnSessionAction(name,config) }
        TraitManager.instance.registerActionType(CommandAction.NAME) {
                name : String, config : ConfigurationSection -> CommandAction(name,config) }
        TraitManager.instance.registerActionType(OnInteractAction.NAME) {
                name : String, config : ConfigurationSection -> OnInteractAction(name,config) }
        TraitManager.instance.registerActionType(MMSkillAction.NAME) {
                name : String, config : ConfigurationSection -> MMSkillAction(name,config) }
        TraitManager.instance.registerActionType(MMSkillOnAttackAction.NAME) {
                name : String, config : ConfigurationSection -> MMSkillOnAttackAction(name,config) }
        TraitManager.instance.registerActionType(MMSkillOnDamagedAction.NAME) {
                name : String, config : ConfigurationSection -> MMSkillOnDamagedAction(name,config) }
        TraitManager.instance.registerActionType(MultipleAction.NAME) {
                name : String, config : ConfigurationSection -> MultipleAction(name,config) }
        TraitManager.instance.registerActionType(AttributeAction.NAME) {
                name : String, config : ConfigurationSection -> AttributeAction(name,config) }
        TraitManager.instance.registerActionType(OnTimerAction.NAME) {
                name : String, config : ConfigurationSection -> OnTimerAction(name,config) }
        TraitManager.instance.registerActionType(FoodVegetarianAction.NAME) {
                name : String, config : ConfigurationSection -> FoodVegetarianAction(name,config) }
    }

    override fun onEnable() {
        loadFinished = true
        ResourceUtils.fetchAllResources(this, file)

        val mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs")
        if (mythicMobs != null && mythicMobs.isEnabled) {
            mythicMobsEnabled = true
            VinUtils.sendDebugMessage(0,"MythicMobs found, compatibility features enabled.")
            val mythicMobsListener = MythicMobsListener()
            mythicMobsListener.registerMechanics()
            mythicMobsListener.registerConditions()
            server.pluginManager.registerEvents(mythicMobsListener,this)
        }

        loadData()

        when(config.getString("Storage","mysql")!!) {
            "mysql" -> storage = MySQLStorage()
        }

        VinTraitCommands.setupCommands(this)

        server.pluginManager.registerEvents(PlayerListener(), this)

        server.asyncScheduler.runAtFixedRate(this, {
            saveData()
        }, 30L, 30L, TimeUnit.MINUTES)
    }

    override fun onDisable() {
        saveData()
    }

    fun saveData() {
        storage?.save()
        storage?.saveOnlinePlayersData()
        logger.info("Trait owners data saved.")
    }

    fun loadData() {
        reloadConfig()

        val selectedLang = getConfig().getString("Language")
        val langLines = VineriumLib.inst().langManager.loadLanguageFile(
            this,
            dataFolder.path + File.separator + "lang" + File.separator + selectedLang + ".yml"
        )
        VineriumLib.inst().langManager.registerLangLines(langLines)

        var prevTime = System.currentTimeMillis()
        TraitManager.instance.unregisterAllActions()
        TraitManager.instance.loadParams(this)
        var time = System.currentTimeMillis()
        logger.info("Loaded " + TraitManager.instance.traits.size + " traits (${TraitManager.instance.getActionsRegistrySize()} total actions). ("+(time-prevTime)+" ms)")
        prevTime = System.currentTimeMillis()

    }

}