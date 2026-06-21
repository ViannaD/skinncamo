package com.skincamo.network.packet;

import com.skincamo.capability.BodyPart;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Servidor -> clientes: uma atualização pequena e incremental da skin de
 * UM jogador, em um dos 3 formatos possíveis:
 *  - PART:  preencheu uma parte do corpo inteira com uma cor (botão "Aplicar na Parte").
 *  - ALL:   preencheu a skin inteira com uma cor (botão "Preencher Tudo" / conta-gotas / auto-camuflagem).
 *  - PIXEL: pintou um único pixel (x,y) - usado pelo Modo Pincel 3D.
 *
 * Para o caso de alguém entrando no mundo e precisar do estado completo de
 * todo mundo de uma vez, existe um pacote separado: {@link FullSkinSyncPacket}.
 * Mantemos os dois separados de propósito para não desperdiçar rede mandando
 * o bitmap 64x64 inteiro (~16KB) a cada clique de botão ou pincelada.
 */
public class SkinDeltaPacket {

    private enum Type { PART, ALL, PIXEL }

    private final UUID playerId;
    private final Type type;
    private final BodyPart part; // só usado quando type == PART
    private final int x, y;      // só usados quando type == PIXEL
    private final int rgb;

    private SkinDeltaPacket(UUID playerId, Type type, BodyPart part, int x, int y, int rgb) {
        this.playerId = playerId;
        this.type = type;
        this.part = part;
        this.x = x;
        this.y = y;
        this.rgb = rgb;
    }

    public static SkinDeltaPacket partFill(UUID playerId, BodyPart part, int rgb) {
        return new SkinDeltaPacket(playerId, Type.PART, part, 0, 0, rgb);
    }

    public static SkinDeltaPacket allFill(UUID playerId, int rgb) {
        return new SkinDeltaPacket(playerId, Type.ALL, null, 0, 0, rgb);
    }

    public static SkinDeltaPacket pixel(UUID playerId, int x, int y, int rgb) {
        return new SkinDeltaPacket(playerId, Type.PIXEL, null, x, y, rgb);
    }

    public static void encode(SkinDeltaPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeEnum(msg.type);
        switch (msg.type) {
            case PART -> buf.writeEnum(msg.part);
            case PIXEL -> {
                buf.writeShort(msg.x);
                buf.writeShort(msg.y);
            }
            case ALL -> { /* nada extra */ }
        }
        buf.writeInt(msg.rgb & 0xFFFFFF);
    }

    public static SkinDeltaPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        Type type = buf.readEnum(Type.class);
        BodyPart part = null;
        int x = 0, y = 0;
        switch (type) {
            case PART -> part = buf.readEnum(BodyPart.class);
            case PIXEL -> {
                x = buf.readShort();
                y = buf.readShort();
            }
            case ALL -> { /* nada extra */ }
        }
        int rgb = buf.readInt();
        return new SkinDeltaPacket(id, type, part, x, y, rgb);
    }

    public static void handle(SkinDeltaPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            switch (msg.type) {
                case PART -> com.skincamo.client.ClientSkinCache.applyPartDelta(msg.playerId, msg.part, msg.rgb);
                case ALL -> com.skincamo.client.ClientSkinCache.applyAllDelta(msg.playerId, msg.rgb);
                case PIXEL -> com.skincamo.client.ClientSkinCache.applyPixelDelta(msg.playerId, msg.x, msg.y, msg.rgb);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
