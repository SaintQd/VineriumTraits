package org.saintqd.vineriumtraits

import io.lumine.mythic.core.skills.CustomComponentRegistry
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.utils.ResourceUtils
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.commands.VinTraitCommands
import org.saintqd.vineriumtraits.listeners.BindableActionListener
import org.saintqd.vineriumtraits.listeners.PlayerListener
import org.saintqd.vineriumtraits.managers.ActionTypeRegistrar
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.storage.DataStorage
import org.saintqd.vineriumtraits.storage.MySqlJdbiStorage
import placeholders.VinTraitsPlaceholders
import java.io.File
import java.util.concurrent.TimeUnit

class VineriumTraits : JavaPlugin() {

    var storage : DataStorage? = null
    var loadFinished = false

    var mythicMobsEnabled = false
    var cmiEnabled = false

    companion object {
        private var plugin : VineriumTraits? = null

        fun inst() : VineriumTraits {
            return plugin!!
        }
    }

    override fun onLoad() {
        plugin = this

        ActionTypeRegistrar.registerFromPackage("org.saintqd.vineriumtraits.traits")
    }

    override fun onEnable() {
        loadFinished = true
        ResourceUtils.fetchAllResources(this, file)

        val cmi = Bukkit.getPluginManager().getPlugin("CMI")
        if (cmi != null && cmi.isEnabled()) {
            cmiEnabled = true
            VinUtils.sendDebugMessage(0, "CMI found, compatibility features enabled.")
        }

        val placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
        if (placeholderAPI != null && placeholderAPI.isEnabled) {
            VinTraitsPlaceholders.instance = VinTraitsPlaceholders(this)
            VinTraitsPlaceholders.instance?.registerPlaceholders()
            VinTraitsPlaceholders.instance?.register()
        }

        val mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs")
        if (mythicMobs != null && mythicMobs.isEnabled) {
            mythicMobsEnabled = true
            VinUtils.sendDebugMessage(0,"MythicMobs found, compatibility features enabled.")

            CustomComponentRegistry(this,"org.saintqd.vineriumtraits.mythicmobs")

            //val mythicMobsListener = MythicMobsListener()
            //mythicMobsListener.registerMechanics()
            //mythicMobsListener.registerConditions()
            //server.pluginManager.registerEvents(mythicMobsListener,this)
        }

        loadData()

        when(config.getString("Storage","mysql")!!) {
            "mysql" -> storage = MySqlJdbiStorage()
        }

        VinTraitCommands.setupCommands(this)

        server.pluginManager.registerEvents(PlayerListener(), this)
        server.pluginManager.registerEvents(BindableActionListener(), this)

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