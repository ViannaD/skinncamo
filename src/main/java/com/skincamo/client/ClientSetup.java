package com.skincamo.client;

import com.skincamo.client.brush.BrushMode;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientSetup {

    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.register(KeyRegistrar.class);
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        MinecraftForge.EVENT_BUS.register(Eyedropper.class);
        MinecraftForge.EVENT_BUS.register(BrushMode.class);
    }

    /** Classe separada só para o listener estático de registro de teclas (boas práticas do Forge). */
    public static class KeyRegistrar {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.OPEN_PAINTER);
            event.register(KeyBindings.QUICK_EYEDROPPER);
            event.register(KeyBindings.TOGGLE_BRUSH_MODE);
        }
    }
}
