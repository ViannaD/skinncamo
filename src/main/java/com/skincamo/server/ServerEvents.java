package com.skincamo.server;

import com.skincamo.capability.CapabilityHandler;
import com.skincamo.capability.SkinPaintData;
import com.skincamo.network.NetworkHandler;
import com.skincamo.network.packet.FullSkinSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Garante o requisito de multiplayer:
 * "Novos jogadores recebendo imediatamente a aparência atual dos demais."
 *
 * Quando alguém entra:
 *  1) Mandamos para ELE o bitmap completo de pintura de TODOS os jogadores já online.
 *  2) Mandamos para TODOS (inclusive ele mesmo) o bitmap completo dele (branco, ou o
 *     que foi persistido da última sessão, já que a capability é salva no NBT do jogador).
 *
 * Usamos o pacote "pesado" (FullSkinSyncPacket) aqui de propósito: é o único
 * momento em que realmente precisamos do estado inteiro de uma vez. O resto
 * do tempo (pintura normal) usa o pacote pequeno SkinDeltaPacket.
 */
public class ServerEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;

        // 1) Envia ao jogador que entrou o estado completo de todos os outros.
        for (ServerPlayer other : joining.getServer().getPlayerList().getPlayers()) {
            if (other == joining) continue;
            SkinPaintData otherData = CapabilityHandler.getOrDefault(other);
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> joining),
                    FullSkinSyncPacket.of(other.getUUID(), otherData)
            );
        }

        // 2) Anuncia a todos (incluindo ele mesmo) a aparência completa atual de quem entrou.
        SkinPaintData myData = CapabilityHandler.getOrDefault(joining);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                FullSkinSyncPacket.of(joining.getUUID(), myData)
        );
    }
}
