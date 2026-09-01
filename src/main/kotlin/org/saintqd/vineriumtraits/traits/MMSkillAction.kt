package org.saintqd.vineriumtraits.traits

import org.bukkit.configuration.ConfigurationSection
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("mm_skill")
class MMSkillAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val skillName = config.getString("SkillName","vintrait_$name")!!

    companion object {
        const val NAME = "mm_skill"
    }

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
            return@Function MMAbilityData.executeMMSkill(skillName,skillMetadata)
        }
        else
            return@Function false
    }
}