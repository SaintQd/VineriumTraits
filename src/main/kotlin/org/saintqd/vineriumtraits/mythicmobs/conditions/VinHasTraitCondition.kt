package org.saintqd.vineriumtraits.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.core.skills.SkillCondition
import org.bukkit.entity.Player
import org.saintqd.vineriumtraits.managers.TraitManager

class VinHasTraitCondition(line: String, mlc : MythicLineConfig) : SkillCondition(line), IEntityCondition {

    val traitName: String = mlc.getString(arrayOf("trait", "t"),"")

    override fun check(abstractEntity: AbstractEntity): Boolean {
        if (traitName.isEmpty())
            return true
        val entity = abstractEntity.bukkitEntity
        if (entity is Player) {
            val traitOwner = TraitManager.instance.traitOwners[entity.uniqueId] ?: return false
            return traitOwner.traits.contains(traitName)
        }
        return true
    }
}