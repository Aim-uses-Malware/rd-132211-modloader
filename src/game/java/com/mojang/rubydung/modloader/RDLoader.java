package com.mojang.rubydung.modloader;

import com.mojang.rubydung.modloader.api.IMod;
import com.mojang.rubydung.modloader.api.RDMod;
import com.mojang.rubydung.modloader.event.Events;

import java.util.*;

/**
 * ██████╗ ██████╗ ██╗      ██████╗  █████╗ ██████╗ ███████╗██████╗
 * ██╔══██╗██╔══██╗██║     ██╔═══██╗██╔══██╗██╔══██╗██╔════╝██╔══██╗
 * ██████╔╝██║  ██║██║     ██║   ██║███████║██║  ██║█████╗  ██████╔╝
 * ██╔══██╗██║  ██║██║     ██║   ██║██╔══██║██║  ██║██╔══╝  ██╔══██╗
 * ██║  ██║██████╔╝███████╗╚██████╔╝██║  ██║██████╔╝███████╗██║  ██║
 * ╚═╝  ╚═╝╚═════╝ ╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝
 *
 * rdLoader — Modloader for rd-132211 (earliest Minecraft pre-classic)
 * by AimRite2 / Aim-uses-Malware
 *
 * ─────────────────────────────────────────────────────────────────────
 * HOW IT WORKS
 * ─────────────────────────────────────────────────────────────────────
 * 1. Mods register themselves by calling RDLoader.registerMod(new MyMod());
 *    or are auto-discovered via the MOD_CLASSES list (for TeaVM compatibility,
 *    since reflection is limited in TeaVM/browser builds).
 *
 * 2. The patched RubyDung calls:
 *      RDLoader.preInit()  — before GL context
 *      RDLoader.init(game) — after game init
 *      RDLoader.postInit() — after all mods loaded
 *
 * 3. Mods interact with the game via the EVENT_BUS and REGISTRY.
 *
 * ─────────────────────────────────────────────────────────────────────
 * CREATING A MOD
 * ─────────────────────────────────────────────────────────────────────
 *
 *   @RDMod(id = "mymcmod", name = "My Mod", version = "1.0", author = "You")
 *   public class MyMod implements IMod {
 *
 *       @Override
 *       public void preInit() {
 *           // Register tiles, commands, event listeners here
 *           RDLoader.EVENT_BUS.register(Events.BlockBreakEvent.class, e -> {
 *               System.out.println("Block broken at " + e.x + "," + e.y + "," + e.z);
 *           });
 *       }
 *
 *       @Override
 *       public void init(Object game) {
 *           // Access game (RubyDung) here
 *       }
 *
 *       @Override
 *       public void postInit() {}
 *   }
 *
 *   // In your mod registration file or static initializer:
 *   static { RDLoader.registerMod(new MyMod()); }
 */
public final class RDLoader {

    public static final String VERSION = "1.0.0";
    public static final String BRAND   = "rdLoader";

    /** Global event bus — mods subscribe here, game fires events here. */
    public static final EventBus EVENT_BUS = new EventBus();

    /** Central content registry — tiles, commands, etc. */
    public static final ModRegistry REGISTRY = null; // static methods only

    // ─────────────────────────────────────────────────
    // Internal state
    // ─────────────────────────────────────────────────

    private static final List<ModContainer> loadedMods = new ArrayList<>();
    private static final Map<String, ModContainer> modIndex = new HashMap<>();

    private static boolean preInitDone  = false;
    private static boolean initDone     = false;
    private static boolean postInitDone = false;

    private static Object gameInstance  = null;

    // ─────────────────────────────────────────────────
    // Mod registration
    // ─────────────────────────────────────────────────

    /**
     * Register a mod instance. Call this before RDLoader.preInit().
     * The mod class must be annotated with @RDMod.
     */
    public static void registerMod(IMod mod) {
        RDMod meta = mod.getClass().getAnnotation(RDMod.class);
        if (meta == null) {
            throw new IllegalArgumentException("[rdLoader] Mod class " + mod.getClass().getName()
                    + " is missing the @RDMod annotation!");
        }

        if (modIndex.containsKey(meta.id())) {
            System.err.println("[rdLoader] WARNING: Duplicate mod ID '" + meta.id() + "' — skipping.");
            return;
        }

        ModContainer container = new ModContainer(mod, meta);
        loadedMods.add(container);
        modIndex.put(meta.id(), container);
        System.out.println("[rdLoader] Registered mod: " + container);
    }

    // ─────────────────────────────────────────────────
    // Lifecycle — called by patched RubyDung
    // ─────────────────────────────────────────────────

