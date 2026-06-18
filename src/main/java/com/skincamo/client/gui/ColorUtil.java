package com.skincamo.client.gui;

/**
 * Conversões de cor isoladas em uma classe sem nenhuma dependência de API do
 * Minecraft (só matemática) — propositalmente simples para minimizar risco
 * de erro de compilação, já que este projeto não pode ser compilado neste
 * ambiente antes de ser entregue.
 */
public final class ColorUtil {

    private ColorUtil() {}

    /** h em graus [0,360), s e v em [0,1]. Retorna 0xRRGGBB. */
    public static int hsvToRgb(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f;
        s = clamp01(s);
        v = clamp01(v);

        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;

        float r, g, b;
        if (h < 60f) { r = c; g = x; b = 0f; }
        else if (h < 120f) { r = x; g = c; b = 0f; }
        else if (h < 180f) { r = 0f; g = c; b = x; }
        else if (h < 240f) { r = 0f; g = x; b = c; }
        else if (h < 300f) { r = x; g = 0f; b = c; }
        else { r = c; g = 0f; b = x; }

        int ri = clampByte(Math.round((r + m) * 255f));
        int gi = clampByte(Math.round((g + m) * 255f));
        int bi = clampByte(Math.round((b + m) * 255f));
        return (ri << 16) | (gi << 8) | bi;
    }

    /** Retorna {h(0-360), s(0-1), v(0-1)} a partir de 0xRRGGBB. */
    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h;
        if (delta == 0f) {
            h = 0f;
        } else if (max == r) {
            h = 60f * (((g - b) / delta) % 6f);
        } else if (max == g) {
            h = 60f * (((b - r) / delta) + 2f);
        } else {
            h = 60f * (((r - g) / delta) + 4f);
        }
        if (h < 0f) h += 360f;

        float s = max == 0f ? 0f : delta / max;
        float v = max;
        return new float[]{h, s, v};
    }

    /** Aceita "RRGGBB" ou "#RRGGBB". Retorna null se inválido. */
    public static Integer parseHex(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() != 6) return null;
        try {
            return Integer.parseInt(t, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toHex(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static int clampByte(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
