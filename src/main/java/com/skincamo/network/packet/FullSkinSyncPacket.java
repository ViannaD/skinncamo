package com.skincamo.network.packet;

import com.skincamo.capability.SkinPaintData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Servidor -> clientes: o bitmap 64x64 COMPLETO de um jogador.
 *
 * Bem mais "pesado" que {@link SkinDeltaPacket} (manda os 4096 pixels, não só
 * a mudança), mas só é usado quando é realmente necessário pegar o estado
 * inteiro de uma vez: quando um jogador entra no mundo (ele precisa do
 * estado completo de todo mundo já online, e todo mundo precisa do estado
 * completo dele). A compressão de pacotes do próprio Minecraft (zlib, já
 * ligada por padrão acima de um certo tamanho) cuida de comprimir isso
 * automaticamente — uma skin pouco pintada (muito branco) comprime muito bem.
 */
public class FullSkinSyncPacket {

    private final UUID playerId;
    private final int[] pixels;

    public FullSkinSyncPacket(UUID playerId, int[] pixels) {
        this.playerId = playerId;
        this.pixels = pixels;
    }

    public static void encode(FullSkinSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeVarInt(msg.pixels.length);
        for (int p : msg.pixels) {
            buf.writeMedium(p & 0xFFFFFF); // 3 bytes bastam (0xRRGGBB)
        }
    }

    public static FullSkinSyncPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        int length = buf.readVarInt();
        int[] pixels = new int[length];
        for (int i = 0; i < length; i++) {
            pixels[i] = buf.readUnsignedMedium();
        }
        return new FullSkinSyncPacket(id, pixels);
    }

    public static void handle(FullSkinSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.skincamo.client.ClientSkinCache.applyFullImage(msg.playerId, msg.pixels)));
        ctx.setPacketHandled(true);
    }

    public static FullSkinSyncPacket of(UUID playerId, SkinPaintData data) {
        return new FullSkinSyncPacket(playerId, data.snapshot());
    }
}
