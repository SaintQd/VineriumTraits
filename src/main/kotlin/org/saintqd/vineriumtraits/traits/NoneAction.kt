package org.saintqd.vineriumtraits.traits

import org.bukkit.configuration.ConfigurationSection
import org.saintqd.vineriumtraits.annotations.VinTraitType

@VinTraitType("none")
class NoneAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    companion object {
        const val NAME = "none"
    }
}