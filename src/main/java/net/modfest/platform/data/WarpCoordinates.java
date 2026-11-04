package net.modfest.platform.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.joml.Options;
import org.joml.Runtime;

import java.text.NumberFormat;
import java.util.function.IntFunction;

public record WarpCoordinates(int x, int y, int z, Direction direction) {
	public static final Codec<WarpCoordinates> CODEC = RecordCodecBuilder.create(instance ->
	  instance.group(
	    Codec.INT.fieldOf("x").forGetter(WarpCoordinates::x),
	    Codec.INT.fieldOf("y").forGetter(WarpCoordinates::y),
	    Codec.INT.fieldOf("z").forGetter(WarpCoordinates::z),
	    Direction.CODEC.fieldOf("direction").forGetter(WarpCoordinates::direction)
	  ).apply(instance, WarpCoordinates::new)
	);
	public static final StreamCodec<FriendlyByteBuf, WarpCoordinates> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, WarpCoordinates::x,
		ByteBufCodecs.INT, WarpCoordinates::y,
		ByteBufCodecs.INT, WarpCoordinates::z,
		Direction.STREAM_CODEC, WarpCoordinates::direction,
		WarpCoordinates::new
	);

	public WarpCoordinates(Vec3i pos, Direction direction) {
		this(pos.getX(), pos.getY(), pos.getZ(), direction);
	}

	@Override
	public String toString() {
		return "[" + x + ", " + y + ", " + z + "] facing " + direction;
	}

	public enum Direction implements StringRepresentable {
		NORTH(180),
		NORTH_NORTH_EAST(-150),
		NORTH_EAST(-135),
		EAST_NORTH_EAST(-120),
		EAST(-90),
		EAST_SOUTH_EAST(-60),
		SOUTH_EAST(-45),
		SOUTH_SOUTH_EAST(-30),
		SOUTH(0),
		SOUTH_SOUTH_WEST(30),
		SOUTH_WEST(45),
		WEST_SOUTH_WEST(60),
		WEST(90),
		WEST_NORTH_WEST(120),
		NORTH_WEST(135),
		NORTH_NORTH_WEST(150);

		public static final Codec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);
		private static final IntFunction<Direction> BY_ID = ByIdMap.continuous(Direction::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
		public static final StreamCodec<ByteBuf, Direction> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Direction::ordinal);
		private final int yaw;

		Direction(int yaw) {
			this.yaw = yaw;
		}

		public int yaw() {
			return yaw;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}

		public Component getComponent() {
			return Component.translatable("fairgrounds.warp_direction." + getSerializedName());
		}

		@Override
		public String toString() {
			return this.getSerializedName() + " (" + yaw + ")";
		}
	}
}
