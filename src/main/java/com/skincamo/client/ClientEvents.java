package com.skincamo.client;

import com.skincamo.client.brush.BrushMode;
import com.skincamo.client.eyedropper.MimicryHandler;
import com.skincamo.client.gui.PaintActions;
import com.skincamo.client.gui.SkinPainterScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        while (KeyBindings.OPEN_PAINTER.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new SkinPainterScreen());
            }
        }

        while (KeyBindings.QUICK_EYEDROPPER.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                Optional<Integer> color = Eyedropper.captureFromCrosshair();
                color.ifPresent(rgb -> {
                    PaintActions.fillWholeSkin(rgb);
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§7[SkinCamo] §fCor capturada: §a#" + Integer.toHexString(rgb).toUpperCase()),
                            true);
                });
            }
        }

        while (KeyBindings.TOGGLE_BRUSH_MODE.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                BrushMode.toggle();
            }
        }

        InkAnimationManager.tick();
        MimicryHandler.tick(mc);
        BrushMode.tick(mc);
    }
}
