package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("hide_in_container")
class HideInContainerAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private var possibleBlocks = config.getStringList("PossibleBlocks")

    init {
        if (possibleBlocks.isEmpty()) {
            possibleBlocks.addAll(listOf("BARREL","CHEST","TRAPPED_CHEST"))
        }
    }

    data class HiddenPlayerData(
        val player : Player,
        val gameMode: GameMode,
        val lastLocation: Location,
        val hiddenUntil : Long
    )

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
        const val NAME = "hide_in_container"

        private val actionsMap = hashMapOf<String, HideInContainerAction>()

        private val hiddenPlayers = hashMapOf<Player, HiddenPlayerData>()
        private val containerBlocks = hashMapOf<Block, Player>()
        private val cooldown = HashMap<Player, Long>()

        private val listener = object : Listener {

            @EventHandler
            fun onContainerInteract(event : PlayerInteractEvent) {
                val hiddenPlayerData: HiddenPlayerData? = hiddenPlayers[event.player]
                if (event.action == Action.PHYSICAL) {
                    if (hiddenPlayerData != null) {
                        event.setCancelled(true)
                        return
                    }
                }
                if (event.action != Action.RIGHT_CLICK_BLOCK) return
                if (event.player.inventory.getItem(event.hand!!).type != Material.AIR) return
                if (hiddenPlayerData != null) {
                    removePlayer(hiddenPlayerData)
                }
                if (cooldown.getOrDefault(event.player, VinUtils.getCurrentTick()) > VinUtils.getCurrentTick()) {
                    event.setCancelled(true)
                    return
                }
                if (containerBlocks.containsKey(event.clickedBlock)) {
                    event.setCancelled(true)
                    val foundPlayer: Player? = containerBlocks[event.clickedBlock]
                    val playerData: HiddenPlayerData? = hiddenPlayers[foundPlayer]
                    if (foundPlayer != null && playerData != null && foundPlayer.isValid && foundPlayer.isOnline) {
                        removePlayer(playerData)
                        event.player.sendMessage(VineriumLib.inst().langManager.parseLangString(
                            VineriumTraits.inst(), "hide_in_containers_message",foundPlayer.name))
                        event.setCancelled(true)
                        containerBlocks.remove(event.clickedBlock)
                        return
                    } else {
                        containerBlocks.remove(event.clickedBlock)
                    }
                }
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    val action = actionsMap[traitName]?: continue
                    if (TraitManager.instance.executeAction(traitName,traitOwner)) {
                        event.clickedBlock?.let { clickedBlock ->
                            if (action.possibleBlocks.contains(clickedBlock.type.name) && event.player.isSneaking
                            ) {
                                event.setCancelled(true)
                                val clickedBlockFace = event.blockFace
                                val relativeBlock = clickedBlock.getRelative(clickedBlockFace)
                                if (relativeBlock.type != Material.AIR) return
                                var relativeBlockLocation = relativeBlock.location
                                relativeBlockLocation = relativeBlockLocation.add(0.5, 0.0, 0.5)
                                relativeBlockLocation.yaw = event.player.yaw
                                relativeBlockLocation.pitch = event.player.pitch
                                event.player.world.playSound(event.player.location, Sound.BLOCK_BEEHIVE_ENTER, 1f, 1f)
                                hiddenPlayers[event.player] = HiddenPlayerData(event.player,event.player.gameMode,event.player.location,
                                    VinUtils.getCurrentTick() + 10)
                                containerBlocks[clickedBlock] = event.player
                                event.player.gameMode = GameMode.SPECTATOR
                                event.player.teleportAsync(relativeBlockLocation)
                            }
                        }
                    }
                }
            }

            @EventHandler
            fun onPlayerMove(event: PlayerMoveEvent) {
                hiddenPlayers[event.player]?.let { playerData ->
                    if (playerData.player.isSneaking && playerData.hiddenUntil <= VinUtils.getCurrentTick()) {
                        removePlayer(playerData)
                    } else {
                        val to = event.to
                        val from = event.from
                        if (to.x() != from.x() || to.y() != from.y() || to.z() != from.z())
                            event.isCancelled = true
                    }
                }
            }

            @EventHandler
            fun onStartSpectating(event: PlayerStartSpectatingEntityEvent) {
                if (hiddenPlayers.containsKey(event.getPlayer()))
                    event.isCancelled = true
            }

            @EventHandler
            fun onSpectatorTeleport(event: PlayerTeleportEvent) {
                if (hiddenPlayers.containsKey(event.getPlayer()) && event.cause == PlayerTeleportEvent.TeleportCause.SPECTATE)
                    event.isCancelled = true
            }

            @EventHandler(priority = EventPriority.MONITOR)
            fun onContainerBreak(event: BlockBreakEvent) {
                if (event.isCancelled) return
                if (containerBlocks.containsKey(event.getBlock())) {
                    val player = containerBlocks[event.getBlock()]
                    val playerData = hiddenPlayers[player]
                    containerBlocks.remove(event.getBlock())
                    if (playerData != null)
                        removePlayer(playerData)
                }
            }

            @EventHandler(priority = EventPriority.LOWEST)
            fun onPlayerQuit(event: PlayerQuitEvent) {
                if (hiddenPlayers.containsKey(event.getPlayer())) {
                    removePlayer(hiddenPlayers[event.getPlayer()]!!)
                }
            }
        }

        private fun removePlayer(playerData: HiddenPlayerData) {
            playerData.player.gameMode = playerData.gameMode
            val newLocation = playerData.lastLocation
            newLocation.yaw = playerData.player.yaw
            newLocation.pitch = playerData.player.pitch
            playerData.player.teleport(newLocation)
            playerData.player.world.playSound(
                playerData.player.location, Sound.BLOCK_BEEHIVE_EXIT, 1f, 1f
            )
            cooldown[playerData.player] = VinUtils.getCurrentTick() + 10
            hiddenPlayers.remove(playerData.player)
            containerBlocks.entries.removeIf { entry -> entry.value == playerData.player }
        }
    }
}