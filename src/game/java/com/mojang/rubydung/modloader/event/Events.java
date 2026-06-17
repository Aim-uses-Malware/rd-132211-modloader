package com.mojang.rubydung.modloader.event;

import com.mojang.rubydung.HitResult;
import com.mojang.rubydung.Player;
import com.mojang.rubydung.level.Level;

/**
 * All events fired by rdLoader throughout the game loop.
 *
 * Register for events with:
 *   RDLoader.EVENT_BUS.register(EventPlayerTick.class, e -> { ... });
 */
public final class Events {

    // ─────────────────────────────────────────────────
    // Game lifecycle
    // ─────────────────────────────────────────────────

    /** Fired once after GL context and all game systems are ready. */
    public static class GameInitEvent extends Event {
        public final Object game; // RubyDung instance
        public GameInitEvent(Object game) { this.game = game; }
    }

    /** Fired every tick (before player tick). */
    public static class GameTickEvent extends Event {
        public final Object game;
        public GameTickEvent(Object game) { this.game = game; }
    }

    /** Fired each render frame. */
    public static class GameRenderEvent extends Event {
        public final Object game;
        public final float partialTicks;
        public GameRenderEvent(Object game, float partialTicks) {
            this.game = game;
            this.partialTicks = partialTicks;
        }
    }

    // ─────────────────────────────────────────────────
    // Player events
    // ─────────────────────────────────────────────────

    /** Fired every player tick. */
    public static class PlayerTickEvent extends Event {
        public final Player player;
        public PlayerTickEvent(Player player) { this.player = player; }
    }

    /** Fired when the player moves (position changed). */
    public static class PlayerMoveEvent extends Event {
        public final Player player;
        public final float dx, dy, dz;
        public PlayerMoveEvent(Player player, float dx, float dy, float dz) {
            this.player = player;
            this.dx = dx; this.dy = dy; this.dz = dz;
        }
    }

    /** Fired when the player jumps (yd set to jump force). */
    public static class PlayerJumpEvent extends Event {
        public final Player player;
        public float jumpForce;
        @Override public boolean isCancellable() { return true; }
        public PlayerJumpEvent(Player player, float jumpForce) {
            this.player = player;
            this.jumpForce = jumpForce;
        }
    }

    // ─────────────────────────────────────────────────
    // Block / world events
    // ─────────────────────────────────────────────────

    /** Fired when a block is about to be broken (right-click). Cancellable. */
    public static class BlockBreakEvent extends Event {
        public final Level level;
        public final int x, y, z;
        public final int oldType;
        @Override public boolean isCancellable() { return true; }
        public BlockBreakEvent(Level level, int x, int y, int z, int oldType) {
            this.level = level;
            this.x = x; this.y = y; this.z = z;
            this.oldType = oldType;
        }
    }

    /** Fired after a block has been successfully broken. */
    public static class BlockBrokenEvent extends Event {
        public final Level level;
        public final int x, y, z;
        public BlockBrokenEvent(Level level, int x, int y, int z) {
            this.level = level;
            this.x = x; this.y = y; this.z = z;
        }
    }

    /** Fired when a block is about to be placed (left-click). Cancellable. */
    public static class BlockPlaceEvent extends Event {
        public final Level level;
        public int x, y, z;
        public int type;
        @Override public boolean isCancellable() { return true; }
        public BlockPlaceEvent(Level level, int x, int y, int z, int type) {
            this.level = level;
            this.x = x; this.y = y; this.z = z;
            this.type = type;
        }
    }

    /** Fired after a block has been successfully placed. */
    public static class BlockPlacedEvent extends Event {
        public final Level level;
        public final int x, y, z, type;
        public BlockPlacedEvent(Level level, int x, int y, int z, int type) {
            this.level = level;
            this.x = x; this.y = y; this.z = z;
            this.type = type;
        }
    }

    /** Fired when a tile changes in the level (setTile called). */
    public static class TileChangedEvent extends Event {
        public final Level level;
        public final int x, y, z, newType;
        public TileChangedEvent(Level level, int x, int y, int z, int newType) {
            this.level = level;
            this.x = x; this.y = y; this.z = z;
            this.newType = newType;
        }
    }

    // ─────────────────────────────────────────────────
    // Raycast / hit events
    // ─────────────────────────────────────────────────

    /** Fired when the pick ray updates (every render frame). HitResult may be null. */
    public static class PickEvent extends Event {
        public final HitResult hitResult; // null if nothing hit
        public PickEvent(HitResult hitResult) { this.hitResult = hitResult; }
    }

    // ─────────────────────────────────────────────────
    // Level events
    // ─────────────────────────────────────────────────

    /** Fired after the level loads from disk. */
    public static class LevelLoadEvent extends Event {
        public final Level level;
        public LevelLoadEvent(Level level) { this.level = level; }
    }

    /** Fired before the level saves to disk. Cancellable. */
    public static class LevelSaveEvent extends Event {
        public final Level level;
        @Override public boolean isCancellable() { return true; }
        public LevelSaveEvent(Level level) { this.level = level; }
    }

    /** Fired when the level is reset. Cancellable. */
    public static class LevelResetEvent extends Event {
        public final Level level;
        @Override public boolean isCancellable() { return true; }
        public LevelResetEvent(Level level) { this.level = level; }
    }

    // ─────────────────────────────────────────────────
    // Key / mouse events
    // ─────────────────────────────────────────────────

    /** Fired when a keyboard key is pressed. Cancellable (blocks game key handling). */
    public static class KeyPressEvent extends Event {
        public final int key;
        @Override public boolean isCancellable() { return true; }
        public KeyPressEvent(int key) { this.key = key; }
    }

    /** Fired when a mouse button is pressed. Cancellable. */
    public static class MouseClickEvent extends Event {
        public final int button;
        @Override public boolean isCancellable() { return true; }
        public MouseClickEvent(int button) { this.button = button; }
    }

    private Events() {}
}
