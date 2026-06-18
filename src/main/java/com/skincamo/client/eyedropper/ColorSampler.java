package com.skincamo.client.eyedropper;

import com.skincamo.client.Eyedropper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class ColorSampler {

    private ColorSampler() {}

    /**
     * Varre um cubo de "radius" blocos ao redor do jogador, pega a MapColor de
     * cada bloco sólido encontrado e calcula a média simples de R, G e B.
     * Mantemos o raio pequeno (padrão 3) para não pesar: isso já dá ~7^3 = 343
     * blocos no pior caso, rápido o suficiente para rodar em um clique de botão
     * ou a cada poucos ticks no modo mimetismo.
     */
    public static Optional<Integer> averageNearbyBlocks(Minecraft mc, int radius) {
        if (mc.player == null || mc.level == null) return Optional.empty();
        Level level = mc.level;
        BlockPos center = mc.player.blockPosition();

        long sumR = 0, sumG = 0, sumB = 0;
        int count = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    Optional<Integer> color = Eyedropper.sampleColorAt(level, pos);
                    if (color.isPresent()) {
                        int rgb = color.get();
                        sumR += (rgb >> 16) & 0xFF;
                        sumG += (rgb >> 8) & 0xFF;
                        sumB += rgb & 0xFF;
                        count++;
                    }
                }
            }
        }

        if (count == 0) return Optional.empty();
        int r = (int) (sumR / count);
        int g = (int) (sumG / count);
        int b = (int) (sumB / count);
        return Optional.of((r << 16) | (g << 8) | b);
    }
}
