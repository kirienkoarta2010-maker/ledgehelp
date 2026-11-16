package com.example.ledgehelp;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /ledgehelp debug - включает отладочные сообщения над хотбаром. */
@EventBusSubscriber(modid = LedgeHelp.MODID)
public final class LedgeCommand {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("ledgehelp")
                .then(Commands.literal("debug").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean on = LedgeServer.toggleDebug(p);
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("LedgeHelp debug: " + (on ? "включена" : "выключена")), false);
                    return 1;
                })));
    }

    private LedgeCommand() {}
}
