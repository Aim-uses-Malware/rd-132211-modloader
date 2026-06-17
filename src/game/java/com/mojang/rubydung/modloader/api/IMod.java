package com.mojang.rubydung.modloader.api;

/**
 * rdLoader - Mod interface for rd-132211
 *
 * Every mod must implement this interface and be annotated with @RDMod.
 * The mod class must have a no-arg constructor.
 *
 * Lifecycle:
 *   preInit  -> called before the game initializes (before GL, Level, Player)
 *   init     -> called after the game has fully initialized
 *   postInit -> called after all mods have completed their init
 */
public interface IMod {

    /**
     * Called before game init. Safe to register tiles, events, commands here.
     * Do NOT access Level or Player here.
     */
    void preInit();

    /**
     * Called after game init. Level and Player are now available.
     * @param game the RubyDung game instance
     */
    void init(Object game);

    /**
     * Called after all mods have run init(). Good for cross-mod setup.
     */
    void postInit();
}
