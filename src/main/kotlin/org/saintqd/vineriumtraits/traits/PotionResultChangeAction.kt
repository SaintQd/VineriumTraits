package org.saintqd.vineriumtraits.traits

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.NoSuchElementException
import java.util.UUID

@VinTraitType("potion_result_change")
class PotionResultChangeAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val durationCoef = config.getDouble("DurationCoef",1.0)
    val amplifierAmount = config.getInt("AmplifierAmount",0)
    val effectsWhitelist = hashSetOf<NamespacedKey>()

    init {
        val possiblePotionEffects = config.getStringList("PotionEffects")
        for (potionEffectKey in possiblePotionEffects) {
            try {
                val effect = Registry.MOB_EFFECT.getOrThrow(Key.key(Key.MINECRAFT_NAMESPACE, potionEffectKey.lowercase()))
                effectsWhitelist.add(effect.key)
            }
            catch (ex : NoSuchElementException) {
                VineriumTraits.inst().logger.warning("Traits: Trait $name: There is no PotionEffectType named ${potionEffectKey.lowercase()}")
            }
        }
    }

    override fun register() {
        if (actionsMap.isEmpty())
            Bukkit.getServer().pluginManager.registerEvents(listener, VineriumTraits.inst())
        actionsMap[traitName] = this
    }

    override fun unregister() {
        actionsMap.remove(traitName)
        if (actionsMap.isEmpty())
            HandlerList.unregisterAll(listener)
    }

    companion object {

        private val UNMODIFIABLE_KEY = NamespacedKey(VineriumTraits.inst(),"potion_unmodifiable")

        private val brewingStands = hashMapOf<Location, UUID>()
        private val actionsMap = hashMapOf<String, PotionResultChangeAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onBrewingStandInteract(event: PlayerInteractEvent) {
                if (event.hand != EquipmentSlot.HAND) return
                if (event.action != Action.RIGHT_CLICK_BLOCK) return
                event.clickedBlock?.let { clickedBlock ->
                    if (clickedBlock.type != Material.BREWING_STAND) return
                    brewingStands[clickedBlock.location.toBlockLocation()] = event.player.uniqueId
                }
            }

            @EventHandler
            fun onPotionBrew(event: BrewEvent) {
                if (event.isCancelled) return
                brewingStands[event.block.location.toBlockLocation()]?.let { ownerUuid ->
                    val traitOwner = TraitManager.instance.traitOwners[ownerUuid] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                for (potion in event.results) {
                                    val potionMeta = potion.itemMeta
                                    if (potionMeta is PotionMeta && !potion.persistentDataContainer.has(UNMODIFIABLE_KEY)) {
                                        val newEffects = hashSetOf<PotionEffect>()
                                        for (effect in potionMeta.allEffects) {
                                            if (action.effectsWhitelist.isNotEmpty() && !action.effectsWhitelist.contains(effect.type.key))
                                                continue
                                            val newDuration = (effect.duration * action.durationCoef).toInt().coerceAtLeast(1)
                                            val newAmplifier = (effect.amplifier + action.amplifierAmount).coerceIn(0,255)
                                            newEffects.add(PotionEffect(effect.type,newDuration,newAmplifier,effect.isAmbient,effect.hasParticles(),effect.hasIcon()))
                                        }
                                        newEffects.forEach { newEffect ->
                                            potionMeta.addCustomEffect(newEffect,true)
                                        }
                                        potion.itemMeta = potionMeta

                                        if (newEffects.isNotEmpty()) {
                                            potionMeta.basePotionType?.let { basePotionType ->
                                                if (!basePotionType.isUpgradeable || !basePotionType.isExtendable)
                                                    potion.editPersistentDataContainer { pdc ->
                                                        pdc.set(
                                                            UNMODIFIABLE_KEY,
                                                            PersistentDataType.BOOLEAN, true
                                                        )
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}