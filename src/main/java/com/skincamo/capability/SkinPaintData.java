package com.skincamo.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;

/**
 * Estado "fonte da verdade" da pintura de um jogador.
 * Vive no servidor (anexado ao Player via Capability) e é persistido junto
 * com os dados do jogador (o Forge serializa capabilities automaticamente
 * dentro do NBT do jogador, então isso já cobre o requisito de persistência
 * entre sessões sem precisar de um arquivo separado).
 *
 * Guardamos a skin como um BITMAP completo 64x64 (não mais "uma cor por
 * parte"), para suportar tanto o preenchimento de parte/skin inteira (botões
 * da GUI) quanto a pintura livre pixel a pixel (Modo Pincel 3D, mirando no
 * próprio corpo em terceira pessoa).
 */
public class SkinPaintData {

    public static final int DEFAULT_COLOR = 0xFFFFFF; // branco, RGB sem alpha
    public static final int SIZE = 64;

    private final int[] pixels = new int[SIZE * SIZE];

    public SkinPaintData() {
        java.util.Arrays.fill(pixels, DEFAULT_COLOR);
    }

    public int getPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return DEFAULT_COLOR;
        return pixels[y * SIZE + x];
    }

    public void setPixel(int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return;
        pixels[y * SIZE + x] = rgb & 0xFFFFFF;
    }

    /** Preenche todos os retângulos (6 faces) de uma parte do corpo com uma cor sólida. */
    public void setPartColor(BodyPart part, int rgb) {
        for (int[] rect : SkinUvLayout.BASE_LAYER.get(part)) {
            fillRect(rect, rgb);
        }
    }

    /** Preenche a skin inteira (todas as partes) com uma única cor sólida. */
    public void fillAll(int rgb) {
        for (BodyPart part : BodyPart.VALUES) {
            setPartColor(part, rgb);
        }
    }

    /** Cópia direta do buffer (64x64 = 4096 ints, 0xRRGGBB cada). Usado pela sincronização completa. */
    public int[] snapshot() {
        return pixels.clone();
    }

    public void loadSnapshot(int[] data) {
        if (data == null || data.length != pixels.length) return;
        System.arraycopy(data, 0, pixels, 0, pixels.length);
    }

    private void fillRect(int[] rect, int rgb) {
        int color = rgb & 0xFFFFFF;
        for (int y = rect[1]; y < rect[1] + rect[3]; y++) {
            for (int x = rect[0]; x < rect[0] + rect[2]; x++) {
                setPixel(x, y, color);
            }
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("pixels", new IntArrayTag(pixels));
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("pixels")) {
            loadSnapshot(tag.getIntArray("pixels"));
        }
    }
}
