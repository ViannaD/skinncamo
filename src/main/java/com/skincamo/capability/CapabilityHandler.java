package com.skincamo.capability;

import com.skincamo.SkinCamoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public class CapabilityHandler {

    public static final Capability<SkinPaintData> SKIN_PAINT =
            CapabilityManager.get(new CapabilityToken<SkinPaintData>() {});

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(SkinCamoMod.MODID, "skin_paint");

    /**
     * AttachCapabilitiesEvent é disparado no bus de eventos do Forge (jogo),
     * não no bus de eventos do mod (setup) — por isso o registro é feito em
     * MinecraftForge.EVENT_BUS, e não no modBus recebido pelo construtor do
     * mod. Registrar no bus errado faz o Forge recusar carregar o mod com
     * "has @SubscribeEvent annotation, but takes an argument that is not a
     * subtype of ... IModBusEvent".
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new Listener());
    }

    public static LazyOptional<SkinPaintData> get(Player player) {
        return player.getCapability(SKIN_PAINT);
    }

    /** Atalho seguro: devolve sempre um objeto válido (nunca null), mesmo se a capability não estiver presente. */
    public static SkinPaintData getOrDefault(Player player) {
        return get(player).orElseGet(SkinPaintData::new);
    }

    public static class Listener {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(IDENTIFIER, new SkinPaintProvider());
            }
        }
    }
}
