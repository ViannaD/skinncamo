package com.skincamo.capability;

import com.skincamo.SkinCamoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class CapabilityHandler {

    public static final Capability<SkinPaintData> SKIN_PAINT =
            CapabilityManager.get(new CapabilityToken<SkinPaintData>() {});

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(SkinCamoMod.MODID, "skin_paint");

    public static void register(IEventBus modBus) {
        modBus.register(new Listener());
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
