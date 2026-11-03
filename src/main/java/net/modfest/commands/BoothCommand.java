package net.modfest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.phys.Vec3;
import net.modfest.Fairgrounds;
import net.modfest.commands.arguments.BoothStatusArgument;
import net.modfest.commands.arguments.SubmissionArgument;
import net.modfest.commands.arguments.WarpDirectionArgument;
import net.modfest.platform.data.BoothData;
import net.modfest.platform.data.BoothStatus;
import net.modfest.platform.data.SubmissionData;
import net.modfest.platform.data.WarpCoordinates;
import net.modfest.platform.data.WarpCoordinates.Direction;
import net.modfest.util.Text;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.modfest.commands.FairgroundsCommands.ASYNC_PREFIX;

public class BoothCommand {
	private static final DynamicCommandExceptionType ERROR_INVALID_ITEM = new DynamicCommandExceptionType(value -> Component.translatable("command.fairgrounds.booth.error.invalid_item", value));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
		  literal("booth")
			.then(literal("data")
			  .then(argument("submission", SubmissionArgument.submission())
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					SubmissionData submission = SubmissionArgument.getSubmission(context, "submission");
					return getData(source, submission);
				})
			  )
			).then(literal("marker")
			  .then(argument("submission", SubmissionArgument.submission())
				.then(argument("pos", ColumnPosArgument.columnPos())
				  .executes(context -> {
					  CommandSourceStack source = context.getSource();
					  SubmissionData submission = SubmissionArgument.getSubmission(context, "submission");
					  ColumnPos pos = ColumnPosArgument.getColumnPos(context, "pos");
					  return setMarker(source, submission, pos);
				  })
				)
			  )
			).then(literal("warp")
			  .then(argument("submission", SubmissionArgument.submission())
				.then(argument("pos", BlockPosArgument.blockPos())
				  .then(argument("direction", WarpDirectionArgument.warpDirection())
					.executes(context -> {
						CommandSourceStack source = context.getSource();
						SubmissionData submission = SubmissionArgument.getSubmission(context, "submission");
						BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
						Direction direction = WarpDirectionArgument.getWarpDirection(context, "direction");
						return setWarp(source, submission, pos, direction);
					})
				  )
				)
			  )
			).then(literal("icon")
			  // TODO: do we want this to be accessible to all booth authors?
			  .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
			  .then(argument("submission", SubmissionArgument.submission())
				.then(argument("item", ResourceKeyArgument.key(Registries.ITEM))
				  .executes(context -> {
					  CommandSourceStack source = context.getSource();
					  SubmissionData submission = SubmissionArgument.getSubmission(context, "submission");
					  String icon = ResourceKeyArgument.getRegistryKey(context, "item", Registries.ITEM, ERROR_INVALID_ITEM).identifier().toString();
					  return setIcon(source, submission, icon);
				  })
				)
			  )
			).then(literal("status")
			  .then(argument("submission", SubmissionArgument.submission())
				.then(argument("status", BoothStatusArgument.boothStatus()))
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					SubmissionData submission = SubmissionArgument.getSubmission(context, "submission");
					BoothStatus status = BoothStatusArgument.getBoothStatus(context, "submission");
					return setStatus(source, submission, status);
				})
			  )
			)
		);
	}

	private static int getData(CommandSourceStack source, SubmissionData submission) {
		MutableComponent text = Text.compound(
		  "Submission and booth data",
		  "\n  Submission id: " + submission.id(),
		  "\n  Name: " + submission.name(),
		  "\n  Mod ID: " + submission.modId()
		);

		BoothData boothData = submission.boothData();
		if (boothData != null) {
			Text.append(text,
			  "\n  Marker pos: " + boothData.markerPos(),
			  "\n  Warp pos: " + boothData.warp(),
			  "\n  Item icon: " + boothData.itemIcon(),
			  "\n  Status: " + boothData.status()
			);
		} else {
			text.append("\n  No booth data");
		}

		source.sendSuccess(() -> text, false);
		return 0;
	}

	private static int setMarker(CommandSourceStack source, SubmissionData submission, ColumnPos pos) {
		FairgroundsCommands.sendTemporaryMessage(source, Text.compound(ASYNC_PREFIX, Component.translatable("command.fairgrounds.booth.updating")));
		CompletableFuture.runAsync(() -> {
			var response = Fairgrounds.platformData().api().patchBoothData(submission.id(), new BoothData(pos, null, null, null));
			if (response.isError()) {
				source.sendFailure(Component.translatable("command.fairgrounds.booth.marker.error", response.error().message()));
				response.stderr(Fairgrounds.LOGGER, "Failed to update booth data for " + submission.id());
				return;
			}

			SubmissionData data = response.getOrThrow();
			Fairgrounds.platformData().updateSubmission(data);

			source.sendSuccess(() -> Component.translatable("command.fairgrounds.booth.marker.updated", pos.x(), pos.z()), false);
		});

		return 0;
	}

	private static int setWarp(CommandSourceStack source, SubmissionData submission, BlockPos pos, Direction direction) {
		FairgroundsCommands.sendTemporaryMessage(source, Text.compound(ASYNC_PREFIX, Component.translatable("command.fairgrounds.booth.updating")));
		CompletableFuture.runAsync(() -> {
			var response = Fairgrounds.platformData().api().patchBoothData(submission.id(), new BoothData(null, new WarpCoordinates(pos, direction), null, null));
			if (response.isError()) {
				source.sendFailure(Component.translatable("command.fairgrounds.booth.warp.error", response.error().message()));
				response.stderr(Fairgrounds.LOGGER, "Failed to update booth data for " + submission.id());
				return;
			}

			SubmissionData data = response.getOrThrow();
			Fairgrounds.platformData().updateSubmission(data);

			Component posText = Component
			  .translatable("command.fairgrounds.booth.warp.pos", pos.getX(), pos.getY(), pos.getZ(), direction.getComponent())
			  .withStyle(
				s -> s.withColor(ChatFormatting.GREEN)
				  .withClickEvent(new ClickEvent.SuggestCommand("/tp @s %d %d %d %d 0".formatted(pos.getX(), pos.getY(), pos.getZ(), direction.yaw())))
				  .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
			  );

			source.sendSuccess(() -> Component.translatable("command.fairgrounds.booth.warp.updated", posText), false);
		});

		return 0;
	}

	private static int setIcon(CommandSourceStack source, SubmissionData submission, String icon) {
		FairgroundsCommands.sendTemporaryMessage(source, Text.compound(ASYNC_PREFIX, Component.translatable("command.fairgrounds.booth.updating")));
		CompletableFuture.runAsync(() -> {
			var response = Fairgrounds.platformData().api().patchBoothData(submission.id(), new BoothData(null, null, icon, null));
			if (response.isError()) {
				source.sendFailure(Component.translatable("command.fairgrounds.booth.icon.error", response.error().message()));
				response.stderr(Fairgrounds.LOGGER, "Failed to update booth data for " + submission.id());
				return;
			}

			SubmissionData data = response.getOrThrow();
			Fairgrounds.platformData().updateSubmission(data);

			source.sendSuccess(() -> Component.translatable("command.fairgrounds.booth.icon.updated", icon), false);
		});

		return 0;
	}

	private static int setStatus(CommandSourceStack source, SubmissionData submission, BoothStatus status) {
		FairgroundsCommands.sendTemporaryMessage(source, Text.compound(ASYNC_PREFIX, Component.translatable("command.fairgrounds.booth.updating")));
		CompletableFuture.runAsync(() -> {
			var response = Fairgrounds.platformData().api().patchBoothData(submission.id(), new BoothData(null, null, null, status));
			if (response.isError()) {
				source.sendFailure(Component.translatable("command.fairgrounds.booth.status.error", response.error().message()));
				response.stderr(Fairgrounds.LOGGER, "Failed to update booth data for " + submission.id());
				return;
			}

			SubmissionData data = response.getOrThrow();
			Fairgrounds.platformData().updateSubmission(data);

			source.sendSuccess(() -> Component.translatable("command.fairgrounds.booth.status.updated", status.getComponent()), false);
		});

		return 0;
	}
}
