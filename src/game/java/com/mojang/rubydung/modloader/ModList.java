package com.mojang.rubydung.modloader;

import com.mojang.rubydung.mods.example.ExampleMod;
import com.mojang.rubydung.mods.worldgen

/**
 * ModList — центральный реестр всех модов.
 *
 * TeaVM не поддерживает reflection в браузере, поэтому вместо
 * автоматического поиска по classpath мы регистрируем моды вручную.
 *
 * Чтобы добавить свой мод:
 *   1. Создай класс с @RDMod и implements IMod
 *   2. Добавь строку: RDLoader.registerMod(new ВашМод());
 *
 * Этот файл вызывается из статического блока RubyDung ДО preInit().
 */
public final class ModList {

    /**
     * Регистрирует все моды. Вызывается из RubyDung.<clinit> до preInit().
     */
    public static void registerAll() {
        // ─── Встроенные моды rdLoader ──────────────────────────────────
        // Раскомментируй пример чтобы активировать:
        RDLoader.registerMod(new ExampleMod());
        RDLoader.registerMod(new WorldGen());

        // ─── Сюда добавляй свои моды ───────────────────────────────────
        // RDLoader.registerMod(new МойМод());
        // RDLoader.registerMod(new ЕщёОдинМод());
    }

    private ModList() {}
}
