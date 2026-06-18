package com.skincamo.network;

import com.skincamo.SkinCamoMod;
import com.skincamo.network.packet.FillAllPacket;
import com.skincamo.network.packet.PaintPartPacket;
import com.skincamo.network.packet.SyncSkinDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Canal único de rede do mod. Mantemos os pacotes pequenos (poucos bytes)
 * para respeitar o requisito de baixo tráfego de rede:
 *  - PaintPartPacket : cliente -> servidor, pedido de pintar 1 parte do corpo.
 *  - FillAllPacket    : cliente -> servidor, pedido de pintar a skin inteira.
 *  - SyncSkinDataPacket: servidor -> clientes, "fonte da verdade" após validação.
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

        CHANNEL.registerMessage(nextId(), SyncSkinDataPacket.class,
                SyncSkinDataPacket::encode, SyncSkinDataPacket::decode, SyncSkinDataPacket::handle);
    }
}
