package net.modfest.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class EnumArgument<T extends Enum<T> & StringRepresentable> implements ArgumentType<T> {
	private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType(descriptor -> Component.translatable("argument.fairgrounds.enum.invalid", descriptor));
	private static final Function<EnumArgument<?>, Component> DESCRIPTOR = Util.memoize(EnumArgument::descriptor);
	private final Function<String, @Nullable T> lookup;

	public EnumArgument(Function<String, @Nullable T> lookup) {
		this.lookup = lookup;
	}

	@Override
	public T parse(StringReader reader) throws CommandSyntaxException {
		T value = lookup.apply(reader.readUnquotedString());
		if (value == null) throw ERROR_INVALID.createWithContext(reader, DESCRIPTOR.apply(this));

		return value;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(getExamples(), builder);
	}

	@Override
	public abstract Collection<String> getExamples();

	protected Component descriptor() {
		return Component.translatable(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(ArgumentTypeInfos.byClass(this)).toLanguageKey("argument", "descriptor"));
	}

	protected static <T extends Enum<T> & StringRepresentable> Function<String, @Nullable T> lookup(T[] values) {
		return StringRepresentable.createNameLookup(values, T::getSerializedName);
	}

	protected static <T extends Enum<T> & StringRepresentable> Collection<String> examples(T[] values) {
		return Arrays.stream(values).map(T::getSerializedName).toList();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		return Objects.equals(this.lookup, ((EnumArgument<?>) obj).lookup);
	}
}
