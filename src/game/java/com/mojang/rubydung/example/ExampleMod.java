package com.mojang.rubydung.mods.example;

import com.mojang.rubydung.level.Tesselator;
import com.mojang.rubydung.modloader.RDLoader;
import com.mojang.rubydung.modloader.api.IMod;
import com.mojang.rubydung.modloader.api.RDMod;
import com.mojang.rubydung.modloader.event.Events;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * ExampleMod — демонстрационный мод для rdLoader.
 * Добавляет чат как в обычном Minecraft:
 *   T       — открыть чат
 *   Enter   — отправить сообщение
 *   Esc     — закрыть без отправки
 *   Backspace — удалить символ
 */
@RDMod(
    id          = "examplemod",
    name        = "Example Mod",
    version     = "1.0.0",
    author      = "rdLoader",
    description = "Adds Minecraft-style chat to rd-132211."
)
public class ExampleMod implements IMod {

    // ─── Состояние чата ───────────────────────────────────────────────────
    private boolean       chatOpen  = false;
    private StringBuilder inputBuf  = new StringBuilder();
    private final List<String> history  = new ArrayList<>();
    private long          cursorTimer   = 0;

    private static final int MAX_INPUT   = 100;
    private static final int MAX_HISTORY = 10;

    // ─── Растровый 5×7 шрифт ──────────────────────────────────────────────
    // Колонки глифа, биты = строки (бит 0 = верх)
    private static final int[][] GLYPHS = {
        // 0: space
        {0,0,0,0,0},
        // A-Z (1-26)
        {0b1111110,0b0001001,0b0001001,0b0001001,0b1111110}, // A
        {0b1111111,0b1001001,0b1001001,0b1001001,0b0110110}, // B
        {0b0111110,0b1000001,0b1000001,0b1000001,0b0100010}, // C
        {0b1111111,0b1000001,0b1000001,0b1000001,0b0111110}, // D
        {0b1111111,0b1001001,0b1001001,0b1001001,0b1000001}, // E
        {0b1111111,0b0001001,0b0001001,0b0001001,0b0000001}, // F
        {0b0111110,0b1000001,0b1001001,0b1001001,0b0111010}, // G
        {0b1111111,0b0001000,0b0001000,0b0001000,0b1111111}, // H
        {0b0000000,0b1000001,0b1111111,0b1000001,0b0000000}, // I
        {0b0100000,0b1000000,0b1000001,0b0111111,0b0000001}, // J
        {0b1111111,0b0001000,0b0010100,0b0100010,0b1000001}, // K
        {0b1111111,0b1000000,0b1000000,0b1000000,0b1000000}, // L
        {0b1111111,0b0000010,0b0000100,0b0000010,0b1111111}, // M
        {0b1111111,0b0000110,0b0001000,0b0110000,0b1111111}, // N
        {0b0111110,0b1000001,0b1000001,0b1000001,0b0111110}, // O
        {0b1111111,0b0001001,0b0001001,0b0001001,0b0000110}, // P
        {0b0111110,0b1000001,0b1010001,0b0100001,0b1011110}, // Q
        {0b1111111,0b0001001,0b0011001,0b0101001,0b1000110}, // R
        {0b0100110,0b1001001,0b1001001,0b1001001,0b0110010}, // S
        {0b0000001,0b0000001,0b1111111,0b0000001,0b0000001}, // T
        {0b0111111,0b1000000,0b1000000,0b1000000,0b0111111}, // U
        {0b0011111,0b0100000,0b1000000,0b0100000,0b0011111}, // V
        {0b1111111,0b0100000,0b0011000,0b0100000,0b1111111}, // W
        {0b1100011,0b0010100,0b0001000,0b0010100,0b1100011}, // X
        {0b0000111,0b0001000,0b1110000,0b0001000,0b0000111}, // Y
        {0b1100001,0b1010001,0b1001001,0b1000101,0b1000011}, // Z
        // 0-9 (27-36)
        {0b0111110,0b1010001,0b1001001,0b1000101,0b0111110}, // 0
        {0b0000000,0b1000010,0b1111111,0b1000000,0b0000000}, // 1
        {0b1100010,0b1010001,0b1001001,0b1001001,0b1000110}, // 2
        {0b0100010,0b1000001,0b1001001,0b1001001,0b0110110}, // 3
        {0b0011000,0b0010100,0b0010010,0b1111111,0b0010000}, // 4
        {0b0100111,0b1000101,0b1000101,0b1000101,0b0111001}, // 5
        {0b0111110,0b1001001,0b1001001,0b1001001,0b0110000}, // 6
        {0b0000001,0b1110001,0b0001001,0b0000101,0b0000011}, // 7
        {0b0110110,0b1001001,0b1001001,0b1001001,0b0110110}, // 8
        {0b0000110,0b1001001,0b1001001,0b1001001,0b0111110}, // 9
        // спецсимволы (37+)
        {0b0000000,0b1100000,0b1100000,0b0000000,0b0000000}, // .  37
        {0b0000000,0b1010000,0b0110000,0b0000000,0b0000000}, // ,  38
        {0b0000000,0b0000000,0b1011111,0b0000000,0b0000000}, // !  39
        {0b0000010,0b0000001,0b1010001,0b0001001,0b0000110}, // ?  40
        {0b0000000,0b0110110,0b0110110,0b0000000,0b0000000}, // :  41
        {0b0001000,0b0001000,0b0001000,0b0001000,0b0001000}, // -  42
        {0b1000000,0b0100000,0b0010000,0b0001000,0b0000100}, // /  43
        {0b0000000,0b0000000,0b0000111,0b0000000,0b0000000}, // '  44
        {0b0001000,0b0001000,0b0111110,0b0001000,0b0001000}, // +  45
        {0b0000000,0b0000000,0b0000000,0b0000000,0b0000000}, // _  46 (пусто)
        {0b0110110,0b1001001,0b0000000,0b0000000,0b0000000}, // "  47
        {0b0000001,0b0000001,0b0000001,0b0000001,0b0000001}, // |  48
        {0b0001000,0b0010100,0b0100010,0b1000001,0b0000000}, // >  49
        {0b0000000,0b1000001,0b0100010,0b0010100,0b0001000}, // <  50
        {0b0110110,0b0110110,0b0000000,0b0000000,0b0000000}, // ;  51
        {0b0101000,0b0101000,0b0101000,0b0000000,0b0000000}, // ~  52 (approx)
        {0b0000010,0b0000101,0b0000010,0b0000000,0b0000000}, // ^  53
        {0b1000000,0b0100000,0b0011111,0b0000000,0b0000000}, // \  54
        {0b0111000,0b0100100,0b0111000,0b0100000,0b0100000}, // P→ (для <>)
    };

