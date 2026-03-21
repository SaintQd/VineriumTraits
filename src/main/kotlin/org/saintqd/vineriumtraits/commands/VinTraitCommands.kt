package org.saintqd.vineriumtraits.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.gui.TraitsGUI
import org.saintqd.vineriumtraits.managers.TraitManager

class VinTraitCommands {

    companion object {

        fun setupCommands(plugin : VineriumTraits) {
            val manager = plugin.lifecycleManager
            manager.registerEventHandler(LifecycleEvents.COMMANDS) {
                val commands: Commands = it.registrar()
                commands.register(
                    Commands.literal("vintraits")
                        .executes { commandContext: CommandContext<CommandSourceStack> ->
                            commandContext.getSource().sender.sendMessage(
                                VineriumLib.inst().langManager.parseLangString(
                                    VineriumTraits.inst(),
                                    "not_enough_arguments"
                                )
                            )
                            Command.SINGLE_SUCCESS
                        }
                        .then(
                            Commands.literal("reload")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.admin")
                                }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    reloadCommand(
                                        ctx.getSource().sender
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                        )
                        .then(
                            Commands.literal("addtrait")
                            .requires { predicate: CommandSourceStack ->
                                predicate.sender.hasPermission("vineriumtraits.admin")
                            }
                            .then(
                                Commands.argument("trait", StringArgumentType.word())
                                .suggests { _, builder ->
                                    val partName = builder.remaining
                                    TraitManager.instance.traits.keys.forEach { traitName ->
                                        if (traitName.lowercase().startsWith(partName.lowercase()))
                                            builder.suggest(traitName)
                                    }
                                    return@suggests builder.buildFuture()
                                }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    addTraitCommand(
                                        ctx.getSource().sender,
                                        ctx.getArgument("trait", String::class.java),
                                        null
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                                .then(
                                    Commands.argument("player", ArgumentTypes.player())
                                        .executes { ctx: CommandContext<CommandSourceStack> ->
                                            val ctxSource = ctx.getSource()
                                            addTraitCommand(
                                                ctxSource.sender,
                                                ctx.lastChild.getArgument("trait", String::class.java),
                                                ctx.getArgument(
                                                    "player",
                                                    PlayerSelectorArgumentResolver::class.java
                                                ).resolve(ctxSource).first()
                                            )
                                            Command.SINGLE_SUCCESS
                                        }
                                )
                            )
                        )
                        .then(
                            Commands.literal("removetrait")
                            .requires { predicate: CommandSourceStack ->
                                predicate.sender.hasPermission("vineriumtraits.admin")
                            }
                            .then(
                                Commands.argument("trait", StringArgumentType.word())
                                .suggests { _, builder ->
                                    val partName = builder.remaining
                                    TraitManager.instance.traits.keys.forEach { traitName ->
                                        if (traitName.lowercase().startsWith(partName.lowercase()))
                                            builder.suggest(traitName)
                                    }
                                    return@suggests builder.buildFuture()
                                }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    removeTraitCommand(
                                        ctx.getSource().sender,
                                        ctx.getArgument("trait", String::class.java),
                                        null
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                                .then(
                                    Commands.argument("player", ArgumentTypes.player())
                                        .executes { ctx: CommandContext<CommandSourceStack> ->
                                            removeTraitCommand(
                                                ctx.getSource().sender,
                                                ctx.lastChild.getArgument("trait", String::class.java),
                                                ctx.getArgument(
                                                    "player",
                                                    PlayerSelectorArgumentResolver::class.java
                                                ).resolve(ctx.getSource()).first()
                                            )
                                            Command.SINGLE_SUCCESS
                                        }
                                )
                            )
                        )
                        .then(
                            Commands.literal("menu")
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                val ctxSource = ctx.getSource()
                                openMenuCommand(
                                    ctxSource.sender,
                                    null
                                )
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                Commands.argument("player", ArgumentTypes.player())
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.admin")
                                }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    val ctxSource = ctx.getSource()
                                    openMenuCommand(
                                        ctxSource.sender,
                                        ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                                            .resolve(ctxSource).first()
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                            )
                        )
                        .then(
                            Commands.literal("reviewmenu")
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                val ctxSource = ctx.getSource()
                                openReviewMenuCommand(
                                    ctxSource.sender,
                                    null
                                )
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                Commands.argument("player", ArgumentTypes.player())
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.admin")
                                }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    val ctxSource = ctx.getSource()
                                    openReviewMenuCommand(
                                        ctxSource.sender,
                                        ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                                            .resolve(ctxSource).first()
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                            )
                        )
                        .then(Commands.literal("resetselectcooldown")
                            .requires { predicate: CommandSourceStack ->
                                predicate.sender.hasPermission("vineriumtraits.admin")
                            }
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                val ctxSource = ctx.getSource()
                                resetSelectCooldownCommand(
                                    ctxSource.sender,
                                    null
                                )
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                Commands.argument("player", ArgumentTypes.player())
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    val ctxSource = ctx.getSource()
                                    resetSelectCooldownCommand(
                                        ctxSource.sender,
                                        ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                                            .resolve(ctxSource).first()
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                            )
                        )
                        .then(Commands.literal("resetactioncooldowns")
                            .requires { predicate: CommandSourceStack ->
                                predicate.sender.hasPermission("vineriumtraits.admin")
                            }
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                val ctxSource = ctx.getSource()
                                resetActionCooldownsCommand(
                                    ctxSource.sender,
                                    null
                                )
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                Commands.argument("player", ArgumentTypes.player())
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    val ctxSource = ctx.getSource()
                                    resetActionCooldownsCommand(
                                        ctxSource.sender,
                                        ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                                            .resolve(ctxSource).first()
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                            )
                        )
                        .build(),
                    "Основная команда."
                )
            }
        }

        private fun reloadCommand(sender: CommandSender) {
            VineriumTraits.inst().loadData()
            if (sender is Player) sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "reload_message")
            )
        }

        private fun addTraitCommand(sender: CommandSender, traitName : String, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            val trait = TraitManager.instance.traits[traitName]
            if (trait == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_does_not_exist",traitName)
                )
                return
            }
            val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]
            if (traitOwner == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_owner_not_exist",player.name))
                return
            }
            traitOwner.addTrait(trait)
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_add_trait_success",traitName,player.name))
            return
        }

        private fun removeTraitCommand(sender: CommandSender, traitName : String, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)


            val trait = TraitManager.instance.traits[traitName]
            if (trait == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_does_not_exist",traitName)
                )
                return
            }
            val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]
            if (traitOwner == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_owner_not_exist",player.name))
                return
            }
            traitOwner.removeTrait(trait)
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_remove_trait_success",traitName,player.name))
            return
        }

        private fun openMenuCommand(sender: CommandSender, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            val traitsGui = TraitsGUI(player)
            traitsGui.setTraitMenu()
            player.openInventory(traitsGui.inventory)

            return
        }

        private fun openReviewMenuCommand(sender: CommandSender, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            val traitsGui = TraitsGUI(player)
            traitsGui.setReviewMenu()
            player.openInventory(traitsGui.inventory)

            return
        }

        private fun resetSelectCooldownCommand(sender: CommandSender, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]
            if (traitOwner == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_owner_not_exist",player.name))
                return
            }
            traitOwner.lastTraitChangeTimestamp = 0L
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_reset_select_success",player.name))

            return
        }

        private fun resetActionCooldownsCommand(sender: CommandSender, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]
            if (traitOwner == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_owner_not_exist",player.name))
                return
            }
            traitOwner.cooldowns.clear()
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_reset_action_success",player.name))

            return
        }
    }
}