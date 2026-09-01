package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.*

@VinTraitType("burn_in_daylight")
class BurnInDaylightAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val maxBurnTime = config.getLong("MaxBurnTime",12300L).coerceAtMost(24000L)
    val minBurnTime = config.getLong("MinBurnTime",0L).coerceAtLeast(0L)
    val exposureSpeed = config.getDouble("ExposureSpeed", 15.0).coerceIn(0.0,100.0)
    val burnWithHelmet = config.getBoolean("BurnWithHelmet", true)
    val preventingItemKey = config.getString("PreventingItemKey","vineriumtraits:burn_in_daylight_block")!!
    val preventingItemDamage = config.getInt("PreventingItemKeyDamage",4).coerceAtLeast(0)

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
        const val NAME = "burn_in_daylight"

        private val actionsMap = hashMapOf<String, BurnInDaylightAction>()

        val currentSunExposure = hashMapOf<UUID, Double>()

        private val listener = object : Listener {

            @EventHandler
            fun onServerTickEnd(event: ServerTickEndEvent) {
                if (event.tickNumber % 80 != 0) {
                    return
                }
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.isDead) continue
                    val traitOwner = TraitManager.instance.traitOwners[player.uniqueId] ?: continue
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) continue
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->

                            var increaseExposure = false
                            var sunExposureValue = currentSunExposure.getOrDefault(player.uniqueId, 0.0)
                            if (player.world.environment == World.Environment.NORMAL
                                && player.gameMode != GameMode.CREATIVE
                                && player.gameMode != GameMode.SPECTATOR) {

                                val loc = player.location

                                var block = player.world.getHighestBlockAt(player.location)
                                while (MaterialTags.GLASS.isTagged(block) || MaterialTags.GLASS_PANES.isTagged(block)
                                    && block.y.toDouble() >= player.location.y) {

                                    block = block.getRelative(BlockFace.DOWN)
                                }

                                val height = block.y.toDouble() < player.location.y
                                // Нагреваться игрок может только под открытым небом (не в воде), если погода солнечная и если в мире в данный момент день
                                if (height && loc.getWorld().isClearWeather
                                    && loc.getWorld().time > action.minBurnTime
                                    && loc.getWorld().time < action.maxBurnTime
                                    && !player.isInWater
                                ) increaseExposure = true

                                val helm = player.inventory.helmet
                                if (!action.burnWithHelmet && helm != null && helm.type != Material.AIR) {
                                    increaseExposure = false
                                }
                                val preventingItemKey = NamespacedKey.fromString(action.preventingItemKey)!!
                                val pdc = player.inventory.itemInMainHand.persistentDataContainer
                                if (pdc.has(preventingItemKey) && increaseExposure) {
                                    increaseExposure = false
                                    if (action.preventingItemDamage > 0)
                                        player.inventory.itemInMainHand.damage(action.preventingItemDamage, player)
                                }
                            }

                            if (increaseExposure) {
                                sunExposureValue = (sunExposureValue + action.exposureSpeed).coerceAtMost(100.0)
                            } else {
                                sunExposureValue -= action.exposureSpeed
                            }

                            if (sunExposureValue <= 0) sunExposureValue = 0.0
                            else {
                                if (sunExposureValue > 40) {
                                    player.addPotionEffect(PotionEffect(PotionEffectType.NAUSEA, 200, 0, false, false))
                                    if (sunExposureValue > 60) {
                                        player.addPotionEffect(
                                            PotionEffect(
                                                PotionEffectType.SLOWNESS,
                                                200,
                                                1,
                                                false,
                                                false
                                            )
                                        )
                                        if (sunExposureValue > 80) {
                                            player.addPotionEffect(
                                                PotionEffect(
                                                    PotionEffectType.BLINDNESS,
                                                    200,
                                                    1,
                                                    false,
                                                    false
                                                )
                                            )
                                            if (sunExposureValue > 90) {
                                                player.fireTicks = 140
                                            }
                                        }
                                    }
                                }
                            }
                            currentSunExposure[player.uniqueId] = sunExposureValue
                        }
                    }
                }
            }
        }
    }
}