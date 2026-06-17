package com.mojang.rubydung.modloader;

import com.mojang.rubydung.level.Tile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for mod-registered content.
 *
 * Currently supports: custom Tile types.
 * Future: entities, items, commands, etc.
 */
public class ModRegistry {

    // ─────────────────────────────────────────────────
    // Tiles
    // ─────────────────────────────────────────────────

    /**
     * Info about a registered tile type.
     */
    public static class TileEntry {
        public final String modId;
        public final String tileId;    // e.g. "mymod:cobblestone"
        public final int    numericId; // 2..255
        public final Tile   tile;

        TileEntry(String modId, String tileId, int numericId, Tile tile) {
            this.modId     = modId;
            this.tileId    = tileId;
            this.numericId = numericId;
            this.tile      = tile;
        }

        @Override public String toString() {
            return tileId + "(" + numericId + ")";
        }
    }

    // Vanilla uses 0 (air) and 1 (stone/grass). Mods start at ID 2.
    private static int nextTileId = 2;

    private static final Map<String, TileEntry> tileById  = new HashMap<>();
    private static final Map<Integer, TileEntry> tileByNum = new HashMap<>();
    private static final List<TileEntry>         allTiles  = new ArrayList<>();

    /**
     * Register a custom tile.
     *
     * @param modId    the mod's @RDMod id
     * @param tileId   a unique name for this tile (modid:name format recommended)
     * @param tile     the Tile instance to register
     * @return the numeric ID assigned to this tile
     * @throws IllegalArgumentException if tileId is already registered
     * @throws IllegalStateException    if the numeric ID space is full (>255)
     */
    public static int registerTile(String modId, String tileId, Tile tile) {
        if (tileById.containsKey(tileId)) {
            throw new IllegalArgumentException("[rdLoader/Registry] Tile already registered: " + tileId);
        }
        if (nextTileId > 255) {
            throw new IllegalStateException("[rdLoader/Registry] Tile ID space exhausted (max 255)");
        }

        int id = nextTileId++;
        TileEntry entry = new TileEntry(modId, tileId, id, tile);
        tileById.put(tileId, entry);
        tileByNum.put(id, entry);
        allTiles.add(entry);

        System.out.println("[rdLoader/Registry] Registered tile: " + tileId + " -> id=" + id + " (from mod " + modId + ")");
        return id;
    }

    /** Look up a tile entry by its string ID. Returns null if not found. */
    public static TileEntry getTileById(String tileId) {
        return tileById.get(tileId);
    }

    /** Look up a tile entry by its numeric ID. Returns null if vanilla or unknown. */
    public static TileEntry getTileByNumericId(int id) {
        return tileByNum.get(id);
    }

    /** Whether a numeric ID belongs to a mod tile. */
    public static boolean isModTile(int id) {
        return id >= 2 && tileByNum.containsKey(id);
    }

    /** All registered mod tiles. */
    public static List<TileEntry> getAllTiles() {
        return new ArrayList<>(allTiles);
    }

    // ─────────────────────────────────────────────────
    // Commands (simple in-game text commands)
    // ─────────────────────────────────────────────────

    public interface CommandHandler {
        /**
         * Execute the command.
         * @param args the whitespace-split tokens after the command name
         */
        void execute(String[] args);
    }

    private static final Map<String, CommandHandler> commands = new HashMap<>();

    /**
     * Register a command accessible via the in-game console (if a mod adds one).
     */
    public static void registerCommand(String name, CommandHandler handler) {
        String key = name.toLowerCase();
        if (commands.containsKey(key)) {
            System.err.println("[rdLoader/Registry] Warning: overwriting command /" + key);
        }
        commands.put(key, handler);
        System.out.println("[rdLoader/Registry] Registered command: /" + key);
    }

    /** Execute a command string. Returns false if no such command. */
    public static boolean executeCommand(String input) {
        if (input == null || input.isEmpty()) return false;
        String[] parts = input.trim().split("\\s+");
        String name = parts[0].toLowerCase();
        if (name.startsWith("/")) name = name.substring(1);

        CommandHandler handler = commands.get(name);
        if (handler == null) return false;

        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        try {
            handler.execute(args);
        } catch (Throwable t) {
            System.err.println("[rdLoader/Registry] Command /" + name + " threw: " + t.getMessage());
        }
        return true;
    }

    /** All registered command names. */
    public static List<String> getCommandNames() {
        return new ArrayList<>(commands.keySet());
    }

    private ModRegistry() {}
}
