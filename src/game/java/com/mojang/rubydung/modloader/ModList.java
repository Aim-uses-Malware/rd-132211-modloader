package com.mojang.rubydung.modloader;

import com.mojang.rubydung.mods.example.ExampleMod;
import com.mojang.rubydung.mods.worldgen.WorldGenMod;

/**
 * ModList — центральный реестр всех модов.
 *
 * TeaVM не поддерживает reflection в браузере, поэтому вместо
 * автоматического поиска по classpath мы регистрируем моды вручную.
 */
public final class ModList {

    /**
     * Регистрирует все моды. Вызывается из RubyDung.<clinit> до preInit().
     */
    public static void registerAll() {
        // ─── Встроенные моды rdLoader ──────────────────────────────────
        RDLoader.registerMod(new ExampleMod());
        RDLoader.registerMod(new WorldGenMod());

        // ─── Сюда добавляй свои моды ───────────────────────────────────
        // RDLoader.registerMod(new МойМод());
    }

    private ModList() {}
}
