package com.skincamo.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.skincamo.capability.BodyPart;
import net.minecraft.client.renderer.texture.DynamicTexture;

public final class SkinTextureFactory {

    private SkinTextureFactory() {}

    public static final int SIZE = 64;

    /** Cria a "tela em branco" inicial: skin 64x64 totalmente branca, overlay transparente. */
    public static DynamicTexture createBlank() {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, true);
        for (BodyPart part : BodyPart.VALUES) {
            SkinPixelMap.fillPart(image, part, 0xFFFFFF);
        }
        SkinPixelMap.clearOverlay(image);
        DynamicTexture texture = new DynamicTexture(image);
        texture.upload();
        return texture;
    }
}
