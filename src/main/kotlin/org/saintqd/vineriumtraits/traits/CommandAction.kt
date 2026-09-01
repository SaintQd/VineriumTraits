package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("command")
class CommandAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val commands = config.getStringList("Commands")

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            for (command in commands) {
                val parsedCommand = command.replace("%player_name%", traitOwner.player.name)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand)
            }
            return@Function true
        }
        return@Function false
    }

    companion object {
        const val NAME = "command"
    }
}