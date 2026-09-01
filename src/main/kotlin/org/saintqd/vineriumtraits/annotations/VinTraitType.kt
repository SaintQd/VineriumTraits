package org.saintqd.vineriumtraits.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class VinTraitType(
    val name : String = ""
)
