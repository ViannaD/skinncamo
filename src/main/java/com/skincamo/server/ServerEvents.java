package com.skincamo.server;

import com.skincamo.capability.CapabilityHandler;
import com.skincamo.capability.SkinPaintData;
import com.skincamo.network.NetworkHandler;
import com.skincamo.network.packet.SyncSkinDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Garante o requisito de multiplayer:
 * "Novos jogadores recebendo imediatamente a aparência atual dos demais."
 *
 * Quando alguém entra:
 *  1) Mandamos para ELE o estado de pintura de TODOS os jogadores já online.
 *  2) Mandamos para TODOS (inclusive ele mesmo) o estado dele (branco, ou o que
 *     foi persistido da última sessão, já que a capability é salva no NBT do jogador).
 */
public class ServerEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;

        // 1) Envia ao jogador que entrou o estado de todos os outros.
        for (ServerPlayer other : joining.getServer().getPlayerList().getPlayers()) {
            if (other == joining) continue;
            SkinPaintData otherData = CapabilityHandler.getOrDefault(other);
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> joining),
                    SyncSkinDataPacket.of(other.getUUID(), otherData)
            );
        }

        // 2) Anuncia a todos (incluindo ele mesmo) a aparência atual do jogador que entrou.
        SkinPaintData myData = CapabilityHandler.getOrDefault(joining);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                SyncSkinDataPacket.of(joining.getUUID(), myData)
        );
    }
}
