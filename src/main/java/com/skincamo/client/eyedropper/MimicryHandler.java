package com.skincamo.client.eyedropper;

import com.skincamo.client.gui.PaintActions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Modo "Mimetismo": enquanto ativo, fica re-amostrando os blocos ao redor do
 * jogador periodicamente e repintando a skin inteira automaticamente.
 *
 * Cuidados de desempenho (requisito "baixo tráfego de rede / baixo uso de
 * memória" da spec):
 *  - Só roda 1x a cada {@link #SAMPLE_INTERVAL_TICKS} ticks (não todo tick).
 *  - Só dispara reamostragem se o jogador realmente se moveu uma distância
 *    mínima desde a última amostra (evita reprocessar parado).
 *  - Só reenvia/repaint se a cor calculada for "suficientemente diferente"
 *    da última aplicada (limiar de distância em RGB), evitando pacotes de
 *    rede e uploads de textura por mudanças imperceptíveis.
 */
public final class MimicryHandler {

    private MimicryHandler() {}

    private static volatile boolean active = false;

    private static final int SAMPLE_INTERVAL_TICKS = 10; // ~0.5s a 20 ticks/seg
    private static final double MIN_MOVE_DISTANCE_SQR = 0.75 * 0.75;
    private static final int COLOR_CHANGE_THRESHOLD = 10; // distância simples em R+G+B

    private static int tickCounter = 0;
    private static Vec3 lastSamplePos = null;
    private static Integer lastAppliedColor = null;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
        if (!active) {
            lastSamplePos = null;
        }
    }

    public static void toggle() {
        setActive(!active);
    }

    public static void tick(Minecraft mc) {
        if (!active) return;
        if (mc.player == null || mc.level == null) return;

        tickCounter++;
        if (tickCounter < SAMPLE_INTERVAL_TICKS) return;
        tickCounter = 0;

        Vec3 currentPos = mc.player.position();
        if (lastSamplePos != null && currentPos.distanceToSqr(lastSamplePos) < MIN_MOVE_DISTANCE_SQR) {
            return; // jogador praticamente não se moveu, não vale a pena reamostrar
        }
        lastSamplePos = currentPos;

        Optional<Integer> averaged = ColorSampler.averageNearbyBlocks(mc, 3);
        averaged.ifPresent(rgb -> {
            if (lastAppliedColor == null || colorDistance(lastAppliedColor, rgb) > COLOR_CHANGE_THRESHOLD) {
                lastAppliedColor = rgb;
                PaintActions.fillWholeSkin(rgb);
            }
        });
    }

    private static int colorDistance(int a, int b) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return Math.abs(ar - br) + Math.abs(ag - bg) + Math.abs(ab - bb);
    }
}
