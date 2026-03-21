package org.saintqd.vineriumtraits.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.saintqd.vineriumtraits.traits.TraitOwner;

public class TraitOwnerQuitEvent extends Event {

    public static final HandlerList HANDLERS = new HandlerList();

    private final TraitOwner traitOwner;

    public TraitOwnerQuitEvent(TraitOwner traitOwner) {
        this.traitOwner = traitOwner;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public TraitOwner getTraitOwner() {
        return traitOwner;
    }
}