    private static int[] glyphFor(char c) {
        c = Character.toUpperCase(c);
        switch (c) {
            case ' ':  return GLYPHS[0];
            case '.':  return GLYPHS[37];
            case ',':  return GLYPHS[38];
            case '!':  return GLYPHS[39];
            case '?':  return GLYPHS[40];
            case ':':  return GLYPHS[41];
            case '-':  return GLYPHS[42];
            case '/':  return GLYPHS[43];
            case '\'': return GLYPHS[44];
            case '+':  return GLYPHS[45];
            case '_':  return GLYPHS[46];
            case '"':  return GLYPHS[47];
            case '|':  return GLYPHS[48];
            case '>':  return GLYPHS[49];
            case '<':  return GLYPHS[50];
            case ';':  return GLYPHS[51];
            default:
                if (c >= 'A' && c <= 'Z') return GLYPHS[1 + (c - 'A')];
                if (c >= '0' && c <= '9') return GLYPHS[27 + (c - '0')];
                return GLYPHS[0];
        }
    }

    // ─── IMod ─────────────────────────────────────────────────────────────

    @Override
    public void preInit() {
        System.out.println("[ExampleMod] preInit");

        // Клавиши
        RDLoader.EVENT_BUS.register(Events.KeyPressEvent.class, e -> {
            if (!chatOpen) {
                if (e.key == Keyboard.KEY_T) {
                    openChat();
                    e.cancel();
                }
            } else {
                handleChatKey(e.key);
                e.cancel();
            }
        });

        // Рендер overlay (и повторный setGrabbed(false) чтобы RubyDung не перехватывал)
        RDLoader.EVENT_BUS.register(Events.GameRenderEvent.class, e -> {
            if (chatOpen) {
                // RubyDung вызывает setGrabbed(true) в keyboard loop —
                // восстанавливаем наш ungrab здесь каждый кадр
                Mouse.setGrabbed(false);
            }
            if (chatOpen || !history.isEmpty()) {
                renderChatOverlay();
            }
        });
    }

    @Override
    public void init(Object game) {
        System.out.println("[ExampleMod] init — нажми T чтобы открыть чат");
    }

    @Override
    public void postInit() {}

    // ─── Логика чата ──────────────────────────────────────────────────────

    private void openChat() {
        chatOpen = true;
        inputBuf.setLength(0);
        cursorTimer = System.currentTimeMillis();
        Mouse.setGrabbed(false);
    }

    private void closeChat() {
        chatOpen = false;
        Mouse.setGrabbed(true);
    }

    private void sendMessage() {
        String msg = inputBuf.toString().trim();
        if (!msg.isEmpty()) {
            addToHistory("<You> " + msg);
            System.out.println("[Chat] <You> " + msg);
        }
        closeChat();
    }

