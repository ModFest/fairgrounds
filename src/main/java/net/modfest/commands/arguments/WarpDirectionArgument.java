package net.modfest.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.modfest.platform.data.WarpCoordinates;

import java.util.Collection;
import java.util.function.Function;

public class WarpDirectionArgument extends EnumArgument<WarpCoordinates.Direction> {
	private static final Collection<String> EXAMPLES = examples(WarpCoordinates.Direction.values());
	private static final Function<String, WarpCoordinates.Direction> LOOKUP = lookup(WarpCoordinates.Direction.values());

	public WarpDirectionArgument() {
		super(LOOKUP);
	}

	public static WarpDirectionArgument warpDirection() {
		return new WarpDirectionArgument();
	}

	public static WarpCoordinates.Direction getWarpDirection(CommandContext<CommandSourceStack> context, String name) {
		return context.getArgument(name, WarpCoordinates.Direction.class);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
