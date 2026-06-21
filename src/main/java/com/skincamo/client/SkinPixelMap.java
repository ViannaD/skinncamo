package com.skincamo.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.skincamo.capability.BodyPart;
import com.skincamo.capability.SkinUvLayout;

/**
 * Camada client-only que sabe escrever pixels numa NativeImage. A tabela de
 * coordenadas UV em si mora em {@link SkinUvLayout} (pacote "capability",
 * comum/server-safe) — aqui só ficam os métodos que de fato tocam em
 * NativeImage, para não vazar dependência de classes de renderização para
 * o lado do servidor dedicado.
 */
public final class SkinPixelMap {

    private SkinPixelMap() {}

    /** Preenche todos os pixels de uma parte com a cor opaca dada (não toca no overlay). */
    public static void fillPart(NativeImage image, BodyPart part, int rgbOpaque) {
        for (int[] rect : SkinUvLayout.BASE_LAYER.get(part)) {
            fillRect(image, rect, rgbOpaque);
        }
    }

    /**
     * Preenche progressivamente uma fração [0..1] das linhas de cada retângulo da parte.
     * Usado pela animação de "tinta se espalhando".
     */
    public static void fillPartProgressive(NativeImage image, BodyPart part, int rgbOpaque, float progress) {
        progress = Math.max(0f, Math.min(1f, progress));
        for (int[] rect : SkinUvLayout.BASE_LAYER.get(part)) {
            int rows = Math.max(1, Math.round(rect[3] * progress));
            fillRect(image, new int[]{rect[0], rect[1], rect[2], rows}, rgbOpaque);
        }
    }

    public static void clearOverlay(NativeImage image) {
        for (int[] rect : SkinUvLayout.OVERLAY_LAYER) {
            fillRect(image, rect, 0x00000000, true);
        }
    }

    /** Escreve a imagem inteira (64x64) de uma vez a partir de um buffer 0xRRGGBB linha-a-linha. */
    public static void writeFullImage(NativeImage image, int[] rgbPixels, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixelRGBA(x, y, toAbgrOpaque(rgbPixels[y * width + x]));
            }
        }
    }

    /** Escreve um único pixel (usado pelo Modo Pincel 3D, pintura livre pixel a pixel). */
    public static void writePixel(NativeImage image, int x, int y, int rgbOpaque) {
        image.setPixelRGBA(x, y, toAbgrOpaque(rgbOpaque));
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
