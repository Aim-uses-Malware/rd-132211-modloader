package com.mojang.rubydung.mods.example;

import com.mojang.rubydung.RubyDung;
import com.mojang.rubydung.modloader.ModRegistry;
import com.mojang.rubydung.modloader.RDLoader;
import com.mojang.rubydung.modloader.api.IMod;
import com.mojang.rubydung.modloader.api.RDMod;
import com.mojang.rubydung.modloader.event.Events;
import com.mojang.rubydung.level.Level;
import com.mojang.rubydung.level.Tile;

/**
 * ExampleMod — демонстрирует все возможности rdLoader.
 *
 * Что делает этот мод:
 *  - Логирует все поломанные/поставленные блоки
 *  - Отменяет поломку блоков когда игрок находится высоко (y > 50)
 *  - Отменяет сохранение мира при зажатой клавише ENTER (если мод хочет)
 *  - Логирует позицию игрока каждые 100 тиков
 *  - Регистрирует команду /hello и /pos
 *  - Увеличивает высоту прыжка в 2 раза
 *  - Логирует когда мир загружается или сбрасывается
 */
@RDMod(
    id          = "examplemod",
    name        = "Example Mod",
    version     = "1.0.0",
    author      = "AimRite2",
    description = "Demonstrates all rdLoader features"
)
public class ExampleMod implements IMod {

    private int tickCounter = 0;

    @Override
    public void preInit() {
        System.out.println("[ExampleMod] preInit — регистрирую события и команды...");

        // ─── Событие: блок сломан ────────────────────────────────────────
        RDLoader.EVENT_BUS.register(Events.BlockBreakEvent.class, e -> {
            System.out.println("[ExampleMod] Попытка сломать блок на "
                    + e.x + "," + e.y + "," + e.z
                    + " (тип: " + e.oldType + ")");

            // Отменяем поломку если y > 50 (высоко в воздухе)
            if (e.y > 50) {
                System.out.println("[ExampleMod] Отмена: нельзя ломать блоки выше Y=50!");
                e.cancel();
            }
        });

        // ─── Событие: блок успешно сломан ───────────────────────────────
        RDLoader.EVENT_BUS.register(Events.BlockBrokenEvent.class, e -> {
            System.out.println("[ExampleMod] Блок сломан: " + e.x + "," + e.y + "," + e.z);
        });

        // ─── Событие: блок поставлен ────────────────────────────────────
        RDLoader.EVENT_BUS.register(Events.BlockPlacedEvent.class, e -> {
            System.out.println("[ExampleMod] Блок поставлен: " + e.x + "," + e.y + "," + e.z
                    + " (тип: " + e.type + ")");
        });

        // ─── Событие: тик игрока — логируем позицию каждые 100 тиков ───
        RDLoader.EVENT_BUS.register(Events.PlayerTickEvent.class, e -> {
            tickCounter++;
            if (tickCounter % 100 == 0) {
                System.out.printf("[ExampleMod] Игрок: x=%.1f y=%.1f z=%.1f onGround=%b%n",
                        e.player.x, e.player.y, e.player.z, e.player.onGround);
            }
        });

        // ─── Событие: прыжок — удваиваем силу ───────────────────────────
        RDLoader.EVENT_BUS.register(Events.PlayerJumpEvent.class, e -> {
            System.out.println("[ExampleMod] Прыжок! Удваиваю высоту прыжка.");
            e.jumpForce = e.jumpForce * 2.0f;
        });

        // ─── Событие: загрузка уровня ───────────────────────────────────
        RDLoader.EVENT_BUS.register(Events.LevelLoadEvent.class, e -> {
            System.out.println("[ExampleMod] Уровень загружен! Размер: "
                    + e.level.width + "x" + e.level.depth + "x" + e.level.height);
        });

        // ─── Событие: сброс уровня ──────────────────────────────────────
        RDLoader.EVENT_BUS.register(Events.LevelResetEvent.class, e -> {
            System.out.println("[ExampleMod] Уровень сбрасывается...");
            // Не отменяем — просто логируем
        });

        // ─── Событие: нажата клавиша ────────────────────────────────────
        RDLoader.EVENT_BUS.register(Events.KeyPressEvent.class, e -> {
            // Можно перехватывать клавиши здесь
            // e.cancel() чтобы заблокировать от игры
        });

        // ─── Событие: клик мышью ────────────────────────────────────────
        RDLoader.EVENT_BUS.register(Events.MouseClickEvent.class, e -> {
            // Кнопка 0 = левая (поставить), 1 = правая (сломать)
        });

        // ─── Команда: /hello ─────────────────────────────────────────────
        ModRegistry.registerCommand("hello", args -> {
            System.out.println("[ExampleMod] Привет от rdLoader! Аргументы: "
                    + java.util.Arrays.toString(args));
        });

        // ─── Команда: /pos ───────────────────────────────────────────────
        ModRegistry.registerCommand("pos", args -> {
            Object game = RDLoader.getGame();
            if (game instanceof RubyDung) {
                RubyDung rd = (RubyDung) game;
                if (rd.getPlayer() != null) {
                    System.out.printf("[ExampleMod] /pos: x=%.2f y=%.2f z=%.2f%n",
                            rd.getPlayer().x, rd.getPlayer().y, rd.getPlayer().z);
                }
            }
        });

        System.out.println("[ExampleMod] preInit готово.");
    }

    @Override
    public void init(Object game) {
        System.out.println("[ExampleMod] init — игра готова!");

        if (game instanceof RubyDung) {
            RubyDung rd = (RubyDung) game;
            Level level = rd.getLevel();
            System.out.println("[ExampleMod] Уровень: " + level.width
                    + "x" + level.depth + "x" + level.height
                    + ", блоков: " + level.getBlockCount());
        }
    }

    @Override
    public void postInit() {
        System.out.println("[ExampleMod] postInit — всё загружено, мод активен!");
        System.out.println("[ExampleMod] Зарегистрированные команды: "
                + ModRegistry.getCommandNames());
    }
}