    /**
     * Phase 1 — called before GL/game init.
     * Mods register tiles, events, commands here.
     */
    public static void preInit() {
        if (preInitDone) return;

        System.out.println("[rdLoader] ═══════════════════════════════");
        System.out.println("[rdLoader] rdLoader v" + VERSION + " starting...");
        System.out.println("[rdLoader] " + loadedMods.size() + " mod(s) discovered.");
        System.out.println("[rdLoader] ═══════════════════════════════");

        List<ModContainer> sorted = sortByDependencies(loadedMods);

        for (ModContainer mod : sorted) {
            System.out.println("[rdLoader] [preInit] " + mod.getName() + " v" + mod.getVersion());
            try {
                mod.getInstance().preInit();
                mod.setState(ModContainer.State.PRE_INIT);
            } catch (Throwable t) {
                mod.setError("preInit failed: " + t.getMessage());
                System.err.println("[rdLoader] FAILED preInit for " + mod.getId() + ": " + t.getMessage());
                t.printStackTrace();
            }
        }

        preInitDone = true;
        System.out.println("[rdLoader] preInit complete.");
    }

    /**
     * Phase 2 — called after game systems are initialized (GL, Level, Player ready).
     * @param game the RubyDung instance
     */
    public static void init(Object game) {
        if (!preInitDone) preInit();
        if (initDone) return;

        gameInstance = game;

        System.out.println("[rdLoader] [init] Initializing " + loadedMods.size() + " mod(s)...");
        for (ModContainer mod : loadedMods) {
            if (mod.hasError()) continue;
            System.out.println("[rdLoader] [init] " + mod.getName());
            try {
                mod.getInstance().init(game);
                mod.setState(ModContainer.State.INIT);
            } catch (Throwable t) {
                mod.setError("init failed: " + t.getMessage());
                System.err.println("[rdLoader] FAILED init for " + mod.getId() + ": " + t.getMessage());
                t.printStackTrace();
            }
        }

        // Fire the game init event
        EVENT_BUS.post(new Events.GameInitEvent(game));

        initDone = true;
        System.out.println("[rdLoader] init complete.");
    }

    /**
     * Phase 3 — called after all mods have run init().
     */
    public static void postInit() {
        if (!initDone) return;
        if (postInitDone) return;

        System.out.println("[rdLoader] [postInit] Running...");
        for (ModContainer mod : loadedMods) {
            if (mod.hasError()) continue;
            try {
                mod.getInstance().postInit();
                mod.setState(ModContainer.State.POST_INIT);
            } catch (Throwable t) {
                mod.setError("postInit failed: " + t.getMessage());
                System.err.println("[rdLoader] FAILED postInit for " + mod.getId() + ": " + t.getMessage());
                t.printStackTrace();
            }
        }

        postInitDone = true;
        printLoadSummary();
    }

    // ─────────────────────────────────────────────────
    // Event dispatch helpers (called from patched game code)
    // ─────────────────────────────────────────────────

    /** Called every game tick. Returns true if tick should be skipped (never for this event). */
    public static void fireTick(Object game) {
        EVENT_BUS.post(new Events.GameTickEvent(game));
    }

    /** Called every render frame. */
    public static void fireRender(Object game, float partialTicks) {
        EVENT_BUS.post(new Events.GameRenderEvent(game, partialTicks));
    }

    /**
     * Called when a block break is attempted.
     * @return true if the event was cancelled (prevent the break)
     */
    public static boolean fireBlockBreak(
            com.mojang.rubydung.level.Level level, int x, int y, int z, int oldType) {
        boolean cancelled = EVENT_BUS.post(new Events.BlockBreakEvent(level, x, y, z, oldType));
        if (!cancelled) {
            EVENT_BUS.post(new Events.BlockBrokenEvent(level, x, y, z));
        }
        return cancelled;
    }

    /**
     * Called when a block place is attempted.
     * @return true if the event was cancelled (prevent the placement)
     */
    public static boolean fireBlockPlace(
            com.mojang.rubydung.level.Level level, int x, int y, int z, int type) {
        Events.BlockPlaceEvent placeEvent = new Events.BlockPlaceEvent(level, x, y, z, type);
        boolean cancelled = EVENT_BUS.post(placeEvent);
        if (!cancelled) {
            EVENT_BUS.post(new Events.BlockPlacedEvent(level, placeEvent.x, placeEvent.y, placeEvent.z, placeEvent.type));
        }
        return cancelled;
    }

    /** Called from Level.setTile for any tile change. */
    public static void fireTileChanged(
            com.mojang.rubydung.level.Level level, int x, int y, int z, int newType) {
        EVENT_BUS.post(new Events.TileChangedEvent(level, x, y, z, newType));
    }

    /** Called from Level.save before saving. Returns true if save should be skipped. */
    public static boolean fireLevelSave(com.mojang.rubydung.level.Level level) {
        return EVENT_BUS.post(new Events.LevelSaveEvent(level));
    }

    /** Called from Level.load after loading. */
    public static void fireLevelLoad(com.mojang.rubydung.level.Level level) {
        EVENT_BUS.post(new Events.LevelLoadEvent(level));
    }

