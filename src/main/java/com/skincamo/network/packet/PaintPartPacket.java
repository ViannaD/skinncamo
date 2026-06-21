package com.skincamo.network.packet;

import com.skincamo.capability.BodyPart;
import com.skincamo.capability.CapabilityHandler;
import com.skincamo.capability.SkinPaintData;
import com.skincamo.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PaintPartPacket {

    private final BodyPart part;
    private final int rgb;

    public PaintPartPacket(BodyPart part, int rgb) {
        this.part = part;
        this.rgb = rgb;
    }

    public static void encode(PaintPartPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.part);
        buf.writeInt(msg.rgb & 0xFFFFFF);
    }

    public static PaintPartPacket decode(FriendlyByteBuf buf) {
        return new PaintPartPacket(buf.readEnum(BodyPart.class), buf.readInt());
    }

    public static void handle(PaintPartPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Servidor é a fonte da verdade: valida e atualiza a capability.
            SkinPaintData data = CapabilityHandler.getOrDefault(player);
            data.setPartColor(msg.part, msg.rgb);

            // Re-transmite (pequeno: UUID + enum + cor) para TODOS, inclusive quem enviou.
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.ALL.noArg(),
                    SkinDeltaPacket.partFill(player.getUUID(), msg.part, msg.rgb)
            );
        });
        ctx.setPacketHandled(true);
    }
}
