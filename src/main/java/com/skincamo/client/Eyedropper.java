package com.skincamo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

/**
 * Conta-gotas: em vez de tentar ler pixel a pixel da textura/atlas (algo
 * bem mais pesado e frágil entre versões/mods), usamos a "MapColor" que
 * cada bloco já expõe nativamente — é a MESMA cor usada para desenhar mapas
 * no jogo, e por isso JÁ leva em conta bioma (grama, folhas, água mudam de
 * cor conforme o bioma) e funciona automaticamente com blocos de qualquer
 * mod, já que todo Block tem uma MapColor padrão.
 *
 * Quem quiser precisão "pixel perfect" da textura real pode trocar
 * sampleColorAt() por uma leitura do atlas de blocos via TextureAtlasSprite
 * nas coordenadas de UV do BlockHitResult — fica como ponto de expansão.
 */
public final class Eyedropper {

    private Eyedropper() {}

    /** true = próximo clique esquerdo no mundo captura a cor em vez de quebrar o bloco. */
    private static volatile boolean armed = false;
    private static volatile Integer lastColor = null;

    public static void arm() {
        armed = true;
    }

    public static boolean isArmed() {
        return armed;
    }

    /** Consome (lê e zera) a última cor capturada - usado pela GUI ao reabrir. */
    public static Integer consumeLastColor() {
        Integer c = lastColor;
        lastColor = null;
        return c;
    }

    /** Captura instantânea olhando para o bloco no centro da tela (usada pela tecla rápida G). */
    public static Optional<Integer> captureFromCrosshair() {
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || mc.level == null) {
            return Optional.empty();
        }
        return sampleColorAt(mc.level, blockHit.getBlockPos());
    }

    public static Optional<Integer> sampleColorAt(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return Optional.empty();
        MapColor mapColor = state.getMapColor(level, pos);
        if (mapColor == null || mapColor == MapColor.NONE) return Optional.empty();
        return Optional.of(mapColor.col & 0xFFFFFF);
    }

    /**
     * Enquanto "armado", intercepta o próximo clique em um bloco do mundo,
     * captura a cor e cancela a quebra do bloco.
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!armed) return;
        if (!event.getLevel().isClientSide()) return;

        armed = false;
        sampleColorAt(event.getLevel(), event.getPos()).ifPresent(color -> lastColor = color);
        event.setCanceled(true);

        Minecraft.getInstance().setScreen(new com.skincamo.client.gui.SkinPainterScreen());
    }
}
