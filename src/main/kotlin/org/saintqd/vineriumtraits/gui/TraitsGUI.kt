package org.saintqd.vineriumtraits.gui

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.gui.VinGUI
import org.saintqd.vineriumlib.gui.VinGUIButton
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder
import org.saintqd.vineriumlib.managers.LangManager
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.enums.InteractionType
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner
import org.saintqd.vineriumtraits.traits.BindableAction
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.time.Clock

class TraitsGUI(player: Player) : VinGUI(player) {

    // TODO: Возможность бинда свойств в меню просмотра имеющихся свойств

    val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]!!

    @Suppress("UnstableApiUsage")
    fun setTraitMenu(page : Int = 1) {

        val isSimpleLayout = VineriumTraits.inst().config.getBoolean("Gui.Simple",false)

        var availablePoints = VineriumTraits.inst().config.getInt("Traits.AvailablePoints",10)
        val ownerTags = hashSetOf<String>()
        val ownerLinkedTraits = hashSetOf<String>()
        for (traitName in traitOwner.traits) {
            val trait = TraitManager.instance.traits[traitName] ?: continue
            availablePoints -= trait.cost
            ownerTags.addAll(trait.tags)
            ownerLinkedTraits.addAll(trait.linkedTraitNames)
        }
        for (preselectedTraitToRemoveName in traitOwner.preselectedTraitsToRemove) {
            val trait = TraitManager.instance.traits[preselectedTraitToRemoveName] ?: continue
            ownerTags.removeAll(trait.tags)
            ownerLinkedTraits.removeAll(trait.linkedTraitNames)
        }
        for (traitName in traitOwner.preselectedTraits) {
            val trait = TraitManager.instance.traits[traitName] ?: continue
            ownerTags.addAll(trait.tags)
            ownerLinkedTraits.addAll(trait.linkedTraitNames)
        }

        val size = 54
        inventory = Bukkit.createInventory(
            VinGUIHolder(this), size,
            VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "trait_gui_title",availablePoints.toString()))
        buttons.clear()

        var fillerSlots = 0
        if (!isSimpleLayout) {
            val fillerItem = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
            fillerItem.setData(DataComponentTypes.CUSTOM_NAME, Component.empty())
            for (slot in size - 9..< size) {
                inventory.setItem(slot, fillerItem.clone())
                fillerSlots++
            }
            var rowIndex = 4
            while (rowIndex < size) {
                inventory.setItem(rowIndex, fillerItem.clone())
                rowIndex += 9
                fillerSlots++
            }
        }

        var positiveCostTraits = 0
        var negativeCostTraits = 0

        var positiveCostTraitIndex = 0
        var negativeCostTraitIndex = 0
        var slotIndex : Int

        val maxTraitsPerPage = if (isSimpleLayout) 6 else (size - fillerSlots) / 2
        var biggerAmountOfTraits = 0

        for (trait in TraitManager.instance.traits.values) {
            if (trait.permission.isNotEmpty() && !player.hasPermission(trait.permission)) continue
            if (trait.neededTags.isNotEmpty() && !ownerTags.containsAll(trait.neededTags)) continue
            if (trait.conflictingTags.isNotEmpty() && ownerTags.any { it in trait.conflictingTags }) continue
            if ((traitOwner.traits.contains(trait.name) || traitOwner.preselectedTraits.contains(trait.name))
                && !traitOwner.preselectedTraitsToRemove.contains(trait.name)) continue

            if (trait.cost <= 0) {
                positiveCostTraits++
                if (positiveCostTraits <= (page - 1) * maxTraitsPerPage) continue  // Проверки для отображения свойств только текущей страницы
                slotIndex = positiveCostTraitIndex % 4 + positiveCostTraitIndex / 4 * 9
                if (isSimpleLayout)
                    slotIndex = 10 + positiveCostTraitIndex % 2 + positiveCostTraitIndex / 2 * 9
                positiveCostTraitIndex++
            }
            else {
                negativeCostTraits++
                if (negativeCostTraits <= (page - 1) * maxTraitsPerPage) continue  // Проверки для отображения свойств только текущей страницы
                slotIndex = 5 + negativeCostTraitIndex % 4 + negativeCostTraitIndex / 4 * 9
                if (isSimpleLayout)
                    slotIndex = 15 + negativeCostTraitIndex % 2 + negativeCostTraitIndex / 2 * 9
                negativeCostTraitIndex++
            }

            biggerAmountOfTraits = max(positiveCostTraits,negativeCostTraits)
            if (biggerAmountOfTraits > page * (maxTraitsPerPage)) break

            val traitIcon = generateTraitItemStack(trait,ownerLinkedTraits)

            val loreLines = mutableListOf<Component>()
            loreLines.addAll(traitIcon.getData(DataComponentTypes.LORE)!!.lines())
            loreLines.add(Component.empty())
            loreLines.add(VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "trait_gui_select_trait"))
            traitIcon.setData(DataComponentTypes.LORE, ItemLore.lore(loreLines))

            buttons[slotIndex] = VinGUIButton().consumer { _ ->
                player.sendMessage(VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "trait_preselect",trait.displayName))
                if (traitOwner.preselectedTraitsToRemove.contains(trait.name))
                    traitOwner.preselectedTraitsToRemove.remove(trait.name)
                else
                    traitOwner.preselectedTraits.add(trait.name)
                setTraitMenu(page)
                player.openInventory(inventory)

                val soundName = VineriumTraits.inst().config.getString(
                    "Gui.Sounds.click","")!!
                if (soundName.isNotEmpty()) {
                    val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                        net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                    player.playSound(sound,player)
                }
            }

            inventory.setItem(slotIndex, traitIcon)
        }

        if (page > 1) {
            val pageItem = ItemStack.of(Material.PAPER)
            val modelName = VineriumTraits.inst().config.getString("Gui.Models.PrevPage", "paper")!!
            pageItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
            pageItem.setData(
                DataComponentTypes.CUSTOM_NAME,
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_prev_page")
            )
            pageItem.setData(DataComponentTypes.LORE,ItemLore.lore().addLine(
                        VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_prev_page_lore")
                    ).build()
            )

            val button = VinGUIButton().consumer { _: InventoryClickEvent ->
                setTraitMenu(page - 1)
                player.openInventory(inventory)

                val soundName = VineriumTraits.inst().config.getString(
                    "Gui.Sounds.click","")!!
                if (soundName.isNotEmpty()) {
                    val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                        net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                    player.playSound(sound,player)
                }
            }
            buttons[size - 6] = button
            inventory.setItem(size - 6, pageItem)
        }
        if (biggerAmountOfTraits > page * maxTraitsPerPage) {
            val pageItem = ItemStack.of(Material.PAPER)
            val modelName = VineriumTraits.inst().config.getString("Gui.Models.NextPage", "paper")!!
            pageItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
            pageItem.setData(
                DataComponentTypes.CUSTOM_NAME,
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_next_page")
            )
            pageItem.setData<ItemLore?>(
                DataComponentTypes.LORE,
                ItemLore.lore().addLine(
                        VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_next_page_lore")
                    )
                    .build()
            )

            val button = VinGUIButton().consumer { _: InventoryClickEvent ->
                setTraitMenu(page + 1)
                player.openInventory(inventory)

                val soundName = VineriumTraits.inst().config.getString(
                    "Gui.Sounds.click","")!!
                if (soundName.isNotEmpty()) {
                    val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                        net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                    player.playSound(sound,player)
                }
            }
            buttons[size - 4] = button
            inventory.setItem(size - 4, pageItem)
        }

        val closeItem = ItemStack.of(Material.PAPER)
        var modelName = VineriumTraits.inst().config.getString("Gui.Models.Close", "paper")!!
        closeItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
        closeItem.setData(DataComponentTypes.CUSTOM_NAME,
            VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_close")
        )
        closeItem.setData(DataComponentTypes.LORE,ItemLore.lore()
                .addLine(VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_close_lore"))
                .build()
        )

        val closeButton = VinGUIButton().consumer { _: InventoryClickEvent ->
            player.closeInventory()

            val soundName = VineriumTraits.inst().config.getString(
                "Gui.Sounds.click","")!!
            if (soundName.isNotEmpty()) {
                val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                    net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                player.playSound(sound,player)
            }
        }
        buttons[size - 2] = closeButton
        inventory.setItem(size - 2, closeItem)

        val reviewItem = ItemStack.of(Material.GREEN_CONCRETE)
        modelName = VineriumTraits.inst().config.getString("Gui.Models.Review", "green_concrete")!!
        if (traitOwner.preselectedTraits.isNotEmpty())
            modelName = VineriumTraits.inst().config.getString("Gui.Models.ReviewPending", "lime_concrete")!!
        reviewItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
        reviewItem.setData(DataComponentTypes.CUSTOM_NAME,
            VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_review")
        )
        val loreBuilder = ItemLore.lore()
        for (line in VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumtraits:menu_review_lore")]!!.split("<newline>")) {
            loreBuilder.addLine { VinUtils.parseString(line) }
        }
        reviewItem.setData(DataComponentTypes.LORE,loreBuilder.build())

        val reviewButton = VinGUIButton().consumer { _: InventoryClickEvent ->
            setReviewMenu()
            player.openInventory(inventory)

            val soundName = VineriumTraits.inst().config.getString(
                "Gui.Sounds.click","")!!
            if (soundName.isNotEmpty()) {
                val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                    net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                player.playSound(sound,player)
            }
        }
        buttons[size - 5] = reviewButton
        inventory.setItem(size - 5, reviewItem)
    }

    @Suppress("UnstableApiUsage")
    fun setReviewMenu(page: Int = 1) {

        val isSimpleLayout = VineriumTraits.inst().config.getBoolean("Gui.Simple",false)
        var availablePoints = VineriumTraits.inst().config.getInt("Traits.AvailablePoints",10)

        val ownerTags = hashSetOf<String>()
        val ownerLinkedTraits = hashSetOf<String>()
        for (traitName in traitOwner.traits) {
            val trait = TraitManager.instance.traits[traitName] ?: continue
            availablePoints -= trait.cost

            ownerTags.addAll(trait.tags)
            ownerLinkedTraits.addAll(trait.linkedTraitNames)
        }

        val combinedList = mutableListOf<String>()
        combinedList.addAll(traitOwner.traits)
        for (preselectedTraitToRemoveName in traitOwner.preselectedTraitsToRemove) {
            val trait = TraitManager.instance.traits[preselectedTraitToRemoveName] ?: continue
            availablePoints += trait.cost

            combinedList.removeAll(trait.linkedTraitNames)
            ownerTags.removeAll(trait.tags)
            ownerLinkedTraits.removeAll(trait.linkedTraitNames)
        }
        combinedList.addAll(traitOwner.preselectedTraits)
        for (preselectedTraitName in traitOwner.preselectedTraits) {
            val trait = TraitManager.instance.traits[preselectedTraitName] ?: continue
            availablePoints -= trait.cost

            combinedList.addAll(trait.linkedTraitNames)
            ownerLinkedTraits.addAll(trait.linkedTraitNames)
        }
        combinedList.removeIf { traitName ->
            val trait = TraitManager.instance.traits[traitName] ?: return@removeIf true
            if (trait.neededTags.isNotEmpty() && !ownerTags.containsAll(trait.neededTags) && !trait.showIfPresent) {
                return@removeIf true
            }
            else if (trait.permission.isNotEmpty() && !player.hasPermission(trait.permission) && !trait.showIfPresent) {
                return@removeIf true
            }
            else !TraitManager.instance.traits.contains(traitName) || traitOwner.preselectedTraitsToRemove.contains(traitName)
        }
        val sortedList = combinedList.map { traitName -> return@map TraitManager.instance.traits[traitName]!! }.sortedBy{ trait -> trait.cost }.reversed()

        val size = 54
        inventory = Bukkit.createInventory(
            VinGUIHolder(this), size,
            VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "trait_review_gui_title",availablePoints.toString()))
        buttons.clear()

        var fillerSlots = 0
        if (!isSimpleLayout) {
            val fillerItem = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
            fillerItem.setData(DataComponentTypes.CUSTOM_NAME, Component.empty())
            for (slot in size - 9..< size) {
                inventory.setItem(slot, fillerItem.clone())
                fillerSlots++
            }
            var rowIndex = 4
            while (rowIndex < size) {
                inventory.setItem(rowIndex, fillerItem.clone())
                rowIndex += 9
                fillerSlots++
            }
        }

        //val fillerItem = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
        //fillerItem.setData(DataComponentTypes.CUSTOM_NAME, Component.empty())
        //for (slot in size - 9..< size) {
        //    inventory.setItem(slot, fillerItem.clone())
        //}

        var slotIndex : Int

        var positiveCostTraits = 0
        var negativeCostTraits = 0

        var positiveCostTraitIndex = 0
        var negativeCostTraitIndex = 0

        val maxTraitsPerPage = if (isSimpleLayout) 6 else (size - fillerSlots) / 2
        var biggerAmountOfTraits = 0

        for (trait in sortedList) {

            if (trait.cost <= 0) {
                positiveCostTraits++
                if (positiveCostTraits <= (page - 1) * maxTraitsPerPage) continue  // Проверки для отображения свойств только текущей страницы
                slotIndex = positiveCostTraitIndex % 4 + positiveCostTraitIndex / 4 * 9
                if (isSimpleLayout)
                    slotIndex = 10 + positiveCostTraitIndex % 2 + positiveCostTraitIndex / 2 * 9
                positiveCostTraitIndex++
            }
            else {
                negativeCostTraits++
                if (negativeCostTraits <= (page - 1) * maxTraitsPerPage) continue  // Проверки для отображения свойств только текущей страницы
                slotIndex = 5 + negativeCostTraitIndex % 4 + negativeCostTraitIndex / 4 * 9
                if (isSimpleLayout)
                    slotIndex = 15 + negativeCostTraitIndex % 2 + negativeCostTraitIndex / 2 * 9
                negativeCostTraitIndex++
            }

            biggerAmountOfTraits = max(positiveCostTraits,negativeCostTraits)
            if (biggerAmountOfTraits > page * (maxTraitsPerPage)) break

            val traitIcon = generateTraitItemStack(trait, ownerLinkedTraits)

            val loreLines = mutableListOf<Component>()
            loreLines.addAll(traitIcon.getData(DataComponentTypes.LORE)!!.lines())

            val canRemoveTrait = trait.canDisable
                    && !ownerLinkedTraits.contains(trait.name)
                    && (trait.permission.isEmpty() || player.hasPermission(trait.permission))
                    && (trait.neededTags.isEmpty() || ownerTags.containsAll(trait.neededTags))

            val canBind = traitOwner.traits.contains(trait.name) && trait.action is BindableAction && trait.action.isBindable()
            val bindData = traitOwner.bindedTraits[trait.name]

            if (canBind) {
                loreLines.add(Component.empty())
                loreLines.add(
                    VineriumLib.inst().langManager.parseLangString(
                        VineriumTraits.inst(),
                        "menu_review_bind_trait_hint"
                    )
                )
                var interactionHintLine = LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "menu_review_bind_trait_interaction_hint")
                    .replace("{1}","1")
                val selectedInteractionType = bindData?.interactionType
                val interactionTypesString = mutableListOf<String>()


                for (interactionType in InteractionType.entries) {
                    if (selectedInteractionType != null && interactionType == selectedInteractionType) {
                        interactionTypesString.add(LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "menu_review_bind_trait_interaction_selected")
                            .replace("{1}","<key:${interactionType.key}>"))
                    }
                    else {
                        interactionTypesString.add(LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "menu_review_bind_trait_interaction_not_selected")
                            .replace("{1}","<key:${interactionType.key}>"))
                    }
                }
                interactionHintLine += interactionTypesString.joinToString(", ")
                loreLines.add(VinUtils.parseString(interactionHintLine))

                var sneakingLine = LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "menu_review_bind_trait_require_sneaking_hint")
                    .replace("{1}","2").replace("{2}",
                        LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "text_disabled"))
                if (bindData != null && bindData.requireSneaking) {
                    sneakingLine = LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "menu_review_bind_trait_require_sneaking_hint")
                        .replace("{1}","2").replace("{2}",
                            LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "text_enabled"))
                }
                loreLines.add(VinUtils.parseString(sneakingLine))

                var emptyHandLine = LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "menu_review_bind_trait_require_empty_hand_hint")
                    .replace("{1}","3").replace("{2}",
                        LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "text_disabled"))
                if (bindData != null && bindData.requireEmptyHand) {
                    emptyHandLine = LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "menu_review_bind_trait_require_empty_hand_hint")
                        .replace("{1}","3").replace("{2}",
                            LangManager.INSTANCE.getRawLangString(VineriumTraits.inst(), "text_enabled"))
                }
                loreLines.add(VinUtils.parseString(emptyHandLine))
                loreLines.add(LangManager.INSTANCE.parseLangString(VineriumTraits.inst(),"menu_review_bind_trait_remove"))
            }

            if (canRemoveTrait) {
                loreLines.add(Component.empty())
                loreLines.add(
                    VineriumLib.inst().langManager.parseLangString(
                        VineriumTraits.inst(),
                        "menu_review_remove_lore"
                    )
                )
            }

            traitIcon.setData(DataComponentTypes.LORE, ItemLore.lore(loreLines))

            val button = VinGUIButton().consumer { event: InventoryClickEvent ->
                if (event.click.isLeftClick && canRemoveTrait) {
                    if (traitOwner.traits.contains(trait.name))
                        traitOwner.preselectedTraitsToRemove.add(trait.name)
                    traitOwner.preselectedTraits.remove(trait.name)

                    setReviewMenu(page)
                    player.openInventory(inventory)

                    val soundName = VineriumTraits.inst().config.getString(
                        "Gui.Sounds.click","")!!
                    if (soundName.isNotEmpty()) {
                        val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                            net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                        player.playSound(sound,player)
                    }
                }
                if (event.click == ClickType.NUMBER_KEY && canBind) {
                    val clickedNumber = event.hotbarButton + 1
                    when (clickedNumber) {
                        1 -> {
                            var selectedInteractionType = InteractionType.LEFT_CLICK
                            if (bindData != null) {
                                var nextInteractionTypeIndex = bindData.interactionType.ordinal + 1
                                if (nextInteractionTypeIndex >= InteractionType.entries.size)
                                    nextInteractionTypeIndex = 0
                                selectedInteractionType = InteractionType.entries[nextInteractionTypeIndex]
                                traitOwner.bindedTraits[trait.name] = TraitOwner.TraitBindData(selectedInteractionType,bindData.requireSneaking,bindData.requireEmptyHand)
                            }
                            else {
                                traitOwner.bindedTraits[trait.name] = TraitOwner.TraitBindData(selectedInteractionType,
                                    requireSneaking = false,
                                    requireEmptyHand = false
                                )
                            }
                        }
                        2 -> {
                            if (bindData != null) {
                                traitOwner.bindedTraits[trait.name] = TraitOwner.TraitBindData(bindData.interactionType,!bindData.requireSneaking,bindData.requireEmptyHand)
                            }
                            else
                                traitOwner.bindedTraits[trait.name] = TraitOwner.TraitBindData(InteractionType.LEFT_CLICK,
                                    requireSneaking = true,
                                    requireEmptyHand = false
                                )
                        }
                        3 -> {
                            if (bindData != null) {
                                traitOwner.bindedTraits[trait.name] = TraitOwner.TraitBindData(bindData.interactionType,bindData.requireSneaking,!bindData.requireEmptyHand)
                            }
                            else
                                traitOwner.bindedTraits[trait.name] = TraitOwner.TraitBindData(InteractionType.LEFT_CLICK,
                                    requireSneaking = false,
                                    requireEmptyHand = true
                                )
                        }
                    }

                    setReviewMenu(page)
                    player.openInventory(inventory)

                    val soundName = VineriumTraits.inst().config.getString(
                        "Gui.Sounds.click","")!!
                    if (soundName.isNotEmpty()) {
                        val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                            net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                        player.playSound(sound,player)
                    }
                }
                if (event.click.isRightClick && canBind) {

                    traitOwner.bindedTraits.remove(trait.name)

                    traitOwner.player.sendMessage(LangManager.INSTANCE.parseLangString(VineriumTraits.inst(),"menu_review_bind_trait_remove_message",trait.displayName))

                    setReviewMenu(page)
                    player.openInventory(inventory)

                    val soundName = VineriumTraits.inst().config.getString(
                        "Gui.Sounds.click","")!!
                    if (soundName.isNotEmpty()) {
                        val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                            net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                        player.playSound(sound,player)
                    }
                }
            }
            buttons[slotIndex] = button

            inventory.setItem(slotIndex, traitIcon)
        }

        if (traitOwner.preselectedTraits.isNotEmpty() || traitOwner.preselectedTraitsToRemove.isNotEmpty()) {

            val traitChangeCooldown = VineriumTraits.inst().config.getLong("Traits.SecondsBetweenChange",2592000L)
            var canConfirm = true

            val loreBuilder = ItemLore.lore()

            if (!traitOwner.player.hasPermission("vineriumtraits.selectcooldownbypass")) {

                if ((traitOwner.lastTraitChangeTimestamp + traitChangeCooldown) > Clock.System.now().epochSeconds) {

                    var possibleApplyCooldown = false
                    for (traitName in traitOwner.preselectedTraitsToRemove) {
                        val trait = TraitManager.instance.traits[traitName] ?: continue
                        if (trait.applyCooldownOnSelect)
                            possibleApplyCooldown = true
                    }
                    for (traitName in traitOwner.preselectedTraits) {
                        val trait = TraitManager.instance.traits[traitName] ?: continue
                        if (trait.applyCooldownOnSelect)
                            possibleApplyCooldown = true
                    }
                    if (possibleApplyCooldown) {
                        val timeToCooldown = ((traitOwner.lastTraitChangeTimestamp + traitChangeCooldown - Clock.System.now().epochSeconds) / 86400).coerceAtLeast(1)
                        canConfirm = false
                        loreBuilder.addLine { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),"trait_gui_select_trait_cooldown",timeToCooldown.toString()) }
                    }
                }
            }
            var availablePointsFormat = "<yellow>$availablePoints"
            if (availablePoints > 0)
                availablePointsFormat = "<green>$availablePoints"
            if (availablePoints < 0)
                availablePointsFormat = "<red>$availablePoints"

            if (availablePoints < 0) {
                canConfirm = false
                loreBuilder.addLine { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),"trait_gui_select_trait_cooldown",availablePointsFormat) }
                loreBuilder.addLine { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),"trait_gui_select_trait_no_points") }
            }

            val confirmItem = ItemStack.of(Material.LIME_CONCRETE)
            val modelName = VineriumTraits.inst().config.getString("Gui.Models.ReviewConfirm", "lime_concrete")!!
            confirmItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
            confirmItem.setData(
                DataComponentTypes.CUSTOM_NAME,
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_confirm")
            )
            if (canConfirm) {
                loreBuilder.addLine { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),"trait_gui_select_trait_cooldown",availablePointsFormat) }
                for (line in VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumtraits:menu_confirm_lore_hint")]!!.split("<newline>")) {
                    loreBuilder.addLine { VinUtils.parseString(line) }
                }
                var possibleApplyCooldown = false
                for (traitName in traitOwner.preselectedTraitsToRemove) {
                    val trait = TraitManager.instance.traits[traitName] ?: continue
                    if (trait.applyCooldownOnSelect)
                        possibleApplyCooldown = true
                }
                for (traitName in traitOwner.preselectedTraits) {
                    val trait = TraitManager.instance.traits[traitName] ?: continue
                    if (trait.applyCooldownOnSelect)
                        possibleApplyCooldown = true
                }
                if (possibleApplyCooldown) {
                    for (line in VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumtraits:menu_confirm_lore")]!!.split("<newline>")) {
                        loreBuilder.addLine { VinUtils.parseString(line.replace("{1}", (traitChangeCooldown / 86400).coerceAtLeast(1).toString())) }
                    }
                }
                val button = VinGUIButton().consumer { _: InventoryClickEvent ->
                    for (traitName in traitOwner.preselectedTraitsToRemove) {
                        if (!traitOwner.traits.contains(traitName)) {
                            player.sendMessage { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),"menu_confirm_preselected_trait_to_remove_not_present") }
                            traitOwner.preselectedTraits.clear()
                            traitOwner.preselectedTraitsToRemove.clear()
                            setReviewMenu()
                            player.openInventory(inventory)
                            return@consumer
                        }
                    }
                    traitOwner.bindedTraits.keys.removeIf { bindedTraitName -> traitOwner.preselectedTraitsToRemove.contains(bindedTraitName) }
                    var applyCooldown = false
                    for (traitName in traitOwner.preselectedTraitsToRemove) {
                        val trait = TraitManager.instance.traits[traitName] ?: continue
                        traitOwner.removeTrait(trait)
                        if (trait.applyCooldownOnSelect)
                            applyCooldown = true
                    }
                    for (traitName in traitOwner.preselectedTraits) {
                        val trait = TraitManager.instance.traits[traitName] ?: continue
                        traitOwner.addTrait(trait)
                        if (trait.applyCooldownOnSelect)
                            applyCooldown = true
                    }
                    traitOwner.preselectedTraits.clear()
                    traitOwner.preselectedTraitsToRemove.clear()
                    if (applyCooldown)
                        traitOwner.lastTraitChangeTimestamp = Clock.System.now().epochSeconds
                    setReviewMenu()
                    player.openInventory(inventory)
                    player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f)
                    player.sendMessage { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),"menu_confirm_success") }

                    val soundName = VineriumTraits.inst().config.getString(
                        "Gui.Sounds.confirm","")!!
                    if (soundName.isNotEmpty()) {
                        val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                            net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                        player.playSound(sound,player)
                    }
                }
                buttons[size - 5] = button
            }
            confirmItem.setData(DataComponentTypes.LORE, loreBuilder.build())

            inventory.setItem(size - 5, confirmItem)
        }

        if (traitOwner.preselectedTraits.isNotEmpty() || traitOwner.preselectedTraitsToRemove.isNotEmpty()) {
            val reviewItem = ItemStack.of(Material.PAPER)
            val modelName = VineriumTraits.inst().config.getString("Gui.Models.Reset", "paper")!!
            reviewItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
            reviewItem.setData(
                DataComponentTypes.CUSTOM_NAME,
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_reset")
            )
            val loreBuilder = ItemLore.lore()
            for (line in VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumtraits:menu_reset_lore")]!!.split("<newline>")) {
                loreBuilder.addLine { VinUtils.parseString(line) }
            }
            reviewItem.setData(DataComponentTypes.LORE, ItemLore.lore().build())

            val button = VinGUIButton().consumer { _: InventoryClickEvent ->
                traitOwner.preselectedTraits.clear()
                traitOwner.preselectedTraitsToRemove.clear()
                setReviewMenu()
                player.openInventory(inventory)
                player.playSound(player, Sound.BLOCK_COPPER_TRAPDOOR_OPEN, 1f, 1f)
                player.sendMessage { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_reset_success") }

                val soundName = VineriumTraits.inst().config.getString(
                    "Gui.Sounds.click","")!!
                if (soundName.isNotEmpty()) {
                    val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                        net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                    player.playSound(sound,player)
                }
            }
            buttons[size - 9] = button
            inventory.setItem(size - 9, reviewItem)
        }

        if (page > 1) {
            val pageItem = ItemStack.of(Material.PAPER)
            val modelName = VineriumTraits.inst().config.getString("Gui.Models.PrevPage", "paper")!!
            pageItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
            pageItem.setData(
                DataComponentTypes.CUSTOM_NAME,
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_prev_page")
            )
            pageItem.setData(DataComponentTypes.LORE,ItemLore.lore().addLine(
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_prev_page_lore")
            ).build()
            )

            val button = VinGUIButton().consumer { _: InventoryClickEvent ->
                setReviewMenu(page - 1)
                player.openInventory(inventory)

                val soundName = VineriumTraits.inst().config.getString(
                    "Gui.Sounds.click","")!!
                if (soundName.isNotEmpty()) {
                    val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                        net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                    player.playSound(sound,player)
                }
            }
            buttons[size - 6] = button
            inventory.setItem(size - 6, pageItem)
        }
        if (biggerAmountOfTraits > page * maxTraitsPerPage) {
            val pageItem = ItemStack.of(Material.PAPER)
            val modelName = VineriumTraits.inst().config.getString("Gui.Models.NextPage", "paper")!!
            pageItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
            pageItem.setData(
                DataComponentTypes.CUSTOM_NAME,
                VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_next_page")
            )
            pageItem.setData<ItemLore?>(
                DataComponentTypes.LORE,
                ItemLore.lore().addLine(
                    VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_next_page_lore")
                )
                    .build()
            )

            val button = VinGUIButton().consumer { _: InventoryClickEvent ->
                setReviewMenu(page + 1)
                player.openInventory(inventory)

                val soundName = VineriumTraits.inst().config.getString(
                    "Gui.Sounds.click","")!!
                if (soundName.isNotEmpty()) {
                    val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                        net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                    player.playSound(sound,player)
                }
            }
            buttons[size - 4] = button
            inventory.setItem(size - 4, pageItem)
        }


        val returnItem = ItemStack.of(Material.BARRIER)
        val modelName = VineriumTraits.inst().config.getString("Gui.Models.Return", "barrier")!!
        returnItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName))
        returnItem.setData(DataComponentTypes.CUSTOM_NAME,
            VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_return")
        )

        val returnButton = VinGUIButton().consumer { _: InventoryClickEvent ->
            setTraitMenu()
            player.openInventory(inventory)

            val soundName = VineriumTraits.inst().config.getString(
                "Gui.Sounds.click","")!!
            if (soundName.isNotEmpty()) {
                val sound = net.kyori.adventure.sound.Sound.sound(Key.key(soundName),
                    net.kyori.adventure.sound.Sound.Source.UI,1f,1f)
                player.playSound(sound,player)
            }
        }
        buttons[size - 1] = returnButton
        inventory.setItem(size - 1, returnItem)
    }

    @Suppress("UnstableApiUsage")
    private fun generateTraitItemStack(trait: TraitManager.VinTrait, ownerLinkedTraits : Set<String>) : ItemStack {

        val traitIcon = ItemStack.of(Material.STONE)

        val costIdentifier = if (trait.cost > 0)
            "vineriumtraits:trait_gui_positive_cost_format"
        else if (trait.cost < 0)
            "vineriumtraits:trait_gui_negative_cost_format"
        else
            "vineriumtraits:trait_gui_neutral_cost_format"
        val absCost = trait.cost.absoluteValue.toString()
        val langLine = VineriumLib.inst().langManager.langLines[NamespacedKey.fromString(costIdentifier)]!!.replace("{1}",absCost)
        traitIcon.setData(DataComponentTypes.CUSTOM_NAME, VinUtils.parseString("${trait.displayName} $langLine"))

        val loreLines = VinUtils.parseStringList(trait.lore)
        val itemLoreBuilder = ItemLore.lore()
        if (!trait.canDisable || ownerLinkedTraits.contains(trait.name))
            itemLoreBuilder.addLine { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "menu_review_remove_deny_lore") }

        if (loreLines.isNotEmpty()) {
            itemLoreBuilder.addLine { Component.empty() }
            itemLoreBuilder.addLines(loreLines)
        }
        if (trait.model.isNotEmpty()) {
            val modelKey = Key.key(trait.model)
            traitIcon.setData(DataComponentTypes.ITEM_MODEL, modelKey)
        }
        traitIcon.setData(DataComponentTypes.LORE, itemLoreBuilder.build())


        return traitIcon
    }
}