package org.saintqd.vineriumtraits.traits

interface BindableAction {

    fun isBindable() : Boolean = true
    fun shouldCancelEvent() : Boolean = false
}