package org.saintqd.vineriumtraits.listeners

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ISkillMechanic
import io.lumine.mythic.api.skills.conditions.ISkillCondition
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.core.skills.SkillExecutor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.mythicmobs.conditions.VinHasTraitCondition
import org.saintqd.vineriumtraits.mythicmobs.mechanics.VinAddTraitMechanic
import org.saintqd.vineriumtraits.mythicmobs.mechanics.VinRemoveTraitMechanic
import java.io.File

class MythicMobsListener : Listener {

    private val mechanics = hashMapOf<String,(SkillExecutor, File, String, MythicLineConfig) -> ISkillMechanic>()
    private val conditions = hashMapOf<String,(String, MythicLineConfig) -> ISkillCondition>()

    fun registerMechanics() {
        mechanics["vinaddtrait"] = { manager : SkillExecutor, file : File, line : String, config : MythicLineConfig -> VinAddTraitMechanic(manager,file,line,config) }
        mechanics["vinremovetrait"] = { manager : SkillExecutor, file : File, line : String, config : MythicLineConfig -> VinRemoveTraitMechanic(manager,file,line,config) }
    }

    fun registerConditions() {
        conditions["vinhastrait"] = { line: String, mlc : MythicLineConfig -> VinHasTraitCondition(line,mlc) }
    }

    @EventHandler
    fun onMythicMechanicLoad(event: MythicMechanicLoadEvent) {
        val file = event.container.file
        val configLine = event.container.config.line
        val manager = event.container.manager
        mechanics[event.mechanicName.lowercase()]?.let { mechanic ->
            event.register(mechanic.invoke(manager, file, configLine,event.config))
        }
    }

    @EventHandler
    fun onMythicConditionLoad(event: MythicConditionLoadEvent) {
        conditions[event.conditionName.lowercase()]?.let { condition ->
            event.register(condition.invoke(event.argument,event.config))
        }
    }
}