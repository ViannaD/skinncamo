package com.skincamo.network;

import com.skincamo.SkinCamoMod;
import com.skincamo.network.packet.FillAllPacket;
import com.skincamo.network.packet.FullSkinSyncPacket;
import com.skincamo.network.packet.PaintPartPacket;
import com.skincamo.network.packet.PaintPixelPacket;
import com.skincamo.network.packet.SkinDeltaPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Canal único de rede do mod. Pacotes pequenos no caminho "quente"
 * (pintura contínua) e um pacote maior só no caminho "frio" (sincronização
 * completa ao entrar no mundo):
 *  - PaintPartPacket  : cliente -> servidor, preenche 1 parte do corpo.
 *  - FillAllPacket    : cliente -> servidor, preenche a skin inteira.
 *  - PaintPixelPacket : cliente -> servidor, pinta 1 pixel (Modo Pincel 3D).
 *  - SkinDeltaPacket  : servidor -> clientes, eco pequeno de uma das 3 ações acima.
 *  - FullSkinSyncPacket: servidor -> clientes, bitmap 64x64 completo (só no login).
 */
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    private static int id = 0;

    private static int nextId() {
        return id++;
    }

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(SkinCamoMod.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        CHANNEL.registerMessage(nextId(), PaintPartPacket.class,
                PaintPartPacket::encode, PaintPartPacket::decode, PaintPartPacket::handle);

        CHANNEL.registerMessage(nextId(), FillAllPacket.class,
                FillAllPacket::encode, FillAllPacket::decode, FillAllPacket::handle);

        CHANNEL.registerMessage(nextId(), PaintPixelPacket.class,
                PaintPixelPacket::encode, PaintPixelPacket::decode, PaintPixelPacket::handle);

        CHANNEL.registerMessage(nextId(), SkinDeltaPacket.class,
                SkinDeltaPacket::encode, SkinDeltaPacket::decode, SkinDeltaPacket::handle);

        CHANNEL.registerMessage(nextId(), FullSkinSyncPacket.class,
                FullSkinSyncPacket::encode, FullSkinSyncPacket::decode, FullSkinSyncPacket::handle);
    }
}
