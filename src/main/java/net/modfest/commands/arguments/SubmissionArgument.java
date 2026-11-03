package net.modfest.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.server.permissions.Permissions;
import net.modfest.Fairgrounds;
import net.modfest.platform.data.SubmissionData;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@NullMarked
public class SubmissionArgument implements ArgumentType<SubmissionData> {
	private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(Component.translatable("argument.fairgrounds.submission.empty"));
	private static final DynamicCommandExceptionType ERROR_NO_RESULTS_FOUND = new DynamicCommandExceptionType(name -> Component.translatable("argument.fairgrounds.submission.not_found", name));
	private static final Collection<String> EXAMPLES = Arrays.asList("glowcase", "fairgrounds", "fabric-api");

	public static SubmissionArgument submission() {
		return new SubmissionArgument();
	}

	@Override
	public SubmissionData parse(StringReader reader) throws CommandSyntaxException {
		return parse(reader, getSubmissions(null));
	}

	@Override
	public <S> SubmissionData parse(StringReader reader, S source) throws CommandSyntaxException {
		return parse(reader, getSubmissions(source));
	}

	public SubmissionData parse(StringReader reader, Collection<SubmissionData> submissions) throws CommandSyntaxException {
		if (submissions.isEmpty()) throw ERROR_NO_RESULTS.create();

		var submissionName = reader.readUnquotedString();

		SubmissionData submission = Fairgrounds.platformData().getSubmission(submissionName);
		if (submission == null || !submissions.contains(submission)) throw ERROR_NO_RESULTS_FOUND.create(submissionName);

		return submission;
	}

	public static SubmissionData getSubmission(CommandContext<CommandSourceStack> context, String name) {
		return context.getArgument(name, SubmissionData.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof SharedSuggestionProvider)) return Suggestions.empty();
		return SharedSuggestionProvider.suggest(getSubmissions(context.getSource()).stream().map(SubmissionData::modId), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private <S> Collection<SubmissionData> getSubmissions(@Nullable S source) {
		// Can't find a submission without permission or player
		if (!(source instanceof PermissionSetSupplier sender)) return List.of();

		if (sender.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) {
			return Fairgrounds.platformData().getSubmissions().values();
		}

		if (source instanceof CommandSourceStack sourceStack) {
			// Doesn't have permission and isn't a player, better to deny it
			if (!sourceStack.isPlayer()) return List.of();
			var user = Fairgrounds.platformData().getUser(sourceStack.getPlayer().getUUID());
			if (user == null) return List.of();
			return user.submissions();
		}

		return List.of();
	}
}
