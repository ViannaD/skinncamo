package com.skincamo.network.packet;

import com.skincamo.capability.CapabilityHandler;
import com.skincamo.capability.SkinPaintData;
import com.skincamo.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class FillAllPacket {

    private final int rgb;

    public FillAllPacket(int rgb) {
        this.rgb = rgb;
    }

    public static void encode(FillAllPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.rgb & 0xFFFFFF);
    }

    public static FillAllPacket decode(FriendlyByteBuf buf) {
        return new FillAllPacket(buf.readInt());
    }

    public static void handle(FillAllPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            SkinPaintData data = CapabilityHandler.getOrDefault(player);
            data.fillAll(msg.rgb);

            NetworkHandler.CHANNEL.send(
                    PacketDistributor.ALL.noArg(),
                    SyncSkinDataPacket.of(player.getUUID(), data)
            );
        });
        ctx.setPacketHandled(true);
    }
}
