package org.saintqd.vineriumtraits.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.key.Key
import net.kyori.adventure.util.TriState
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.gui.TraitsGUI
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.*

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
                            Commands.literal("savedata")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.admin")
                                }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    saveDataCommand(
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
                            Commands.literal("addpreselectedtrait")
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
                                    addPreselectedTraitCommand(
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
                                            addPreselectedTraitCommand(
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
                            Commands.literal("addpreselectedtraittoremove")
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
                                    addPreselectedTraitToRemoveCommand(
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
                                            addPreselectedTraitToRemoveCommand(
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
                            Commands.literal("removepreselectedtrait")
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
                                    removePreselectedTraitCommand(
                                        ctx.getSource().sender,
                                        ctx.getArgument("trait", String::class.java),
                                        null
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                                .then(
                                    Commands.argument("player", ArgumentTypes.player())
                                        .executes { ctx: CommandContext<CommandSourceStack> ->
                                            removePreselectedTraitCommand(
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
                            Commands.literal("resetpreselectedtraits")
                            .requires { predicate: CommandSourceStack ->
                                predicate.sender.hasPermission("vineriumtraits.admin")
                            }
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                resetPreselectedTraitsCommand(
                                    ctx.getSource().sender,
                                    null
                                )
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                Commands.argument("player", ArgumentTypes.player())
                                    .executes { ctx: CommandContext<CommandSourceStack> ->
                                        resetPreselectedTraitsCommand(
                                            ctx.getSource().sender,
                                            ctx.getArgument(
                                                "player",
                                                PlayerSelectorArgumentResolver::class.java
                                            ).resolve(ctx.getSource()).first()
                                        )
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                        )
                        .then(
                            Commands.literal("removedata")
                            .requires { predicate: CommandSourceStack ->
                                predicate.sender.hasPermission("vineriumtraits.admin")
                            }
                            .then(
                                Commands.argument("player_name", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        val partName = builder.remaining
                                        Bukkit.getOnlinePlayers().forEach { player ->
                                            if (player.name.lowercase().startsWith(partName.lowercase()))
                                                builder.suggest(player.name)
                                        }
                                        return@suggests builder.buildFuture()
                                    }
                                    .executes { ctx: CommandContext<CommandSourceStack> ->
                                        removeTraitOwnerDataCommand(
                                            ctx.getSource().sender,
                                            ctx.getArgument("player_name", String::class.java),
                                            false
                                        )
                                        Command.SINGLE_SUCCESS
                                    }
                                    .then(
                                        Commands.argument("force", BoolArgumentType.bool())
                                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                                removeTraitOwnerDataCommand(
                                                    ctx.getSource().sender,
                                                    ctx.lastChild.getArgument("player_name", String::class.java),
                                                    ctx.getArgument("force",Boolean::class.java)
                                                )
                                                Command.SINGLE_SUCCESS
                                            }
                                    )
                            )
                        )
                        .then(
                            Commands.literal("menu")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.menu")
                                }
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
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.reviewmenu")
                                }
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                val ctxSource = ctx.getSource()
                                openReviewMenuCommand(
                                    ctxSource.sender,
                                    null,
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
                                            .resolve(ctxSource).first(),
                                        null
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                            )
                        )
                        .then(
                            Commands.literal("playerswithtrait")
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
                                            listOnlinePlayersWithTraitCommand(
                                                ctx.getSource().sender,
                                                ctx.getArgument("trait", String::class.java)
                                            )
                                            Command.SINGLE_SUCCESS
                                        }
                                )
                        )
                        .then(
                            Commands.literal("reviewmenuother")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender is Player &&
                                            predicate.sender.hasPermission("vineriumtraits.admin")
                                }
                            .then(
                                Commands.argument("player", ArgumentTypes.player())
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    val ctxSource = ctx.getSource()
                                    openReviewMenuCommand(
                                        ctxSource.sender,
                                        ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                                            .resolve(ctxSource).first(),
                                        ctxSource.sender as Player
                                    )
                                    Command.SINGLE_SUCCESS
                                }
                            )
                        )
                        .then(
                            Commands.literal("listplayertraits")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender is Player &&
                                            predicate.sender.hasPermission("vineriumtraits.admin")
                                }
                            .then(
                                Commands.argument("player", ArgumentTypes.player())
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    val ctxSource = ctx.getSource()
                                    listPlayerTraitsCommand(
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
                                Commands.argument("player", StringArgumentType.word())
                                    .suggests { ctx, builder ->
                                        val partName = builder.remaining
                                        Bukkit.getOnlinePlayers().forEach { player ->
                                            if (player.name.lowercase().startsWith(partName.lowercase()))
                                                builder.suggest(player.name)
                                        }
                                        return@suggests builder.buildFuture()
                                    }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    val ctxSource = ctx.getSource()
                                    resetSelectCooldownCommand(
                                        ctxSource.sender,
                                        ctx.getArgument("player", String::class.java)
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
                        .then(
                            Commands.literal("use")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender is Player &&
                                    predicate.sender.hasPermission("vineriumtraits.use")
                                }
                                .then(
                                    Commands.argument("trait", StringArgumentType.greedyString())
                                        .suggests { ctx, builder ->
                                            val partName = builder.remaining
                                            TraitManager.instance.namesToTraits.keys.forEach { traitDisplayName ->
                                                TraitManager.instance.namesToTraits[traitDisplayName]?.let { traitName ->
                                                    TraitManager.instance.traits[traitName]?.let { trait ->
                                                        if (trait.executableViaCommand) {
                                                            if (trait.action.checkIfPresent) {
                                                                val player = ctx.source.sender as Player
                                                                TraitManager.instance.traitOwners[player.uniqueId]?.let { traitOwner ->
                                                                    if (!traitOwner.traits.contains(traitName))
                                                                        return@forEach
                                                                }
                                                            }
                                                            if (traitDisplayName.lowercase().startsWith(partName.lowercase()))
                                                                builder.suggest(traitDisplayName)
                                                        }
                                                    }
                                                }
                                            }
                                            return@suggests builder.buildFuture()
                                        }
                                        .executes { ctx: CommandContext<CommandSourceStack> ->
                                            useTraitActionCommand(
                                                ctx.getSource().sender,
                                                ctx.getArgument("trait", String::class.java),
                                                null
                                            )
                                            Command.SINGLE_SUCCESS
                                        }
                                )
                        )
                        .then(
                            Commands.literal("useother")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.admin")
                                }
                                .then(
                                    Commands.argument("trait", StringArgumentType.string())
                                        .suggests { _, builder ->
                                            val partName = builder.remaining
                                            TraitManager.instance.traits.keys.forEach { traitName ->
                                                TraitManager.instance.traits[traitName]?.let { trait ->
                                                    if (trait.executableViaCommand) {
                                                        if (traitName.lowercase().startsWith(partName.lowercase()))
                                                            builder.suggest(traitName)
                                                    }
                                                }
                                            }
                                            return@suggests builder.buildFuture()
                                        }
                                        .then(
                                            Commands.argument("player", ArgumentTypes.player())
                                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                                    useTraitActionCommand(
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
                            Commands.literal("toggleinteract")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.toggleinteract")
                                }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    toggleInteractCommand(ctx.getSource().sender,null,null)
                                    Command.SINGLE_SUCCESS
                                }
                                .then(Commands.argument("state",StringArgumentType.word())
                                    .suggests { _, builder ->
                                        builder.suggest("true")
                                        builder.suggest("false")
                                        return@suggests builder.buildFuture()
                                    }
                                    .executes { ctx: CommandContext<CommandSourceStack> ->
                                        toggleInteractCommand(ctx.getSource().sender,ctx.getArgument("state", String::class.java),null)
                                        Command.SINGLE_SUCCESS
                                    }
                                    .then(
                                        Commands.argument("player", ArgumentTypes.player())
                                            .requires { predicate: CommandSourceStack ->
                                                predicate.sender.hasPermission("vineriumtraits.admin")
                                            }
                                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                                toggleInteractCommand(
                                                    ctx.getSource().sender,
                                                    ctx.lastChild.getArgument("state", String::class.java),
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
                            Commands.literal("togglebindhint")
                                .requires { predicate: CommandSourceStack ->
                                    predicate.sender.hasPermission("vineriumtraits.togglebindhint")
                                }
                                .executes { ctx: CommandContext<CommandSourceStack> ->
                                    toggleBindHintCommand(ctx.getSource().sender,null,null)
                                    Command.SINGLE_SUCCESS
                                }
                                .then(Commands.argument("state",StringArgumentType.word())
                                    .suggests { _, builder ->
                                        builder.suggest("true")
                                        builder.suggest("false")
                                        return@suggests builder.buildFuture()
                                    }
                                    .executes { ctx: CommandContext<CommandSourceStack> ->
                                        toggleBindHintCommand(ctx.getSource().sender,ctx.getArgument("state", String::class.java),null)
                                        Command.SINGLE_SUCCESS
                                    }
                                    .then(
                                        Commands.argument("player", ArgumentTypes.player())
                                            .requires { predicate: CommandSourceStack ->
                                                predicate.sender.hasPermission("vineriumtraits.admin")
                                            }
                                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                                toggleBindHintCommand(
                                                    ctx.getSource().sender,
                                                    ctx.lastChild.getArgument("state", String::class.java),
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

        private fun saveDataCommand(sender: CommandSender) {
            VineriumTraits.inst().saveData()
            if (sender is Player) sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "save_data_message")
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
            if (traitOwner.traits.contains(trait.name)) {
                sender.sendMessage(VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),
                    "command_add_trait_already_present",traitName,player.name))
                return
            }
            traitOwner.addTrait(trait)
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_add_trait_success",traitName,player.name))
            return
        }

        private fun addPreselectedTraitCommand(sender: CommandSender, traitName : String, player: Player?) {
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
            if (traitOwner.preselectedTraitsToRemove.contains(trait.name))
                traitOwner.preselectedTraitsToRemove.remove(trait.name)
            else
                traitOwner.preselectedTraits.add(trait.name)
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_add_preselected_trait_success",traitName,player.name))
            return
        }

        private fun addPreselectedTraitToRemoveCommand(sender: CommandSender, traitName : String, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]
            if (traitOwner == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_owner_not_exist",player.name))
                return
            }

            if (traitName == "ALL") {
                val ownerTraits = hashSetOf<String>()
                ownerTraits.addAll(traitOwner.traits)
                for (traitName in traitOwner.traits) {
                    val trait = TraitManager.instance.traits[traitName] ?: continue
                    ownerTraits.removeAll(trait.linkedTraitNames)
                }
                for (traitName in ownerTraits) {
                    if (traitOwner.traits.contains(traitName))
                        traitOwner.preselectedTraitsToRemove.add(traitName)
                    traitOwner.preselectedTraits.remove(traitName)
                }
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_add_preselected_trait_to_remove_success",traitName,player.name))
                return
            }
            else {
                val trait = TraitManager.instance.traits[traitName]
                if (trait == null) {
                    sender.sendMessage(
                        VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_does_not_exist",traitName)
                    )
                    return
                }
                if (traitOwner.traits.contains(trait.name))
                    traitOwner.preselectedTraitsToRemove.add(trait.name)
                traitOwner.preselectedTraits.remove(trait.name)

                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_add_preselected_trait_to_remove_success",traitName,player.name))
                return
            }
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
            if (!traitOwner.traits.contains(trait.name)) {
                sender.sendMessage(VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),
                    "command_remove_trait_not_present",traitName,player.name))
                return
            }
            traitOwner.removeTrait(trait)
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_remove_trait_success",traitName,player.name))
            return
        }

        private fun removePreselectedTraitCommand(sender: CommandSender, traitName : String, player: Player?) {
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
            if (traitOwner.traits.contains(trait.name))
                traitOwner.preselectedTraitsToRemove.add(trait.name)
            traitOwner.preselectedTraits.remove(trait.name)
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_remove_preselected_trait_success",traitName,player.name))
            return
        }

        private fun resetPreselectedTraitsCommand(sender: CommandSender, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)


            val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]
            if (traitOwner == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_owner_not_exist",player.name))
                return
            }
            traitOwner.preselectedTraits.clear()
            traitOwner.preselectedTraitsToRemove.clear()
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_reset_preselected_traits_success",player.name))
            return
        }

        private fun openMenuCommand(sender: CommandSender, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            val traitsGui = TraitsGUI(player)
            traitsGui.setTraitMenu()
            player.openInventory(traitsGui.inventory)

            val soundName = VineriumTraits.inst().config.getString(
                "Gui.Sounds.open","")!!
            if (soundName.isNotEmpty()) {
                val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                    net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                player.playSound(sound,player)
            }

            return
        }

        private fun openReviewMenuCommand(sender: CommandSender, player: Player?, viewer: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            val traitsGui = TraitsGUI(player)
            traitsGui.setReviewMenu()
            if (viewer != null) {
                viewer.openInventory(traitsGui.inventory)
            }
            else
                player.openInventory(traitsGui.inventory)

            val soundName = VineriumTraits.inst().config.getString(
                "Gui.Sounds.open","")!!
            if (soundName.isNotEmpty()) {
                val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                    net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                sender.playSound(sound,player)
            }

            return
        }

        private fun listPlayerTraitsCommand(sender: CommandSender, player: Player) {

            val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]
            if (traitOwner == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_owner_not_exist",player.name))
                return
            }

            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "list_player_traits_title",player.name))
            traitOwner.traits.forEachIndexed { index, traitName ->
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),
                        "list_player_traits_format",index.toString(),traitName))
            }

            return
        }

        private fun listOnlinePlayersWithTraitCommand(sender: CommandSender, traitName : String) {
            val trait = TraitManager.instance.traits[traitName]
            if (trait == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_does_not_exist",traitName)
                )
                return
            }
            sender.sendMessage(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "list_online_players_with_trait_title",traitName))
            TraitManager.instance.traitOwners.values.filter { traitOwner -> traitOwner.traits.contains(traitName) }.forEachIndexed { index, traitOwner ->
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),
                        "list_online_players_with_trait_format",index.toString(),traitOwner.player.name))
            }
        }

        private fun resetSelectCooldownCommand(sender: CommandSender, playerName: String?) {

            if (sender !is Player && playerName == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),
                        "command_only_by_player"))
                return
            }

            var uuid : UUID? = null

            if (sender is Player) {
                uuid = sender.uniqueId
            }
            if (playerName != null) {
                val possiblePlayer = Bukkit.getOfflinePlayer(playerName)
                if (!possiblePlayer.hasPlayedBefore()) {
                    sender.sendMessage(
                        VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),
                            "command_reset_select_offline_player_not_found",playerName))
                    return
                }
                uuid = possiblePlayer.uniqueId
            }
            if (uuid != null) {

                val traitOwner = TraitManager.instance.traitOwners[uuid]

                if (traitOwner != null) {
                    traitOwner.lastTraitChangeTimestamp = 0L
                    sender.sendMessage(
                        VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_reset_select_success",traitOwner.player.name))
                }
                else {
                    VineriumTraits.inst().storage?.resetTraitOwnerSelectCooldown(uuid)
                    sender.sendMessage(
                        VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_reset_select_success",uuid.toString()))
                }
            }

            return
        }

        private fun removeTraitOwnerDataCommand(sender: CommandSender, playerName: String, force : Boolean) {

            val possiblePlayer = Bukkit.getOfflinePlayer(playerName)
            if (!possiblePlayer.hasPlayedBefore()) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),
                        "command_reset_select_offline_player_not_found",playerName))
                return
            }
            val uuid = possiblePlayer.uniqueId

            val traitOwner = TraitManager.instance.traitOwners[uuid]

            if (traitOwner != null) {
                val traitOwnerTraits = traitOwner.traits
                val linkedTraits = hashSetOf<String>()
                for (traitName in traitOwnerTraits) {
                    TraitManager.instance.traits[traitName]?.let { trait ->
                        linkedTraits.addAll(trait.linkedTraitNames)
                    }
                }
                traitOwnerTraits.removeAll(linkedTraits)
                for (traitName in traitOwnerTraits) {
                    TraitManager.instance.traits[traitName]?.let { trait ->
                        traitOwner.removeTrait(trait)
                    }
                }
                traitOwner.lastTraitChangeTimestamp = 0L
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_remove_data_success",traitOwner.player.name))
            }
            else {
                VineriumTraits.inst().storage?.removeTraitOwnerData(uuid,force)
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_remove_data_success",uuid.toString()))
            }

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

        private fun useTraitActionCommand(sender: CommandSender, traitName : String, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)

            var trait = TraitManager.instance.traits[traitName]
            if (trait == null) {
                TraitManager.instance.namesToTraits[traitName]?.let { traitDisplayName ->
                    trait = TraitManager.instance.traits[traitDisplayName]
                }
            }
            if (trait == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_does_not_exist",traitName)
                )
                return
            }
            if (!trait.executableViaCommand) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_use_trait_not_bindable",traitName)
                )
                return
            }
            val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]
            if (traitOwner == null) {
                sender.sendMessage(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "command_trait_owner_not_exist",player.name))
                return
            }
            TraitManager.instance.executeAction(trait.name,traitOwner)
            return
        }

        private fun toggleInteractCommand(sender: CommandSender, state: String?, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)
            val vaultManager = VineriumLib.inst().vaultManager

            val boolState: Boolean
            if (state == null) {
                val triState = player.permissionValue("vineriumtraits.interactdisabled")
                boolState = triState != TriState.TRUE
            } else boolState = state.toBoolean()

            if (boolState) {
                vaultManager.permissionProvider.playerAdd(null, player, "vineriumtraits.interactdisabled")
                if (sender === player) sender.sendMessage(
                    VineriumLib.inst().langManager
                        .parseLangString(VineriumTraits.inst(), "command_toggle_interact_false")
                )
                else sender.sendMessage(
                    VineriumLib.inst().langManager
                        .parseLangString(VineriumTraits.inst(), "command_toggle_interact_false_other", player.name)
                )
            } else {
                vaultManager.permissionProvider.playerRemove(null, player, "vineriumtraits.interactdisabled")
                if (sender === player) sender.sendMessage(
                    VineriumLib.inst().langManager
                        .parseLangString(VineriumTraits.inst(), "command_toggle_interact_true")
                )
                else sender.sendMessage(
                    VineriumLib.inst().langManager
                        .parseLangString(VineriumTraits.inst(), "command_toggle_interact_true_other", player.name)
                )
            }
        }

        private fun toggleBindHintCommand(sender: CommandSender, state: String?, player: Player?) {
            var player = player
            player = VinUtils.checkForPlayerPresent(sender, player)
            val vaultManager = VineriumLib.inst().vaultManager

            val boolState: Boolean
            if (state == null) {
                val triState = player.permissionValue("vineriumtraits.bindhintdisabled")
                boolState = triState != TriState.TRUE
            } else boolState = state.toBoolean()

            if (boolState) {
                vaultManager.permissionProvider.playerAdd(null, player, "vineriumtraits.bindhintdisabled")
                if (sender === player) sender.sendMessage(
                    VineriumLib.inst().langManager
                        .parseLangString(VineriumTraits.inst(), "command_binded_traits_toggle_disable")
                )
                else sender.sendMessage(
                    VineriumLib.inst().langManager
                        .parseLangString(VineriumTraits.inst(), "command_binded_traits_toggle_disable_other", player.name)
                )
            } else {
                vaultManager.permissionProvider.playerRemove(null, player, "vineriumtraits.bindhintdisabled")
                if (sender === player) sender.sendMessage(
                    VineriumLib.inst().langManager
                        .parseLangString(VineriumTraits.inst(), "command_binded_traits_toggle_enable")
                )
                else sender.sendMessage(
                    VineriumLib.inst().langManager
                        .parseLangString(VineriumTraits.inst(), "command_binded_traits_toggle_enable_other", player.name)
                )
            }
        }
    }
}