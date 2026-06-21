package com.skincamo.client.brush;

import com.skincamo.capability.BodyPart;
import com.skincamo.capability.SkinUvLayout;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Testa um raio (origem + direção, em coordenadas do mundo) contra uma
 * versão simplificada da geometria do PRÓPRIO jogador (ver
 * {@link PlayerBodyGeometry}), pra saber em qual pixel da skin 64x64 esse
 * raio "tocaria".
 *
 * O Minecraft normalmente nunca testa o raio de mira contra o seu próprio
 * jogador (faria você "minerar a si mesmo" por engano), então esse teste
 * não existe em nenhum lugar do jogo - é todo escrito aqui.
 *
 * Algoritmo: para cada uma das 6 partes do corpo, projeta o raio nos 3 eixos
 * locais do corpo (right/up/forward, ver PlayerBodyGeometry) e faz o
 * clássico teste de interseção raio-vs-caixa por "slabs" (1 por eixo),
 * guardando não só SE bateu, mas em qual das 6 faces e em que ponto 2D
 * dessa face — para então converter isso num pixel exato usando as mesmas
 * tabelas de UV que o resto do mod já usa ({@link SkinUvLayout}).
 */
public final class BodyRaycaster {

    private BodyRaycaster() {}

    /** Alcance máximo (em blocos) do pincel - parecido com o alcance normal de interação. */
    private static final double MAX_DISTANCE = 6.0;

    public static final class Hit {
        public final BodyPart part;
        public final int pixelX, pixelY;
        public final Vec3 worldPoint;

        Hit(BodyPart part, int pixelX, int pixelY, Vec3 worldPoint) {
            this.part = part;
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.worldPoint = worldPoint;
        }
    }

    public static Optional<Hit> raycast(AbstractClientPlayer player, float partialTick, Vec3 rayOrigin, Vec3 rayDir) {
        Vec3 root = PlayerBodyGeometry.rootWorldPosition(player, partialTick);
        Vec3 forward = PlayerBodyGeometry.bodyForward(player, partialTick);
        Vec3 up = PlayerBodyGeometry.worldUp();
        Vec3 right = PlayerBodyGeometry.bodyRight(forward, up);

        Vec3 relOrigin = rayOrigin.subtract(root);
        double oR = relOrigin.dot(right);
        double oU = relOrigin.dot(up);
        double oF = relOrigin.dot(forward);
        double dR = rayDir.dot(right);
        double dU = rayDir.dot(up);
        double dF = rayDir.dot(forward);

        BodyPart bestPart = null;
        int bestFace = -1;
        double bestT = Double.MAX_VALUE;
        double bestU = 0, bestV = 0;

        for (BodyPart part : BodyPart.VALUES) {
            PlayerBodyGeometry.Box box = PlayerBodyGeometry.boxFor(part);
            double[] result = intersectBox(oR, oU, oF, dR, dU, dF, box);
            if (result == null) continue;

            double t = result[0];
            if (t >= 0 && t <= MAX_DISTANCE && t < bestT) {
                bestT = t;
                bestPart = part;
                bestFace = (int) result[1];
                bestU = result[2];
                bestV = result[3];
            }
        }

        if (bestPart == null) return Optional.empty();

        int[] rect = SkinUvLayout.faceRect(bestPart, bestFace);
        int px = rect[0] + clampIndex((int) Math.floor(bestU * rect[2]), rect[2]);
        int py = rect[1] + clampIndex((int) Math.floor(bestV * rect[3]), rect[3]);

        Vec3 worldPoint = rayOrigin.add(rayDir.scale(bestT));
        return Optional.of(new Hit(bestPart, px, py, worldPoint));
    }

    private static int clampIndex(int v, int size) {
        if (v < 0) return 0;
        if (v >= size) return size - 1;
        return v;
    }

    /**
     * Testa o raio (já projetado nos 3 eixos locais) contra uma caixa
     * axis-aligned. Retorna {t, face, u, v} do hit mais próximo, ou null
     * se o raio não cruza a caixa. "face" usa os mesmos índices de
     * {@link SkinUvLayout} (TOP=0, BOTTOM=1, RIGHT=2, FRONT=3, LEFT=4, BACK=5).
     */
    private static double[] intersectBox(double oR, double oU, double oF, double dR, double dU, double dF,
                                          PlayerBodyGeometry.Box box) {
        double tEntry = Double.NEGATIVE_INFINITY;
        double tExit = Double.POSITIVE_INFINITY;
        int enterAxis = -1; // 0=right, 1=up, 2=forward
        boolean enterMax = false;

        double[] slabR = slab(oR, dR, box.rMin, box.rMax);
        if (slabR == null) return null;
        if (slabR[0] > tEntry) {
            tEntry = slabR[0];
            enterAxis = 0;
            enterMax = slabR[2] > 0;
        }
        tExit = Math.min(tExit, slabR[1]);

        double[] slabU = slab(oU, dU, box.uMin, box.uMax);
        if (slabU == null) return null;
        if (slabU[0] > tEntry) {
            tEntry = slabU[0];
            enterAxis = 1;
            enterMax = slabU[2] > 0;
        }
        tExit = Math.min(tExit, slabU[1]);

        double[] slabF = slab(oF, dF, box.fMin, box.fMax);
        if (slabF == null) return null;
        if (slabF[0] > tEntry) {
            tEntry = slabF[0];
            enterAxis = 2;
            enterMax = slabF[2] > 0;
        }
        tExit = Math.min(tExit, slabF[1]);

        if (enterAxis == -1 || tEntry > tExit || tExit < 0) return null;

        double hitR = oR + dR * tEntry;
        double hitU = oU + dU * tEntry;
        double hitF = oF + dF * tEntry;

        int face;
        double u, v;
        if (enterAxis == 1) {
            face = enterMax ? SkinUvLayout.TOP : SkinUvLayout.BOTTOM;
            u = normalize(hitR, box.rMin, box.rMax);
            v = normalize(hitF, box.fMin, box.fMax);
        } else if (enterAxis == 0) {
            face = enterMax ? SkinUvLayout.RIGHT : SkinUvLayout.LEFT;
            u = normalize(hitF, box.fMin, box.fMax);
            v = 1.0 - normalize(hitU, box.uMin, box.uMax);
        } else {
            face = enterMax ? SkinUvLayout.FRONT : SkinUvLayout.BACK;
            u = normalize(hitR, box.rMin, box.rMax);
            v = 1.0 - normalize(hitU, box.uMin, box.uMax);
        }

        return new double[]{tEntry, face, clamp01(u), clamp01(v)};
    }

    /** Slab 1D: retorna {tNear, tFar, sinalDoLadoDeEntrada} ou null se não houver interseção nesse eixo. */
    private static double[] slab(double o, double d, double min, double max) {
        if (Math.abs(d) < 1e-9) {
            if (o < min || o > max) return null;
            return new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0};
        }
        double t1 = (min - o) / d;
        double t2 = (max - o) / d;
        if (t1 < t2) {
            return new double[]{t1, t2, -1};
        } else {
            return new double[]{t2, t1, 1};
        }
    }

    private static double normalize(double v, double min, double max) {
        if (max - min < 1e-9) return 0.5;
        return (v - min) / (max - min);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
