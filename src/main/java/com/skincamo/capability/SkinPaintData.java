package com.skincamo.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

/**
 * Estado "fonte da verdade" da pintura de um jogador.
 * Vive no servidor (anexado ao Player via Capability) e é persistido junto
 * com os dados do jogador (o Forge serializa capabilities automaticamente
 * dentro do NBT do jogador, então isso já cobre o requisito de persistência
 * entre sessões sem precisar de um arquivo separado).
 */
public class SkinPaintData {

    public static final int DEFAULT_COLOR = 0xFFFFFF; // branco, RGB sem alpha

    private final Map<BodyPart, Integer> colors = new EnumMap<>(BodyPart.class);

    public SkinPaintData() {
        for (BodyPart part : BodyPart.VALUES) {
            colors.put(part, DEFAULT_COLOR);
        }
    }

    public int getColor(BodyPart part) {
        return colors.getOrDefault(part, DEFAULT_COLOR);
    }

    public void setColor(BodyPart part, int rgb) {
        colors.put(part, rgb & 0xFFFFFF);
    }

    public void fillAll(int rgb) {
        for (BodyPart part : BodyPart.VALUES) {
            colors.put(part, rgb & 0xFFFFFF);
        }
    }

    public Map<BodyPart, Integer> asMap() {
        return colors;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (BodyPart part : BodyPart.VALUES) {
            tag.putInt(part.name(), getColor(part));
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        for (BodyPart part : BodyPart.VALUES) {
            if (tag.contains(part.name())) {
                colors.put(part, tag.getInt(part.name()));
            }
        }
    }
}
