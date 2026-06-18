package com.skincamo.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.skincamo.capability.BodyPart;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Animação leve e puramente cosmética (não afeta o estado "real" salvo no
 * servidor): revela a nova cor da parte do corpo progressivamente, linha por
 * linha, em ~6 ticks (0.3s), simulando tinta se espalhando.
 *
 * É registrada no ClientTickHandler e some sozinha quando termina, então o
 * custo em jogadores parados é zero.
 */
public final class InkAnimationManager {

    private InkAnimationManager() {}

    private static final int DURATION_TICKS = 6;

    private static class Job {
        final UUID playerId;
        final BodyPart part;
        final int color;
        int ticksDone = 0;

        Job(UUID playerId, BodyPart part, int color) {
            this.playerId = playerId;
            this.part = part;
            this.color = color;
        }
    }

    private static final List<Job> ACTIVE = new CopyOnWriteArrayList<>();

    public static void start(UUID playerId, BodyPart part, int color) {
        ACTIVE.add(new Job(playerId, part, color));
    }

    /** Chamado uma vez por tick de cliente (ver ClientEvents). */
    public static void tick() {
        if (ACTIVE.isEmpty()) return;

        List<Job> finished = new ArrayList<>();
        for (Job job : ACTIVE) {
            job.ticksDone++;
            float progress = (float) job.ticksDone / DURATION_TICKS;

            DynamicTexture texture = SkinCamoTextureManager.getTexture(job.playerId);
            if (texture != null && texture.getPixels() != null) {
                NativeImage image = texture.getPixels();
                SkinPixelMap.fillPartProgressive(image, job.part, job.color, progress);
                texture.upload();
            }

            if (progress >= 1f) {
                finished.add(job);
            }
        }
        ACTIVE.removeAll(finished);
    }
}
