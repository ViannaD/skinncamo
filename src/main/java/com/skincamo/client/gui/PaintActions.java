package com.skincamo.client.gui;

import com.skincamo.capability.BodyPart;
import com.skincamo.client.SkinCamoTextureManager;
import com.skincamo.network.NetworkHandler;
import com.skincamo.network.packet.FillAllPacket;
import com.skincamo.network.packet.PaintPartPacket;
import net.minecraft.client.Minecraft;

/**
 * Centraliza o fluxo "pintar": aplica localmente de forma otimista (resposta
 * instantânea na tela do próprio jogador, sem esperar o servidor) e manda o
 * pacote para o servidor, que é a fonte da verdade e vai retransmitir a
 * confirmação para todo mundo (inclusive o próprio remetente).
 */
public final class PaintActions {

    private PaintActions() {}

    public static void paintPart(BodyPart part, int rgb) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            SkinCamoTextureManager.applyPartColor(mc.player.getUUID(), part, rgb);
        }
        NetworkHandler.CHANNEL.sendToServer(new PaintPartPacket(part, rgb));
        ClientPaintStorage.pushHistory(rgb);
    }

    public static void fillWholeSkin(int rgb) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            for (BodyPart part : BodyPart.VALUES) {
                SkinCamoTextureManager.applyPartColor(mc.player.getUUID(), part, rgb);
            }
        }
        NetworkHandler.CHANNEL.sendToServer(new FillAllPacket(rgb));
        ClientPaintStorage.pushHistory(rgb);
    }
}
