package com.example.ledgehelp;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = LedgeHelp.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    /** Одна клавиша: зацепиться за край в воздухе / протянуть руку, стоя на земле. */
    public static final KeyMapping HOLD =
            new KeyMapping("key.ledgehelp.hold", InputConstants.KEY_G, "key.categories.ledgehelp");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(HOLD);
    }

    private ClientModEvents() {}
}
