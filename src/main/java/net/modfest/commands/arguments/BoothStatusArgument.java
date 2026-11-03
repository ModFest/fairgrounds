package net.modfest.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.StringRepresentable;
import net.modfest.platform.data.BoothStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class BoothStatusArgument extends EnumArgument<BoothStatus> {
	private static final Collection<String> EXAMPLES = examples(BoothStatus.values());
	private static final Function<String, BoothStatus> LOOKUP = lookup(BoothStatus.values());

	public BoothStatusArgument() {
		super(LOOKUP);
	}

	public static BoothStatusArgument boothStatus() {
		return new BoothStatusArgument();
	}

	public static BoothStatus getBoothStatus(CommandContext<CommandSourceStack> context, String name) {
		return context.getArgument(name, BoothStatus.class);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
