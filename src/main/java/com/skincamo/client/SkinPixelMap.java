package com.skincamo.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.skincamo.capability.BodyPart;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Coordenadas UV do template padrão de skin 64x64 (formato usado desde o MC 1.8).
 * Cada parte do corpo tem 6 faces na camada BASE (skin) e 6 faces na camada
 * OVERLAY (jaqueta/mangas/calças - o "segundo layer" do modelo do jogador).
 *
 * Mantemos a camada overlay sempre TRANSPARENTE (alpha 0) para não duplicar
 * geometria por cima da skin pintada (senão pareceria um boneco "inchado" branco
 * por fora da cor escolhida).
 *
 * Retângulos no formato {x, y, largura, altura}.
 */
public final class SkinPixelMap {

    private SkinPixelMap() {}

    public static final Map<BodyPart, List<int[]>> BASE_LAYER = new EnumMap<>(BodyPart.class);
    public static final List<int[]> OVERLAY_LAYER = List.of(
            // Hat (cabeça)
            new int[]{32, 0, 8, 8}, new int[]{40, 0, 8, 8},
            new int[]{32, 8, 8, 8}, new int[]{40, 8, 8, 8}, new int[]{48, 8, 8, 8}, new int[]{56, 8, 8, 8},
            // Jacket (tronco)
            new int[]{20, 32, 8, 4}, new int[]{28, 32, 8, 4},
            new int[]{16, 36, 4, 12}, new int[]{20, 36, 8, 12}, new int[]{28, 36, 4, 12}, new int[]{32, 36, 8, 12},
            // Right sleeve (braço direito)
            new int[]{44, 32, 4, 4}, new int[]{48, 32, 4, 4},
            new int[]{40, 36, 4, 12}, new int[]{44, 36, 4, 12}, new int[]{48, 36, 4, 12}, new int[]{52, 36, 4, 12},
            // Left sleeve (braço esquerdo)
            new int[]{52, 48, 4, 4}, new int[]{56, 48, 4, 4},
            new int[]{48, 52, 4, 12}, new int[]{52, 52, 4, 12}, new int[]{56, 52, 4, 12}, new int[]{60, 52, 4, 12},
            // Right pant (perna direita)
            new int[]{4, 32, 4, 4}, new int[]{8, 32, 4, 4},
            new int[]{0, 36, 4, 12}, new int[]{4, 36, 4, 12}, new int[]{8, 36, 4, 12}, new int[]{12, 36, 4, 12},
            // Left pant (perna esquerda)
            new int[]{20, 48, 4, 4}, new int[]{24, 48, 4, 4},
            new int[]{16, 52, 4, 12}, new int[]{20, 52, 4, 12}, new int[]{24, 52, 4, 12}, new int[]{28, 52, 4, 12}
    );

    static {
        BASE_LAYER.put(BodyPart.HEAD, List.of(
                new int[]{8, 0, 8, 8}, new int[]{16, 0, 8, 8},   // top, bottom
                new int[]{0, 8, 8, 8}, new int[]{8, 8, 8, 8}, new int[]{16, 8, 8, 8}, new int[]{24, 8, 8, 8} // right, front, left, back
        ));
        BASE_LAYER.put(BodyPart.BODY, List.of(
                new int[]{20, 16, 8, 4}, new int[]{28, 16, 8, 4},
                new int[]{16, 20, 4, 12}, new int[]{20, 20, 8, 12}, new int[]{28, 20, 4, 12}, new int[]{32, 20, 8, 12}
        ));
        BASE_LAYER.put(BodyPart.RIGHT_ARM, List.of(
                new int[]{44, 16, 4, 4}, new int[]{48, 16, 4, 4},
                new int[]{40, 20, 4, 12}, new int[]{44, 20, 4, 12}, new int[]{48, 20, 4, 12}, new int[]{52, 20, 4, 12}
        ));
        BASE_LAYER.put(BodyPart.RIGHT_LEG, List.of(
                new int[]{4, 16, 4, 4}, new int[]{8, 16, 4, 4},
                new int[]{0, 20, 4, 12}, new int[]{4, 20, 4, 12}, new int[]{8, 20, 4, 12}, new int[]{12, 20, 4, 12}
        ));
        BASE_LAYER.put(BodyPart.LEFT_LEG, List.of(
                new int[]{20, 48, 4, 4}, new int[]{24, 48, 4, 4},
                new int[]{16, 52, 4, 12}, new int[]{20, 52, 4, 12}, new int[]{24, 52, 4, 12}, new int[]{28, 52, 4, 12}
        ));
        BASE_LAYER.put(BodyPart.LEFT_ARM, List.of(
                new int[]{36, 48, 4, 4}, new int[]{40, 48, 4, 4},
                new int[]{32, 52, 4, 12}, new int[]{36, 52, 4, 12}, new int[]{40, 52, 4, 12}, new int[]{44, 52, 4, 12}
        ));
    }

    /** Preenche todos os pixels de uma parte com a cor opaca dada (não toca no overlay). */
    public static void fillPart(NativeImage image, BodyPart part, int rgbOpaque) {
        for (int[] rect : BASE_LAYER.get(part)) {
            fillRect(image, rect, rgbOpaque);
        }
    }

    /**
     * Preenche progressivamente uma fração [0..1] das linhas de cada retângulo da parte.
     * Usado pela animação de "tinta se espalhando".
     */
    public static void fillPartProgressive(NativeImage image, BodyPart part, int rgbOpaque, float progress) {
        progress = Math.max(0f, Math.min(1f, progress));
        for (int[] rect : BASE_LAYER.get(part)) {
            int rows = Math.max(1, Math.round(rect[3] * progress));
            fillRect(image, new int[]{rect[0], rect[1], rect[2], rows}, rgbOpaque);
        }
    }

    public static void clearOverlay(NativeImage image) {
        for (int[] rect : OVERLAY_LAYER) {
            fillRect(image, rect, 0x00000000, true);
        }
    }

    private static void fillRect(NativeImage image, int[] rect, int rgbOpaque) {
        fillRect(image, rect, rgbOpaque, false);
    }

    private static void fillRect(NativeImage image, int[] rect, int color, boolean rawArgbWithAlpha) {
        int abgr = rawArgbWithAlpha ? toAbgr(color) : toAbgrOpaque(color);
        for (int y = rect[1]; y < rect[1] + rect[3]; y++) {
            for (int x = rect[0]; x < rect[0] + rect[2]; x++) {
                image.setPixelRGBA(x, y, abgr);
            }
        }
    }

    /** NativeImage espera ABGR (little-endian) e não ARGB. */
    private static int toAbgrOpaque(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (0xFF << 24) | (b << 16) | (g << 8) | r;
    }

    private static int toAbgr(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
