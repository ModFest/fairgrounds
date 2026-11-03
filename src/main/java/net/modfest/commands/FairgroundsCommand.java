package net.modfest.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.modfest.Fairgrounds;
import net.modfest.util.Text;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.modfest.commands.FairgroundsCommands.ASYNC_PREFIX;

public class FairgroundsCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
		  literal("fairgrounds")
			.then(literal("reload_config")
			  .executes(context -> {
				  CommandSourceStack source = context.getSource();

				  source.sendSuccess(() -> Text.compound(ASYNC_PREFIX, "Reloading fairgrounds config and data manager"), true);
				  Fairgrounds.reloadConfig().thenRun(() -> {
					  source.sendSuccess(() -> Text.compound(ASYNC_PREFIX, "Finished restarting the data manager"), true);
				  });

				  return 0;
			  })
			)
		);
	}
}
