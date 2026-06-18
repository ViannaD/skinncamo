package com.skincamo.mixin;

import com.skincamo.client.SkinCamoTextureManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ====================================================================
 *  PONTO DE MAIOR RISCO TÉCNICO DESTE MOD - leia antes de compilar
 * ====================================================================
 * PlayerRenderer#getTextureLocation(AbstractClientPlayer) é o método que o
 * jogo usa, todo frame, para decidir qual ResourceLocation vincular ao
 * desenhar o modelo de um jogador (terceira pessoa, mão em primeira pessoa,
 * outros jogadores na tela). Interceptamos o RETORNO e substituímos pela
 * nossa DynamicTexture, que é exatamente a "tela branca pintável" do mod.
 *
 * Esse nome/assinatura de método é estável desde várias versões do MC com
 * mapeamento oficial (Mojang), mas SE o build falhar aqui com
 * "method not found" ou erro de remap, abra a classe decompilada
 * net.minecraft.client.renderer.entity.player.PlayerRenderer (via um
 * navegador de mappings como o Linkie, ou o "Show decompiled source" de
 * uma IDE com o MDK importado) e confirme o nome real do método nessa
 * build específica do Forge/Minecraft, ajustando a assinatura abaixo.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(
            method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void skincamo$forceCamoSkin(AbstractClientPlayer entity, CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation override = SkinCamoTextureManager.getOrCreateLocation(entity);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
