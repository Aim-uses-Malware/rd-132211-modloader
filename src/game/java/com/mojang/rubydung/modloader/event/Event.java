package com.mojang.rubydung.modloader.event;

/**
 * Base class for all rdLoader events.
 * Events can be cancelled (if they implement ICancellable).
 */
public abstract class Event {

    private boolean cancelled = false;

    /**
     * Cancel this event, preventing default game behaviour from running.
     * Only works if isCancellable() returns true.
     */
    public final void cancel() {
        if (isCancellable()) {
            cancelled = true;
        }
    }

    public final boolean isCancelled() {
        return cancelled;
    }

    /** Override and return true to allow cancellation. */
    public boolean isCancellable() {
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (isCancellable() ? "[cancellable, cancelled=" + cancelled + "]" : "");
    }
}
