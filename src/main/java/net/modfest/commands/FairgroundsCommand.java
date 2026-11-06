package net.modfest.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.modfest.Fairgrounds;
import net.modfest.util.Text;

import static net.minecraft.commands.Commands.literal;
import static net.modfest.commands.FairgroundsCommands.ASYNC_PREFIX;

public class FairgroundsCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
		  literal("fairgrounds")
			.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
			.then(literal("reload_config")
			  .executes(context -> {
				  CommandSourceStack source = context.getSource();

				  FairgroundsCommands.sendTemporaryMessage(source, Text.compound(ASYNC_PREFIX, "Reloading fairgrounds config and data manager"));
				  Fairgrounds.reloadConfig().thenRun(() -> {
					  source.sendSuccess(() -> Text.compound(ASYNC_PREFIX, "Finished restarting the data manager"), true);
				  });

				  return 0;
			  })
			)
		);
	}
}
