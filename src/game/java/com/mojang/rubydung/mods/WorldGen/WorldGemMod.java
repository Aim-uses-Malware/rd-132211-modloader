package com.mojang.rubydung.mods.worldgen;

import com.mojang.rubydung.level.Level;
import com.mojang.rubydung.modloader.RDLoader;
import com.mojang.rubydung.modloader.api.IMod;
import com.mojang.rubydung.modloader.api.RDMod;
import com.mojang.rubydung.modloader.event.Events;

/**
 * WorldGenMod — генерирует реалистичный мир 256×256×64 вместо дефолтного плоского.
 *
 * Алгоритм:
 *   1. Многооктавный Perlin noise → карта высот (Y = 20..52)
 *   2. Снизу → поверхность: камень (tile=1)
 *   3. Несколько слоёв под поверхностью → тоже камень (grass texture на поверхности делает Chunk)
 *   4. Вода: если высота < SEA_LEVEL — заполняем до SEA_LEVEL водяным блоком... но у нас нет
 *      отдельного tile ID воды, поэтому оставляем воздух (пещеры в горах — later)
 *   5. Пещеры: 3D Perlin noise → вырезаем тоннели внутри массива
 *   6. Сохранение: перехватываем LevelResetEvent чтобы при сбросе тоже запускать WorldGen
 *
 * Запускается только если level.dat не существует (wasLoaded = false).
 */
@RDMod(
    id          = "worldgen",
    name        = "WorldGen",
    version     = "1.0.0",
    author      = "rdLoader",
    description = "Replaces flat world with Minecraft-style terrain generation."
)
public class WorldGenMod implements IMod {

    // ─── Константы генерации ──────────────────────────────────────────────
    private static final int SEA_LEVEL    = 32;   // уровень моря
    private static final int BASE_HEIGHT  = 28;   // минимальная высота ландшафта
    private static final int HEIGHT_RANGE = 20;   // амплитуда рельефа (28..48)
    private static final int CAVE_PASSES  = 3;    // количество проходов пещер
    private static final int BEDROCK_Y    = 0;    // бедрок

    // Block IDs (у нас только 1 = solid, 0 = air)
    private static final byte AIR    = 0;
    private static final byte SOLID  = 1;

    private long worldSeed = 0;

    @Override
    public void preInit() {
        System.out.println("[WorldGen] preInit — регистрируем генератор");

        // Перехватываем загрузку уровня: если мир новый — генерируем
        RDLoader.EVENT_BUS.register(Events.LevelLoadEvent.class, e -> {
            Level level = e.level;
            if (!level.wasLoaded) {
                System.out.println("[WorldGen] Новый мир — запускаем генерацию...");
                worldSeed = System.currentTimeMillis();
                generate(level);
                System.out.println("[WorldGen] Генерация завершена.");
            }
        });

        // Перехватываем сброс уровня (cancellable) — отменяем стандартный сброс,
        // запускаем нашу генерацию вместо него
        RDLoader.EVENT_BUS.register(Events.LevelResetEvent.class, e -> {
            e.cancel(); // отменяем дефолтный reset
            Level level = e.level;
            System.out.println("[WorldGen] Reset — регенерируем мир...");
            worldSeed = System.currentTimeMillis();
            // Очищаем сохранение чтобы при следующем старте тоже генерировался
            try {
                net.lax1dude.eaglercraft.internal.vfs2.VFile2 file =
                    new net.lax1dude.eaglercraft.internal.vfs2.VFile2("level.dat");
                if (file.exists()) file.delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            level.wasLoaded = false;
            generate(level);
            level.save();
            System.out.println("[WorldGen] Регенерация завершена.");
        });
    }

    @Override
    public void init(Object game) {
        System.out.println("[WorldGen] init");
    }

    @Override
    public void postInit() {}

    // ─── Генерация мира ───────────────────────────────────────────────────

    private void generate(Level level) {
        int W = level.width;   // 256
        int H = level.height;  // 256
        int D = level.depth;   // 64

        byte[] blocks = new byte[W * H * D];

        // ── 1. Карта высот через многооктавный Perlin ──────────────────
        int[] heightMap = new int[W * H];
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < H; z++) {
                double nx = (double) x / W;
                double nz = (double) z / H;

                // 4 октавы с убывающей амплитудой
                double n = 0;
                n += octave(nx, nz, 1, worldSeed)         * 1.00;
                n += octave(nx, nz, 2, worldSeed + 1)     * 0.50;
                n += octave(nx, nz, 4, worldSeed + 2)     * 0.25;
                n += octave(nx, nz, 8, worldSeed + 3)     * 0.125;
                n /= 1.875; // нормализуем к -1..1

                // Билинейное сглаживание: горы выше за счёт pow
                double t = (n + 1.0) * 0.5; // 0..1
                t = Math.pow(t, 1.2);        // немного сглаживаем плоские участки

                heightMap[x + z * W] = BASE_HEIGHT + (int)(t * HEIGHT_RANGE);
            }
        }

