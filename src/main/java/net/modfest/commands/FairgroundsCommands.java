package net.modfest.commands;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.modfest.commands.arguments.BoothStatusArgument;
import net.modfest.commands.arguments.EnumArgument;
import net.modfest.commands.arguments.SubmissionArgument;
import net.modfest.commands.arguments.WarpDirectionArgument;
import net.modfest.platform.data.BoothStatus;

import static net.minecraft.commands.synchronization.SingletonArgumentInfo.contextFree;
import static net.modfest.Fairgrounds.id;

public class FairgroundsCommands {
	public static final Component ASYNC_PREFIX = Component.translatable("command.fairgrounds.prefix.async").withColor(TextColor.GRAY);

	public static void init() {
		ArgumentTypeRegistry.registerArgumentType(id("submission"), SubmissionArgument.class, contextFree(SubmissionArgument::submission));
		ArgumentTypeRegistry.registerArgumentType(id("booth", "status"), BoothStatusArgument.class, contextFree(BoothStatusArgument::boothStatus));
		ArgumentTypeRegistry.registerArgumentType(id("boot", "warp", "direction"), WarpDirectionArgument.class, contextFree(WarpDirectionArgument::warpDirection));

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			FairgroundsCommand.register(dispatcher);
			BoothCommand.register(dispatcher);
		});
	}

	public static void sendTemporaryMessage(CommandSourceStack source, Component message) {
		if (!source.isSilent()) {
			ServerPlayer player = source.getPlayer();
			if (player != null) {
				player.sendSystemMessage(message, true);
			} else {
				source.sendSystemMessage(message);
			}
		}
	}
}
