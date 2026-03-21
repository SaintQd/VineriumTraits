package org.saintqd.vineriumtraits.traits

import org.bukkit.configuration.ConfigurationSection

class NoneAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    companion object {
        const val NAME = "none"
    }
}