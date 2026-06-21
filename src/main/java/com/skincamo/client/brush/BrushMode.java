package com.skincamo.client.brush;

import com.skincamo.client.gui.ClientPaintStorage;
import com.skincamo.client.gui.PaintActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

/**
 * "Modo Pincel 3D": pinta pixel a pixel mirando direto no próprio corpo,
 * em terceira pessoa, usando o mesmo raio de mira (centro da tela) que o
 * jogo já usa pra mirar em blocos — só que testado contra a geometria do
 * SEU PRÓPRIO jogador (ver {@link BodyRaycaster}), algo que o jogo
 * normalmente nunca faz.
 *
 * Detalhe de implementação: para saber se o botão esquerdo está
 * pressionado, lemos o estado direto do GLFW em vez de usar a API de
 * eventos de input do Forge (que mudou de formato entre versões recentes) —
 * isso reduz risco de incompatibilidade. Para impedir que esse mesmo
 * clique também quebre um bloco de verdade, cancelamos os eventos
 * LeftClickBlock/LeftClickEmpty (esses sim são uma API bem estável,
 * já usada em Eyedropper.java) enquanto o modo estiver ativo.
 */
public final class BrushMode {

    private BrushMode() {}

    private static volatile boolean active = false;
    private static int brushSize = 1; // 1..3 px, ver PaintActions.paintPixel
    private static int lastPaintedX = -1;
    private static int lastPaintedY = -1;

    public static boolean isActive() {
        return active;
    }

    public static int getBrushSize() {
        return brushSize;
    }

    public static void setBrushSize(int size) {
        brushSize = Math.max(1, Math.min(3, size));
    }

    public static void cycleBrushSize() {
        setBrushSize(brushSize >= 3 ? 1 : brushSize + 1);
    }

    public static void toggle() {
        active = !active;
        lastPaintedX = -1;
        lastPaintedY = -1;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("§7[SkinCamo] §fPincel 3D: " + (active ? "§aON §7(mire no seu corpo e clique)" : "§cOFF")),
                    true);
        }
    }

    /** Chamado uma vez por tick de cliente (ver ClientEvents). */
    public static void tick(Minecraft mc) {
        if (!active) return;
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        if (!isLeftMouseHeld(mc)) {
            lastPaintedX = -1;
            lastPaintedY = -1;
            return;
        }

        AbstractClientPlayer self = mc.player;
        float partialTick = mc.getFrameTime();
        Vec3 origin = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 lookVec = mc.gameRenderer.getMainCamera().getLookVector();
        Vec3 dir = new Vec3(lookVec.x, lookVec.y, lookVec.z).normalize();

        Optional<BodyRaycaster.Hit> hit = BodyRaycaster.raycast(self, partialTick, origin, dir);
        if (hit.isEmpty()) return;

        BodyRaycaster.Hit h = hit.get();
        if (h.pixelX == lastPaintedX && h.pixelY == lastPaintedY) return;

        lastPaintedX = h.pixelX;
        lastPaintedY = h.pixelY;

        int rgb = ClientPaintStorage.get().currentColor;
        PaintActions.paintPixel(h.pixelX, h.pixelY, rgb, brushSize);

        mc.level.addParticle(ParticleTypes.END_ROD,
                h.worldPoint.x, h.worldPoint.y, h.worldPoint.z, 0.0, 0.0, 0.0);
    }

    private static boolean isLeftMouseHeld(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (active) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (active) {
            event.setCanceled(true);
        }
    }
}
