package com.mojang.rubydung.modloader;

import com.mojang.rubydung.modloader.event.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * rdLoader EventBus
 *
 * Central publish/subscribe bus. Mods register listeners here:
 *
 *   RDLoader.EVENT_BUS.register(Events.PlayerTickEvent.class, e -> {
 *       System.out.println("Player at " + e.player.x);
 *   });
 *
 * The game fires events by calling:
 *   RDLoader.EVENT_BUS.post(new Events.PlayerTickEvent(player));
 *
 * If an event is cancellable and a listener cancels it, post() returns true
 * and the game should skip its default behaviour.
 */
public class EventBus {

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, List<Consumer>> listeners = new HashMap<>();

    private boolean debugMode = false;

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * Register a listener for a specific event type.
     *
     * @param eventClass the event class to listen for
     * @param listener   the handler
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void register(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
        if (debugMode) {
            System.out.println("[rdLoader/EventBus] Registered listener for " + eventClass.getSimpleName());
        }
    }

    /**
     * Unregister all listeners for a given event type.
     */
    public <T extends Event> void unregisterAll(Class<T> eventClass) {
        listeners.remove(eventClass);
    }

    /**
     * Fire an event to all registered listeners.
     *
     * @return true if the event was cancelled (only possible for cancellable events)
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> boolean post(T event) {
        List<Consumer> handlers = listeners.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) {
            return false;
        }

        if (debugMode) {
            System.out.println("[rdLoader/EventBus] Posting " + event);
        }

        for (Consumer handler : handlers) {
            try {
                handler.accept(event);
            } catch (Throwable t) {
                System.err.println("[rdLoader/EventBus] Exception in listener for "
                        + event.getClass().getSimpleName() + ": " + t.getMessage());
                t.printStackTrace();
            }

            // Stop propagation if cancelled
            if (event.isCancellable() && event.isCancelled()) {
                if (debugMode) {
                    System.out.println("[rdLoader/EventBus] Event cancelled: " + event.getClass().getSimpleName());
                }
                return true;
            }
        }

        return event.isCancellable() && event.isCancelled();
    }

    /** How many listener types are registered. */
    public int getListenerTypeCount() {
        return listeners.size();
    }

    /** Total number of registered listeners across all event types. */
    public int getTotalListenerCount() {
        int count = 0;
        for (List<?> list : listeners.values()) count += list.size();
        return count;
    }
}
