package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import kotlin.math.floor


@VinTraitType("lava_walk")
class LavaWalkAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val walkSpeed = config.getString("WalkSpeed","0.1f")!!.toFloat()
    val requiredPdc : NamespacedKey?

    init {
        val requiredPDC = config.getString("RequiredPDC","")!!
        requiredPdc = if (requiredPDC.isNotEmpty()) {
            NamespacedKey.fromString(requiredPDC)
        } else
            null
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
        const val NAME = "lava_walk"

        private val actionsMap = hashMapOf<String, LavaWalkAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onServerTickEnd(event : ServerTickEndEvent) {
                if (event.tickNumber % 5 != 0)
                    return
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.isDead) continue
                    val traitOwner = TraitManager.instance.traitOwners[player.uniqueId] ?: continue
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) continue
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                if (player.isInLava && (action.requiredPdc == null || player.persistentDataContainer.has(action.requiredPdc))) {
                                    if (player.allowFlight && player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR)
                                        player.isFlying = true
                                    if (!player.isSneaking) {
                                        val num = player.location.y - floor(player.location.y)
                                        if (player.location.block.getRelative(BlockFace.UP).type == Material.LAVA
                                            || num + 0.1 < 0.65
                                        ) {
                                            player.teleport(player.location.add(0.0, 0.1, 0.0))
                                        } else if (0.65 - num > 0.04) {
                                            val loc = player.location.clone()
                                            loc.y = floor(loc.y) + 0.65
                                            player.teleport(loc)
                                        }
                                        player.flySpeed = action.walkSpeed
                                        player.allowFlight = true
                                    }
                                }
                                else {
                                    if (player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR) {
                                        player.isFlying = false
                                        player.allowFlight = false
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