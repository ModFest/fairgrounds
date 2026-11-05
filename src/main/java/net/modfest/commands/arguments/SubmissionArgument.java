package net.modfest.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
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
public class SubmissionArgument implements ArgumentType<String> {
	private static final DynamicCommandExceptionType ERROR_NOT_FOUND = new DynamicCommandExceptionType(name -> Component.translatable("argument.fairgrounds.submission.not_found", name));
	private static final Collection<String> EXAMPLES = Arrays.asList("glowcase", "fairgrounds", "fabric-api");

	public static SubmissionArgument submission() {
		return new SubmissionArgument();
	}

	@Override
	public String parse(StringReader reader) {
		return reader.readUnquotedString();
	}

	public static SubmissionData getSubmission(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		String submissionName = context.getArgument(name, String.class);

		return getSubmission(submissionName, getSubmissions(context.getSource()));
	}

	private static SubmissionData getSubmission(String submissionName, Collection<SubmissionData> submissions) throws CommandSyntaxException {
		if (submissions.isEmpty()) throw ERROR_NOT_FOUND.create(submissionName);

		SubmissionData submission = Fairgrounds.platformData().getSubmission(submissionName);
		if (submission == null || !submissions.contains(submission)) throw ERROR_NOT_FOUND.create(submissionName);

		return submission;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		S rawSource = context.getSource();
		if (rawSource instanceof CommandSourceStack source) {
			return SharedSuggestionProvider.suggest(getSubmissions(source).stream().map(SubmissionData::id), builder);
		} else if (rawSource instanceof SharedSuggestionProvider source) {
			return source.customSuggestion(context);
		}

		return Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static <S> Collection<SubmissionData> getSubmissions(@Nullable S source) {
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
