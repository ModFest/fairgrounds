package net.modfest.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum Unit implements StringRepresentable {
	INSTANCE;

	public static final Codec<Unit> CODEC = MapCodec.unitCodec(INSTANCE);
	public static final StreamCodec<ByteBuf, Unit> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public String getSerializedName() {
		return "unit";
	}

	@Override
	public String toString() {
		return "Unit";
	}
}
