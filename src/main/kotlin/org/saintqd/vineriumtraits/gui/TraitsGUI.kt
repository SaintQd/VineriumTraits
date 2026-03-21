package org.saintqd.vineriumtraits.gui

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.gui.VinGUI
import org.saintqd.vineriumlib.gui.VinGUIButton
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.time.Clock

class TraitsGUI(player: Player) : VinGUI(player) {

    val traitOwner = TraitManager.instance.traitOwners[player.uniqueId]!!

    @Suppress("UnstableApiUsage")
    fun setTraitMenu(page : Int = 1) {

        var availablePoints = VineriumTraits.inst().config.getInt("Traits.AvailablePoints",10)
        for (traitName in traitOwner.traits) {
            val trait = TraitManager.instance.traits[traitName] ?: continue
            availablePoints -= trait.cost
        }

        val size = 54
        inventory = Bukkit.createInventory(
            VinGUIHolder(this), size,
            VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "trait_gui_title",availablePoints.toString()))
        buttons.clear()

        val fillerItem = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
        fillerItem.setData(DataComponentTypes.CUSTOM_NAME, Component.empty())
        var fillerSlots = 0
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

        var positiveCostTraits = 0
        var negativeCostTraits = 0

        var positiveCostTraitIndex = 0
        var negativeCostTraitIndex = 0
        var slotIndex : Int

        val maxTraitsPerPage = (size - fillerSlots) / 2
        var biggerAmountOfTraits = 0

        for (trait in TraitManager.instance.traits.values) {
            if (trait.permission.isNotEmpty() && !player.hasPermission(trait.permission)) continue
            if ((traitOwner.traits.contains(trait.name) || traitOwner.preselectedTraits.contains(trait.name))
                && !traitOwner.preselectedTraitsToRemove.contains(trait.name)) continue

            if (trait.cost >= 0) {
                positiveCostTraits++
                if (positiveCostTraits <= (page - 1) * maxTraitsPerPage) continue  // Проверки для отображения свойств только текущей страницы
                slotIndex = positiveCostTraitIndex % 4 + positiveCostTraitIndex / 4 * 9
                positiveCostTraitIndex++
            }
            else {
                negativeCostTraits++
                if (negativeCostTraits <= (page - 1) * maxTraitsPerPage) continue  // Проверки для отображения свойств только текущей страницы
                slotIndex = 5 + negativeCostTraitIndex % 4 + negativeCostTraitIndex / 4 * 9
                negativeCostTraitIndex++
            }

            biggerAmountOfTraits = max(positiveCostTraits,negativeCostTraits)
            if (biggerAmountOfTraits > page * (maxTraitsPerPage)) break

            val traitIcon = generateTraitItemStack(trait)

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
        }
        buttons[size - 2] = closeButton
        inventory.setItem(size - 2, closeItem)

        val reviewItem = ItemStack.of(Material.GREEN_CONCRETE)
        modelName = VineriumTraits.inst().config.getString("Gui.Models.Review", "green_concrete")!!
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
        }
        buttons[size - 5] = reviewButton
        inventory.setItem(size - 5, reviewItem)
    }

    @Suppress("UnstableApiUsage")
    fun setReviewMenu() {

        val combinedList = mutableListOf<String>()
        combinedList.addAll(traitOwner.traits)
        combinedList.addAll(traitOwner.preselectedTraits)
        combinedList.removeIf { traitName -> !TraitManager.instance.traits.contains(traitName) || traitOwner.preselectedTraitsToRemove.contains(traitName) }
        val sortedList = combinedList.map { traitName -> return@map TraitManager.instance.traits[traitName]!! }.sortedBy{ trait -> trait.cost }.reversed()

        var availablePoints = VineriumTraits.inst().config.getInt("Traits.AvailablePoints",10)
        for (trait in sortedList) {
            availablePoints -= trait.cost
        }

        val size = (sortedList.size / 9 + 2) * 9
        inventory = Bukkit.createInventory(
            VinGUIHolder(this), size,
            VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(), "trait_review_gui_title",availablePoints.toString()))
        buttons.clear()

        val fillerItem = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
        fillerItem.setData(DataComponentTypes.CUSTOM_NAME, Component.empty())
        for (slot in size - 9..< size) {
            inventory.setItem(slot, fillerItem.clone())
        }

        var slotIndex = 0
        for (trait in sortedList) {
            val traitIcon = generateTraitItemStack(trait)

            if (!trait.canDisable) {
                val loreLines = mutableListOf<Component>()
                loreLines.addAll(traitIcon.getData(DataComponentTypes.LORE)!!.lines())
                loreLines.add(Component.empty())
                loreLines.add(
                    VineriumLib.inst().langManager.parseLangString(
                        VineriumTraits.inst(),
                        "menu_review_remove_lore"
                    )
                )
                traitIcon.setData(DataComponentTypes.LORE, ItemLore.lore(loreLines))

                val button = VinGUIButton().consumer { _: InventoryClickEvent ->
                    if (traitOwner.traits.contains(trait.name))
                        traitOwner.preselectedTraitsToRemove.add(trait.name)
                    traitOwner.preselectedTraits.remove(trait.name)
                    setReviewMenu()
                    player.openInventory(inventory)
                }
                buttons[slotIndex] = button
            }

            inventory.setItem(slotIndex++, traitIcon)
        }

        if (traitOwner.preselectedTraits.isNotEmpty() || traitOwner.preselectedTraitsToRemove.isNotEmpty()) {

            val traitChangeCooldown = VineriumTraits.inst().config.getLong("Traits.SecondsBetweenChange",62208000L)
            var canConfirm = true

            val loreBuilder = ItemLore.lore()

            if ((traitOwner.lastTraitChangeTimestamp + traitChangeCooldown) > Clock.System.now().epochSeconds) {
                val timeToCooldown = ((traitOwner.lastTraitChangeTimestamp + traitChangeCooldown - Clock.System.now().epochSeconds) / 24 / 86400).coerceAtLeast(1)
                canConfirm = false
                loreBuilder.addLine { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),"trait_gui_select_trait_cooldown",timeToCooldown.toString()) }
            }
            if (availablePoints < 0) {
                canConfirm = false
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
                for (line in VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumtraits:menu_confirm_lore")]!!.split("<newline>")) {
                    loreBuilder.addLine { VinUtils.parseString(line) }
                }
                val button = VinGUIButton().consumer { _: InventoryClickEvent ->
                    for (traitName in traitOwner.preselectedTraitsToRemove) {
                        val trait = TraitManager.instance.traits[traitName] ?: continue
                        traitOwner.removeTrait(trait)
                    }
                    for (traitName in traitOwner.preselectedTraits) {
                        val trait = TraitManager.instance.traits[traitName] ?: continue
                        traitOwner.addTrait(trait)
                    }
                    traitOwner.preselectedTraits.clear()
                    traitOwner.preselectedTraitsToRemove.clear()
                    traitOwner.lastTraitChangeTimestamp = Clock.System.now().epochSeconds
                    setReviewMenu()
                    player.openInventory(inventory)
                    player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f)
                    player.sendMessage { VineriumLib.inst().langManager.parseLangString(VineriumTraits.inst(),"menu_confirm_success") }
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
            }
            buttons[size - 7] = button
            inventory.setItem(size - 7, reviewItem)
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
        }
        buttons[size - 3] = returnButton
        inventory.setItem(size - 3, returnItem)
    }

    @Suppress("UnstableApiUsage")
    private fun generateTraitItemStack(trait: TraitManager.VinTrait) : ItemStack {

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
        if (!trait.canDisable)
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