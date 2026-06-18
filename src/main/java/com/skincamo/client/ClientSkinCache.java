package com.skincamo.client;

import com.skincamo.capability.BodyPart;

import java.util.Map;
import java.util.UUID;

/**
 * Ponto de entrada client-side para os pacotes SyncSkinDataPacket.
 * Compara com o que já tínhamos: se só uma parte mudou, dispara a animação
 * de "tinta se espalhando" (efeito visual opcional). Se for uma sincronização
 * em massa (ex.: jogador acabou de logar), aplica tudo de uma vez sem animação
 * (mais barato e evita um monte de animações simultâneas ao entrar no mundo).
 */
public final class ClientSkinCache {

    private ClientSkinCache() {}

    public static void apply(UUID playerId, Map<BodyPart, Integer> newColors) {
        int changedParts = 0;
        BodyPart changedPart = null;
        int changedColor = 0;

        for (Map.Entry<BodyPart, Integer> entry : newColors.entrySet()) {
            Integer previous = SkinCamoTextureManager.getLastColor(playerId, entry.getKey());
            if (previous == null || !previous.equals(entry.getValue())) {
                changedParts++;
                changedPart = entry.getKey();
                changedColor = entry.getValue();
            }
        }

        if (changedParts == 1 && SkinCamoTextureManager.hasTexture(playerId)) {
            InkAnimationManager.start(playerId, changedPart, changedColor);
        } else {
            SkinCamoTextureManager.applyColorsImmediate(playerId, newColors);
        }
    }
}
