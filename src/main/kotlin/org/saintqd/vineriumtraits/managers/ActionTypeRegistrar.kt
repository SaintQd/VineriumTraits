package org.saintqd.vineriumtraits.managers

import io.github.classgraph.ClassGraph
import org.bukkit.configuration.ConfigurationSection
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.traits.TraitAction
import kotlin.reflect.full.primaryConstructor

class ActionTypeRegistrar {

    companion object {

        fun registerFromPackage(packageName : String) {
            val possibleClasses = mutableListOf<Class<*>>()
            ClassGraph()
                .enableAllInfo()
                .acceptPackages(packageName)
                .scan().use { scanResult ->
                    val subClasses = scanResult.getClassesWithAnnotation("org.saintqd.vineriumtraits.annotations.VinTraitType")
                    possibleClasses.addAll(subClasses.loadClasses())
                }

            val classes = mutableListOf<Class<out TraitAction>>()
            val parentClass = TraitAction::class.java
            for (possibleClass in possibleClasses) {
                if (parentClass.isAssignableFrom(possibleClass))
                    classes.add(possibleClass.asSubclass(TraitAction::class.java))
            }

            for (clazz in classes) {
                val kClass = clazz.kotlin
                for (annotation in kClass.annotations) {
                    if (annotation is VinTraitType) {
                        val actionTypeName = annotation.name
                        val constructor = kClass.primaryConstructor!!

                        TraitManager.instance.registerActionType(actionTypeName) {
                                name : String, config : ConfigurationSection -> constructor.call(name,config)
                        }
                    }
                    break
                }
            }
        }

        inline fun <reified T : TraitAction> register() {
            val clazz = T::class

            for (annotation in clazz.annotations) {
                if (annotation is VinTraitType) {
                    val actionTypeName = annotation.name
                    val constructor = clazz.primaryConstructor!!

                    TraitManager.instance.registerActionType(actionTypeName) {
                            name : String, config : ConfigurationSection -> constructor.call(name,config)
                    }
                }
                break
            }
        }
    }
}