    /** Called from Level.reset before resetting. Returns true if reset should be skipped. */
    public static boolean fireLevelReset(com.mojang.rubydung.level.Level level) {
        return EVENT_BUS.post(new Events.LevelResetEvent(level));
    }

    /** Called from Player.tick. */
    public static void firePlayerTick(com.mojang.rubydung.Player player) {
        EVENT_BUS.post(new Events.PlayerTickEvent(player));
    }

    /** Called when player jumps. Returns modified jump force (or 0 if cancelled). */
    public static float firePlayerJump(com.mojang.rubydung.Player player, float jumpForce) {
        Events.PlayerJumpEvent event = new Events.PlayerJumpEvent(player, jumpForce);
        boolean cancelled = EVENT_BUS.post(event);
        return cancelled ? 0f : event.jumpForce;
    }

    /** Called from pick after raycast. */
    public static void firePick(com.mojang.rubydung.HitResult hit) {
        EVENT_BUS.post(new Events.PickEvent(hit));
    }

    /** Called on key press. Returns true if the key event should be swallowed. */
    public static boolean fireKeyPress(int key) {
        return EVENT_BUS.post(new Events.KeyPressEvent(key));
    }

    /** Called on mouse click. Returns true if the click should be swallowed. */
    public static boolean fireMouseClick(int button) {
        return EVENT_BUS.post(new Events.MouseClickEvent(button));
    }

    // ─────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────

    /** Get all loaded mod containers. */
    public static List<ModContainer> getMods() {
        return Collections.unmodifiableList(loadedMods);
    }

    /** Find a mod by its ID. Returns null if not found. */
    public static ModContainer getMod(String id) {
        return modIndex.get(id);
    }

    /** Whether a specific mod is loaded and healthy. */
    public static boolean isModLoaded(String id) {
        ModContainer c = modIndex.get(id);
        return c != null && !c.hasError();
    }

    public static Object getGame() {
        return gameInstance;
    }

    // ─────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────

    /**
     * Topological sort: mods that depend on others are loaded after their dependencies.
     * Cycles are broken with a warning.
     */
    private static List<ModContainer> sortByDependencies(List<ModContainer> mods) {
        List<ModContainer> sorted = new ArrayList<>();
        // Use ArrayList instead of HashSet — TeaVM WASM-GC has an illegal cast bug
        // in HashSet.remove(Object) / HashMap.removeByKey when generic type info is erased.
        List<String> visited    = new ArrayList<>();
        List<String> inProgress = new ArrayList<>();

        Map<String, ModContainer> byId = new HashMap<>();
        for (ModContainer m : mods) byId.put(m.getId(), m);

        for (ModContainer m : mods) {
            visitMod(m, byId, visited, inProgress, sorted);
        }
        return sorted;
    }

    private static void visitMod(ModContainer mod,
                                  Map<String, ModContainer> byId,
                                  List<String> visited,
                                  List<String> inProgress,
                                  List<ModContainer> out) {
        String id = mod.getId();
        if (listContains(visited, id)) return;
        if (listContains(inProgress, id)) {
            System.err.println("[rdLoader] Dependency cycle detected for mod: " + id);
            return;
        }
        inProgress.add(id);
        for (String dep : mod.getMeta().dependencies()) {
            ModContainer depMod = byId.get(dep);
            if (depMod == null) {
                System.err.println("[rdLoader] WARNING: mod '" + id
                        + "' requires missing dependency '" + dep + "'");
            } else {
                visitMod(depMod, byId, visited, inProgress, out);
            }
        }
        listRemove(inProgress, id);
        visited.add(id);
        out.add(mod);
    }

    /** TeaVM-safe String contains check (avoids generic type cast issues). */
    private static boolean listContains(List<String> list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(value)) return true;
        }
        return false;
    }

    /** TeaVM-safe String remove (avoids HashSet.remove illegal cast in WASM-GC). */
    private static void listRemove(List<String> list, String value) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).equals(value)) {
                list.remove(i);
                return;
            }
        }
    }

    private static void printLoadSummary() {
        System.out.println("[rdLoader] ═══════════════════════════════");
        System.out.println("[rdLoader] Load summary:");
        int ok = 0, err = 0;
        for (ModContainer m : loadedMods) {
            String status = m.hasError() ? "✗ ERROR: " + m.getError() : "✓ OK";
            System.out.println("[rdLoader]   " + m.getName() + " v" + m.getVersion()
                    + " by " + m.getAuthor() + " — " + status);
            if (m.hasError()) err++; else ok++;
        }
        System.out.println("[rdLoader] " + ok + " loaded, " + err + " failed.");
        System.out.println("[rdLoader] EventBus: " + EVENT_BUS.getTotalListenerCount() + " listener(s).");
        System.out.println("[rdLoader] Tiles registered: " + ModRegistry.getAllTiles().size());
        System.out.println("[rdLoader] ═══════════════════════════════");
    }

    private RDLoader() {}
}
