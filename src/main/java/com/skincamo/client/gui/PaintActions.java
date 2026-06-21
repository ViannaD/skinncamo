package com.skincamo.client.gui;

import com.skincamo.capability.BodyPart;
import com.skincamo.capability.SkinPaintData;
import com.skincamo.client.SkinCamoTextureManager;
import com.skincamo.network.NetworkHandler;
import com.skincamo.network.packet.FillAllPacket;
import com.skincamo.network.packet.PaintPartPacket;
import com.skincamo.network.packet.PaintPixelPacket;
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
            SkinCamoTextureManager.applyAllColor(mc.player.getUUID(), rgb);
        }
        NetworkHandler.CHANNEL.sendToServer(new FillAllPacket(rgb));
        ClientPaintStorage.pushHistory(rgb);
    }

    /**
     * Pintura livre, pixel a pixel - usada pelo Modo Pincel 3D. brushSize=1
     * pinta só o pixel central; 2 ou 3 pintam um quadradinho centrado nele.
     * Não empurra para o histórico de cores (isso geraria spam de entradas
     * idênticas enquanto a pessoa arrasta o pincel).
     */
    public static void paintPixel(int centerX, int centerY, int rgb, int brushSize) {
        Minecraft mc = Minecraft.getInstance();
        int half = Math.max(0, brushSize - 1) / 2;
        int extra = Math.max(0, brushSize - 1) - half;

        for (int dy = -half; dy <= extra; dy++) {
            for (int dx = -half; dx <= extra; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x < 0 || y < 0 || x >= SkinPaintData.SIZE || y >= SkinPaintData.SIZE) continue;

                if (mc.player != null) {
                    SkinCamoTextureManager.setPixel(mc.player.getUUID(), x, y, rgb);
                }
                NetworkHandler.CHANNEL.sendToServer(new PaintPixelPacket(x, y, rgb));
            }
        }
    }
}
