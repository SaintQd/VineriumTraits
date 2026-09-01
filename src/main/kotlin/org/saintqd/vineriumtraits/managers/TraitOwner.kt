package org.saintqd.vineriumtraits.managers

import org.bukkit.entity.Player
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.enums.InteractionType

class TraitOwner(val player: Player) {

    val traits = hashSetOf<String>()
    val cachedTraits = hashSetOf<String>()
    val preselectedTraits = hashSetOf<String>()
    val preselectedTraitsToRemove = hashSetOf<String>()
    val cooldowns = hashMapOf<String,Long>()
    var lastTraitChangeTimestamp = 0L
    val bindedTraits = hashMapOf<String,TraitBindData>()

    data class TraitBindData(
        val interactionType: InteractionType,
        val requireSneaking: Boolean,
        val requireEmptyHand: Boolean
    )

    fun addTrait(trait: TraitManager.VinTrait, load : Boolean = false) {
        trait.action.onLoad(this)
        if (trait.mmSkillOnLoad.isNotEmpty()) {
            val skillMetadata = MMAbilityData.prepareMMSkillData(player)
            MMAbilityData.executeMMSkill(trait.mmSkillOnLoad,skillMetadata)
        }
        if (!load) {
            trait.action.onAdd(this)
            if (trait.actionOnAdd.isNotEmpty()) {
                TraitManager.instance.executeAction(trait.actionOnAdd, this)
            }
            if (trait.mmSkillOnAdd.isNotEmpty()) {
                val skillMetadata = MMAbilityData.prepareMMSkillData(player)
                MMAbilityData.executeMMSkill(trait.mmSkillOnAdd,skillMetadata)
            }
        }
        traits.add(trait.name)
        for (linkedTraitName in trait.linkedTraitNames) {
            val linkedTrait = TraitManager.instance.traits[linkedTraitName] ?: continue
            addTrait(linkedTrait,load)
        }
    }

    fun removeTrait(trait: TraitManager.VinTrait, unload: Boolean = false) {
        trait.action.onUnload(this)
        if (trait.mmSkillOnUnload.isNotEmpty()) {
            val skillMetadata = MMAbilityData.prepareMMSkillData(player)
            MMAbilityData.executeMMSkill(trait.mmSkillOnUnload,skillMetadata)
        }
        if (!unload) {

            trait.action.onRemove(this)
            if (trait.actionOnRemove.isNotEmpty()) {
                TraitManager.instance.executeAction(trait.actionOnRemove, this)
            }
            if (trait.mmSkillOnRemove.isNotEmpty()) {
                val skillMetadata = MMAbilityData.prepareMMSkillData(player)
                MMAbilityData.executeMMSkill(trait.mmSkillOnRemove,skillMetadata)
            }
        }
        for (linkedTraitName in trait.linkedTraitNames) {
            val linkedTrait = TraitManager.instance.traits[linkedTraitName] ?: continue
            removeTrait(linkedTrait,unload)
        }
        traits.remove(trait.name)
    }

}