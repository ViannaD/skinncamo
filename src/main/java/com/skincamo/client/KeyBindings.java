package com.skincamo.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

public final class KeyBindings {

    private KeyBindings() {}

    public static final String CATEGORY = "key.categories.skincamo";

    /** Abre/fecha a interface de pintura. Padrão: P. */
    public static final KeyMapping OPEN_PAINTER = new KeyMapping(
            "key.skincamo.open_painter",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_P,
            CATEGORY
    );

    /** Conta-gotas rápido: captura a cor do bloco olhado e aplica na skin inteira. Padrão: G. */
    public static final KeyMapping QUICK_EYEDROPPER = new KeyMapping(
            "key.skincamo.quick_eyedropper",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            CATEGORY
    );

    /** Liga/desliga o Modo Pincel 3D (pintar mirando no próprio corpo). Padrão: B. */
    public static final KeyMapping TOGGLE_BRUSH_MODE = new KeyMapping(
            "key.skincamo.toggle_brush_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            CATEGORY
    );
}
