package com.skincamo.network.packet;

import com.skincamo.capability.CapabilityHandler;
import com.skincamo.capability.SkinPaintData;
import com.skincamo.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Cliente -> servidor: pinta UM pixel específico (x,y) da skin 64x64.
 * É o pacote que o "Modo Pincel 3D" manda continuamente enquanto você
 * arrasta o clique mirando no seu próprio corpo. Mantido bem pequeno de
 * propósito (3 inteiros) porque pode ser enviado várias vezes por segundo.
 */
public class PaintPixelPacket {

    private final int x;
    private final int y;
    private final int rgb;

    public PaintPixelPacket(int x, int y, int rgb) {
        this.x = x;
        this.y = y;
        this.rgb = rgb;
    }

    public static void encode(PaintPixelPacket msg, FriendlyByteBuf buf) {
        buf.writeShort(msg.x);
        buf.writeShort(msg.y);
        buf.writeInt(msg.rgb & 0xFFFFFF);
    }

    public static PaintPixelPacket decode(FriendlyByteBuf buf) {
        return new PaintPixelPacket(buf.readShort(), buf.readShort(), buf.readInt());
    }

    public static void handle(PaintPixelPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (msg.x < 0 || msg.y < 0 || msg.x >= SkinPaintData.SIZE || msg.y >= SkinPaintData.SIZE) return;

            SkinPaintData data = CapabilityHandler.getOrDefault(player);
            data.setPixel(msg.x, msg.y, msg.rgb);

            NetworkHandler.CHANNEL.send(
                    PacketDistributor.ALL.noArg(),
                    SkinDeltaPacket.pixel(player.getUUID(), msg.x, msg.y, msg.rgb)
            );
        });
        ctx.setPacketHandled(true);
    }
}