    private void addToHistory(String line) {
        history.add(line);
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    private void handleChatKey(int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            closeChat();
        } else if (key == Keyboard.KEY_RETURN) {
            sendMessage();
        } else if (key == Keyboard.KEY_BACK) {
            if (inputBuf.length() > 0) {
                inputBuf.deleteCharAt(inputBuf.length() - 1);
            }
        } else {
            // getEventCharacter() актуален — мы внутри Keyboard.next()
            char c = Keyboard.getEventCharacter();
            if (c >= 32 && c < 127 && inputBuf.length() < MAX_INPUT) {
                inputBuf.append(c);
            }
        }
    }

    // ─── Рендер 2D overlay ────────────────────────────────────────────────

    private static final int SCALE = 2;             // пикселей экрана на пиксель шрифта
    private static final int CHAR_W = (5 + 1) * SCALE; // ширина символа с зазором
    private static final int CHAR_H = 7 * SCALE;       // высота символа
    private static final int PAD_X = 4;
    private static final int PAD_Y = 3;

    private void renderChatOverlay() {
        int sw = Display.getWidth();
        int sh = Display.getHeight();
        if (sw <= 0 || sh <= 0) return;

        // Переход в 2D
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, sw, sh, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tesselator t = Tesselator.instance;
        int lineH = CHAR_H + PAD_Y * 2;
        int inputY = sh - lineH - 2;

        // ── История ─────────────────────────────────────────────────
        int visCount = history.size();
        if (!chatOpen) visCount = Math.min(visCount, 5);

        for (int i = 0; i < visCount; i++) {
            int lineIdx = history.size() - visCount + i;
            String line = history.get(lineIdx);
            int y = inputY - (visCount - i) * (lineH + 1);

            float bgAlpha = chatOpen ? 0.55f : 0.35f;
            int textW = line.length() * CHAR_W + PAD_X * 2;
            fillRect(t, PAD_X - 2, y, textW, lineH, 0, 0, 0, bgAlpha);
            drawString(t, line, PAD_X, y + PAD_Y, 1f, 1f, 1f);
        }

        // ── Поле ввода ───────────────────────────────────────────────
        if (chatOpen) {
            fillRect(t, 2, inputY, sw - 4, lineH, 0, 0, 0, 0.72f);
            // рамка
            float rw = 1;
            fillRect(t, 2, inputY,              sw - 4, rw,     0.5f, 0.5f, 0.5f, 1f);
            fillRect(t, 2, inputY + lineH - rw, sw - 4, rw,     0.5f, 0.5f, 0.5f, 1f);
            fillRect(t, 2, inputY,              rw,     lineH,  0.5f, 0.5f, 0.5f, 1f);
            fillRect(t, sw - 2 - rw, inputY,   rw,     lineH,  0.5f, 0.5f, 0.5f, 1f);

            // текст
            String text = inputBuf.toString();
            drawString(t, text, PAD_X + 2, inputY + PAD_Y, 1f, 1f, 1f);

            // курсор
            boolean showCursor = ((System.currentTimeMillis() - cursorTimer) / 500) % 2 == 0;
            if (showCursor) {
                float cx = PAD_X + 2 + text.length() * CHAR_W;
                fillRect(t, cx, inputY + PAD_Y, SCALE, CHAR_H, 1f, 1f, 1f, 1f);
            }

            // подсказка справа
            String hint = "[T] Chat  [Enter] Send  [Esc] Close";
            float hx = sw - hint.length() * CHAR_W - PAD_X;
            if (hx > text.length() * CHAR_W + PAD_X + 20) {
                drawString(t, hint, hx, inputY + PAD_Y, 0.6f, 0.6f, 0.6f);
            }
        }

        // Восстановление GL
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    // ─── GL-примитивы ────────────────────────────────────────────────────

    private void fillRect(Tesselator t, float x, float y, float w, float h,
                          float r, float g, float b, float a) {
        t.init(7); // GL_QUADS = 7
        t.color(r, g, b, a);
        t.vertex(x,     y,     0);
        t.vertex(x,     y + h, 0);
        t.vertex(x + w, y + h, 0);
        t.vertex(x + w, y,     0);
        t.flush();
    }

    private void drawString(Tesselator t, String text, float x, float y,
                            float r, float g, float b) {
        float cx = x;
        for (int i = 0; i < text.length(); i++) {
            drawGlyph(t, text.charAt(i), cx, y, r, g, b);
            cx += CHAR_W;
        }
    }

    private void drawGlyph(Tesselator t, char c, float x, float y,
                           float r, float g, float b) {
        int[] glyph = glyphFor(c);
        for (int col = 0; col < 5; col++) {
            int bits = glyph[col];
            if (bits == 0) continue;
            for (int row = 0; row < 7; row++) {
                if ((bits & (1 << row)) != 0) {
                    float px = x + col * SCALE;
                    float py = y + row * SCALE;
                    fillRect(t, px, py, SCALE, SCALE, r, g, b, 1f);
                }
            }
        }
    }
}
