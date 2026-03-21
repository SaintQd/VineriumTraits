package org.saintqd.vineriumtraits.traits

import org.bukkit.entity.Player
import org.saintqd.vineriumtraits.managers.TraitManager

class TraitOwner(val player: Player) {

    val traits = hashSetOf<String>()
    val preselectedTraits = hashSetOf<String>()
    val preselectedTraitsToRemove = hashSetOf<String>()
    val cooldowns = hashMapOf<String,Long>()
    var lastTraitChangeTimestamp = 0L

    fun addTrait(trait: TraitManager.VinTrait) {
        traits.add(trait.name)
        if (trait.executeOnLoad && trait.actionOnAdd.isNotEmpty()) {
            TraitManager.instance.executeAction(trait.actionOnAdd, this)
        }
        for (linkedTraitName in trait.linkedTraitNames) {
            val linkedTrait = TraitManager.instance.traits[linkedTraitName] ?: continue
            addTrait(linkedTrait)
        }
    }

    fun removeTrait(trait: TraitManager.VinTrait) {
        if (trait.actionOnRemove.isNotEmpty()) {
            TraitManager.instance.executeAction(trait.actionOnRemove, this)
        }
        for (linkedTraitName in trait.linkedTraitNames) {
            val linkedTrait = TraitManager.instance.traits[linkedTraitName] ?: continue
            removeTrait(linkedTrait)
        }
        traits.remove(trait.name)
    }

}