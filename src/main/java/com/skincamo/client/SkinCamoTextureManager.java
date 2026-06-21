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
 *  - Atualizações pintam SOMENTE a região (retângulos UV) ou o pixel alterado
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
    private static final Map<UUID, int[]> PENDING_FULL_IMAGE = new ConcurrentHashMap<>();
    /** Cache só para a GUI mostrar "a última cor sólida usada" - não é a fonte da verdade. */
    private static final Map<UUID, Map<BodyPart, Integer>> LAST_PART_COLORS = new ConcurrentHashMap<>();

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

        // Se já recebemos a imagem completa desse jogador antes do model ser
        // renderizado por aqui (ex.: chegou o FullSkinSyncPacket antes do
        // jogador entrar no campo de visão), aplicamos agora.
        int[] pending = PENDING_FULL_IMAGE.remove(id);
        if (pending != null) {
            writeFullImage(texture, pending);
        }
        return loc;
    }

    public static boolean hasTexture(UUID id) {
        return TEXTURES.containsKey(id);
    }

    /** Sincronização completa (login): substitui os 64x64 pixels de uma vez, 1 upload só. */
    public static void applyFullImage(UUID id, int[] pixels) {
        DynamicTexture texture = TEXTURES.get(id);
        if (texture == null || texture.getPixels() == null) {
            // Textura ainda não existe (jogador fora do campo de visão) - guarda
            // para aplicar no momento em que getOrCreateLocation() for chamado.
            PENDING_FULL_IMAGE.put(id, pixels);
            return;
        }
        writeFullImage(texture, pixels);
    }

    private static void writeFullImage(DynamicTexture texture, int[] pixels) {
        NativeImage image = texture.getPixels();
        if (image == null) return;
        SkinPixelMap.writeFullImage(image, pixels, SkinTextureFactory.SIZE, SkinTextureFactory.SIZE);
        texture.upload();
    }

    /** Preenche toda a skin (todas as partes) com uma cor sólida - "Preencher Tudo". */
    public static void applyAllColor(UUID id, int rgb) {
        for (BodyPart part : BodyPart.VALUES) {
            applyPartColor(id, part, rgb);
        }
    }

    /** Preenche só UMA parte (mais barato: usado pela pintura normal, atualiza só a região modificada). */
    public static void applyPartColor(UUID id, BodyPart part, int rgb) {
        LAST_PART_COLORS.computeIfAbsent(id, k -> new EnumMap<>(BodyPart.class)).put(part, rgb);

        DynamicTexture texture = TEXTURES.get(id);
        if (texture == null || texture.getPixels() == null) return;

        SkinPixelMap.fillPart(texture.getPixels(), part, rgb);
        texture.upload();
    }

    /** Pintura livre, pixel a pixel - usada pelo Modo Pincel 3D. */
    public static void setPixel(UUID id, int x, int y, int rgb) {
        DynamicTexture texture = TEXTURES.get(id);
        if (texture == null || texture.getPixels() == null) return;

        SkinPixelMap.writePixel(texture.getPixels(), x, y, rgb);
        texture.upload();
    }

    /** Hint de UI (não é fonte da verdade): última cor sólida aplicada numa parte, se houver. */
    public static Integer getLastColor(UUID id, BodyPart part) {
        Map<BodyPart, Integer> cache = LAST_PART_COLORS.get(id);
        return cache == null ? null : cache.get(part);
    }

    public static DynamicTexture getTexture(UUID id) {
        return TEXTURES.get(id);
    }
}
