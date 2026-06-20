package com.skincamo;

import com.skincamo.capability.CapabilityHandler;
import com.skincamo.network.NetworkHandler;
import com.skincamo.server.ServerEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Ponto de entrada do mod "Skin Camo".
 *
 * Visão geral da arquitetura:
 *  - capability/   -> armazena, por jogador, a cor pintada de cada parte do corpo (servidor, persistido).
 *  - network/      -> pacotes customizados para sincronizar pintura entre clientes.
 *  - server/       -> eventos do lado servidor (login, attach de capability, broadcast).
 *  - client/       -> tudo que é renderização, GUI, conta-gotas, teclas.
 *  - mixin/        -> único ponto de "engenharia pesada": força o jogo a usar a NOSSA
 *                      DynamicTexture em vez da skin baixada do Mojang/servidor de skins.
 */
@Mod(SkinCamoMod.MODID)
public class SkinCamoMod {

    public static final String MODID = "skincamo";

    public SkinCamoMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        CapabilityHandler.register();
        NetworkHandler.register();

        MinecraftForge.EVENT_BUS.register(ServerEvents.class);

        // O setup de client (key bindings, screens) só pode referenciar classes client-only.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.skincamo.client.ClientSetup.init(modBus));
    }
}
