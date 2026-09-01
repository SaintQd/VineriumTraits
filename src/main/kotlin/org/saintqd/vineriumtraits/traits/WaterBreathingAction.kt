package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

@VinTraitType("water_breathing")
class WaterBreathingAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val damage = config.getDouble("Damage",2.0)

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

        val WATER_BREATHING_ITEM_KEY = NamespacedKey(VineriumTraits.inst(),"water_breathing_item")

        val deathByDrowningSet = hashSetOf<Player>()
        val lastAirAmountPerPlayer = hashMapOf<Player, Int>()

        private fun hasWaterBreathing(player : Player): Boolean {
            return player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) || player.hasPotionEffect(PotionEffectType.WATER_BREATHING)
        }

        private fun decreaseAir(player : Player) {
            var respirationLevel = 0
            val helmet = player.inventory.helmet
            if (helmet != null && helmet.itemMeta != null) {
                if (helmet.persistentDataContainer.has(WATER_BREATHING_ITEM_KEY)) {
                    if (ThreadLocalRandom.current().nextInt(0,4) <= 0)
                        helmet.damage(1,player)
                    return
                }
                if ((helmet.itemMeta.getEnchantLevel(Enchantment.RESPIRATION).also { respirationLevel = it }) > 0
                        && Random.nextInt(respirationLevel + 1) > 0)
                return
            }
            lastAirAmountPerPlayer[player] = (player.remainingAir - 5).coerceAtLeast(-5)
            player.remainingAir = lastAirAmountPerPlayer[player]!!
        }

        private val actionsMap = hashMapOf<String, WaterBreathingAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onAirChange(event : EntityAirChangeEvent) {
                if (event.entity !is Player) return
                val player = event.entity as Player
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                            lastAirAmountPerPlayer[player]?.let { lastAmount ->
                                if (player.isUnderWater || hasWaterBreathing(player) || player.isInRain) {
                                    if (event.amount < lastAmount) {
                                        event.isCancelled = true
                                    }
                                }
                                else if (event.amount > lastAmount)
                                    event.isCancelled = true
                            }
                        }
                    }
                }
            }

            @EventHandler
            fun onServerTickEnd(event : ServerTickEndEvent) {
                if (event.tickNumber % 5 != 0) return
                for (player in Bukkit.getOnlinePlayers()) {
                    val traitOwner = TraitManager.instance.traitOwners[player.uniqueId] ?: continue
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) continue
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                if (!player.isUnderWater && !hasWaterBreathing(player) && !player.isInRain) {
                                    decreaseAir(player)
                                    if (player.remainingAir <= 0) {
                                        if (player.health <= action.damage)
                                            deathByDrowningSet.add(player)
                                        if (event.tickNumber % 20 == 0) {
                                            player.damage(action.damage, DamageSource.builder(DamageType.DROWN).build())
                                        }
                                    }
                                }
                                else {
                                    lastAirAmountPerPlayer[player] = player.remainingAir + 5
                                    player.remainingAir = (player.remainingAir + 5).coerceAtMost(player.maximumAir)
                                }
                            }
                        }
                    }
                }
            }

            @EventHandler(priority = EventPriority.LOW)
            fun onPlayerDeath(event : PlayerDeathEvent) {
                if (deathByDrowningSet.contains(event.player)) {
                    event.deathMessage(
                        VineriumLib.inst().langManager.parseLangString(
                            VineriumTraits.inst(),
                            "water_breathing_death_message",
                            event.player.name
                        )
                    )
                    deathByDrowningSet.remove(event.player)
                }
            }
        }
    }
}