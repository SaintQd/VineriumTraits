package org.saintqd.vineriumtraits.traits

import org.bukkit.configuration.ConfigurationSection
import org.saintqd.vineriumtraits.managers.TraitManager

class MultipleAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val actionNames = config.getStringList("Actions")

    companion object {
        const val NAME = "multiple_actions"
    }

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            for (actionName in actionNames) {
                return@Function TraitManager.instance.executeAction(actionName,traitOwner)
            }
        }
        return@Function false
    }
}