        // ── 2. Заполнение блоков по карте высот ───────────────────────
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < H; z++) {
                int h = heightMap[x + z * W];
                for (int y = 0; y < D; y++) {
                    int idx = (y * H + z) * W + x;
                    if (y == BEDROCK_Y) {
                        blocks[idx] = SOLID; // бедрок снизу всегда
                    } else if (y <= h) {
                        blocks[idx] = SOLID;
                    } else {
                        blocks[idx] = AIR;
                    }
                }
            }
        }

        // ── 3. Пещеры через 3D Perlin ─────────────────────────────────
        for (int pass = 0; pass < CAVE_PASSES; pass++) {
            long caveSeed = worldSeed + 100 + pass * 7919L;
            double freq = 0.05 + pass * 0.02;
            for (int x = 1; x < W - 1; x++) {
                for (int z = 1; z < H - 1; z++) {
                    for (int y = 2; y < D - 4; y++) {
                        double nx = x * freq;
                        double ny = y * freq * 1.5;
                        double nz = z * freq;
                        double v = noise3d(nx, ny, nz, caveSeed);
                        // порог: чем ниже тем реже пещеры
                        double threshold = 0.72 - (double) y / (D * 3.0);
                        if (v > threshold) {
                            int idx = (y * H + z) * W + x;
                            blocks[idx] = AIR;
                        }
                    }
                }
            }
        }

        // ── 4. Применяем к уровню ─────────────────────────────────────
        level.setAllBlocks(blocks);
    }

    // ─── Perlin noise ─────────────────────────────────────────────────────
    // Простая реализация без массивов permutation (TeaVM-безопасная)

    /** 2D октава Perlin noise, возвращает -1..1 */
    private double octave(double x, double z, double freq, long seed) {
        return noise2d(x * freq, z * freq, seed);
    }

    /** 2D value noise с бикубической интерполяцией */
    private double noise2d(double x, double z, long seed) {
        int xi = fastFloor(x);
        int zi = fastFloor(z);
        double xf = x - xi;
        double zf = z - zi;

        double fx = fade(xf);
        double fz = fade(zf);

        double v00 = gradHash2(xi,   zi,   seed);
        double v10 = gradHash2(xi+1, zi,   seed);
        double v01 = gradHash2(xi,   zi+1, seed);
        double v11 = gradHash2(xi+1, zi+1, seed);

        return lerp(fz,
            lerp(fx, v00, v10),
            lerp(fx, v01, v11));
    }

    /** 3D value noise */
    private double noise3d(double x, double y, double z, long seed) {
        int xi = fastFloor(x);
        int yi = fastFloor(y);
        int zi = fastFloor(z);
        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;

        double fx = fade(xf);
        double fy = fade(yf);
        double fz = fade(zf);

        double v000 = gradHash3(xi,   yi,   zi,   seed);
        double v100 = gradHash3(xi+1, yi,   zi,   seed);
        double v010 = gradHash3(xi,   yi+1, zi,   seed);
        double v110 = gradHash3(xi+1, yi+1, zi,   seed);
        double v001 = gradHash3(xi,   yi,   zi+1, seed);
        double v101 = gradHash3(xi+1, yi,   zi+1, seed);
        double v011 = gradHash3(xi,   yi+1, zi+1, seed);
        double v111 = gradHash3(xi+1, yi+1, zi+1, seed);

        return lerp(fz,
            lerp(fy, lerp(fx, v000, v100), lerp(fx, v010, v110)),
            lerp(fy, lerp(fx, v001, v101), lerp(fx, v011, v111)));
    }

    /** Смешение для плавной интерполяции (6t^5 - 15t^4 + 10t^3) */
    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    /** Детерминированный хэш 2D → -1..1 */
    private static double gradHash2(int x, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0x6C62272E07BB0142L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (double)(h & 0x7FFFFFFL) / (double) 0x7FFFFFFL * 2.0 - 1.0;
    }

    /** Детерминированный хэш 3D → -1..1 */
    private static double gradHash3(int x, int y, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xBF58476D1CE4E5B9L) ^ (z * 0x6C62272E07BB0142L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (double)(h & 0x7FFFFFFL) / (double) 0x7FFFFFFL * 2.0 - 1.0;
    }
}
