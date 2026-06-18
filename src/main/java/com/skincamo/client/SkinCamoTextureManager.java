package com.skincamo.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.skincamo.capability.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Núcleo do "Sistema de Renderização" pedido na spec:
 *  - Uma DynamicTexture (com NativeImage) por jogador, criada uma única vez.
 *  - Atualizações pintam SOMENTE a região (retângulos UV) da parte alterada
 *    e fazem upload incremental (texture.upload() apenas reenvia o buffer já
 *    modificado para a GPU; não recriamos a NativeImage do zero).
 *  - A troca de "qual textura o jogo usa para esse jogador" é feita pelo
 *    PlayerRendererMixin, que chama getOrCreateLocation(...) e retorna o
 *    ResourceLocation único que registramos aqui.
 */
public final class SkinCamoTextureManager {

    private SkinCamoTextureManager() {}

    private static final Map<UUID, DynamicTexture> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceLocation> LOCATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<BodyPart, Integer>> LAST_COLORS = new ConcurrentHashMap<>();

    /** Chamado pelo mixin de renderização para todo jogador visível. Lazy: cria só na primeira vez. */
    public static ResourceLocation getOrCreateLocation(AbstractClientPlayer player) {
        UUID id = player.getUUID();
        ResourceLocation existing = LOCATIONS.get(id);
        if (existing != null) {
            return existing;
        }
        ResourceLocation loc = new ResourceLocation("skincamo",
                "dynamic_skin_" + id.toString().replace("-", ""));
        DynamicTexture texture = SkinTextureFactory.createBlank();
        Minecraft.getInstance().getTextureManager().register(loc, texture);

        TEXTURES.put(id, texture);
        LOCATIONS.put(id, loc);

        // Se já recebemos dados de pintura desse jogador antes do model ser renderizado
        // (ex.: chegou um SyncSkinDataPacket antes do jogador entrar no campo de visão),
        // aplicamos agora.
        Map<BodyPart, Integer> pending = LAST_COLORS.get(id);
        if (pending != null) {
            applyColorsImmediate(id, pending);
        }
        return loc;
    }

    public static boolean hasTexture(UUID id) {
        return TEXTURES.containsKey(id);
    }

    /** Aplica um conjunto de cores instantaneamente (sem animação), usado na sincronização de rede. */
    public static void applyColorsImmediate(UUID id, Map<BodyPart, Integer> colors) {
        LAST_COLORS.put(id, new EnumMap<>(colors));
        DynamicTexture texture = TEXTURES.get(id);
        if (texture == null || texture.getPixels() == null) return;

        NativeImage image = texture.getPixels();
        for (Map.Entry<BodyPart, Integer> entry : colors.entrySet()) {
            SkinPixelMap.fillPart(image, entry.getKey(), entry.getValue());
        }
        texture.upload();
    }

    /** Aplica só UMA parte (mais barato: usado pela pintura normal, atualiza só a região modificada). */
    public static void applyPartColor(UUID id, BodyPart part, int rgb) {
        Map<BodyPart, Integer> cache = LAST_COLORS.computeIfAbsent(id, k -> new EnumMap<>(BodyPart.class));
        cache.put(part, rgb);

        DynamicTexture texture = TEXTURES.get(id);
        if (texture == null || texture.getPixels() == null) return;

        SkinPixelMap.fillPart(texture.getPixels(), part, rgb);
        texture.upload();
    }

    public static Integer getLastColor(UUID id, BodyPart part) {
        Map<BodyPart, Integer> cache = LAST_COLORS.get(id);
        return cache == null ? null : cache.get(part);
    }

    public static DynamicTexture getTexture(UUID id) {
        return TEXTURES.get(id);
    }
}
