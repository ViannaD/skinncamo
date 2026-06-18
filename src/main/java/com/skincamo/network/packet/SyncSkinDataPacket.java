package com.skincamo.network.packet;

import com.skincamo.capability.BodyPart;
import com.skincamo.capability.SkinPaintData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncSkinDataPacket {

    private final UUID playerId;
    private final Map<BodyPart, Integer> colors;

    public SyncSkinDataPacket(UUID playerId, Map<BodyPart, Integer> colors) {
        this.playerId = playerId;
        this.colors = colors;
    }

    public static SyncSkinDataPacket of(UUID playerId, SkinPaintData data) {
        return new SyncSkinDataPacket(playerId, new EnumMap<>(data.asMap()));
    }

    public static void encode(SyncSkinDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        for (BodyPart part : BodyPart.VALUES) {
            buf.writeInt(msg.colors.getOrDefault(part, SkinPaintData.DEFAULT_COLOR));
        }
    }

    public static SyncSkinDataPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        Map<BodyPart, Integer> colors = new EnumMap<>(BodyPart.class);
        for (BodyPart part : BodyPart.VALUES) {
            colors.put(part, buf.readInt());
        }
        return new SyncSkinDataPacket(id, colors);
    }

    public static void handle(SyncSkinDataPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.skincamo.client.ClientSkinCache.apply(msg.playerId, msg.colors))
        );
        ctx.setPacketHandled(true);
    }
}
