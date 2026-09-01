package org.saintqd.vineriumtraits.traits

import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EnderPearl
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("throw_enderpearl")
class ThrowEnderpearlAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val velocity = config.getDouble("Velocity",1.0)

    companion object {
        const val NAME = "throw_enderpearl"
    }

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            traitOwner.player.world.playSound(traitOwner.player.location,Sound.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS,1f,0.5f)
            val enderpearl = traitOwner.player.launchProjectile(EnderPearl::class.java)
            enderpearl.shooter = traitOwner.player
            enderpearl.velocity = enderpearl.velocity.multiply(velocity)
            return@Function true
        }
        else
            return@Function false
    }
}