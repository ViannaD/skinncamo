package com.skincamo.client;

import com.skincamo.capability.BodyPart;

import java.util.UUID;

/**
 * Ponto de entrada client-side para os pacotes de rede que atualizam a
 * aparência de QUALQUER jogador (não só o próprio):
 *  - applyPartDelta / applyAllDelta: vêm do SkinDeltaPacket (preenchimento sólido).
 *  - applyPixelDelta: vem do SkinDeltaPacket no caso de pintura livre (Modo Pincel 3D).
 *  - applyFullImage: vem do FullSkinSyncPacket (sincronização completa ao entrar no mundo).
 */
public final class ClientSkinCache {

    private ClientSkinCache() {}

    public static void applyPartDelta(UUID playerId, BodyPart part, int rgb) {
        if (SkinCamoTextureManager.hasTexture(playerId)) {
            InkAnimationManager.start(playerId, part, rgb);
        } else {
            SkinCamoTextureManager.applyPartColor(playerId, part, rgb);
        }
    }

    public static void applyAllDelta(UUID playerId, int rgb) {
        SkinCamoTextureManager.applyAllColor(playerId, rgb);
    }

    /** Pintura livre, pixel a pixel - sem animação (igual a uma ferramenta de pixel art de verdade). */
    public static void applyPixelDelta(UUID playerId, int x, int y, int rgb) {
        SkinCamoTextureManager.setPixel(playerId, x, y, rgb);
    }

    public static void applyFullImage(UUID playerId, int[] pixels) {
        SkinCamoTextureManager.applyFullImage(playerId, pixels);
    }
}
