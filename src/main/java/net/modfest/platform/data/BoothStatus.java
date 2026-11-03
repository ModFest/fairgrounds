package net.modfest.platform.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum BoothStatus implements StringRepresentable {
	UNDER_CONSTRUCTION,
	PLAYABLE,
	COMPLETE;

	public static final Codec<BoothStatus> CODEC = StringRepresentable.fromEnum(BoothStatus::values);
	private static final IntFunction<BoothStatus> BY_ID = ByIdMap.continuous(BoothStatus::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
	public static final StreamCodec<ByteBuf, BoothStatus> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, BoothStatus::ordinal);

	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}

	public Component getComponent() {
		return Component.translatable("fairgrounds.booth_status." + getSerializedName());
	}

	@Override
	public String toString() {
		return getSerializedName();
	}
}
