package org.saintqd.vineriumtraits.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.RangedInt
import io.lumine.mythic.core.utils.annotations.MythicCondition
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.traits.CountHitEntityTypesAction

@MythicCondition(author = "SaintQd", name = "vintraitscounthitentitytypesamount")
class VinCountHitEntityTypesAmountCondition(event : MythicConditionLoadEvent) : IEntityCondition {

    val amount = RangedInt(event.config.getString(arrayOf("amount", "a"), ">0"))

    override fun check(abstractEntity: AbstractEntity): Boolean {
        VinUtils.sendDebugMessage(3,"MythicMobsCondition: vintraitscounthitentitytypesamount")

        val entity = abstractEntity.bukkitEntity
        VinUtils.sendDebugMessage(4,"Entity: $entity")
        if (entity is Player) {
            VinUtils.sendDebugMessage(4,"Entity is Player")
            val traitOwner = TraitManager.instance.traitOwners[entity.uniqueId] ?: return false
            if (!traitOwner.player.persistentDataContainer.has(CountHitEntityTypesAction.ACTION_KEY))
                return false
            val pdcValue = traitOwner.player.persistentDataContainer.getOrDefault(CountHitEntityTypesAction.ACTION_KEY,
                PersistentDataType.INTEGER,0)
            return amount.equals(pdcValue)
        }
        return true
    }
}