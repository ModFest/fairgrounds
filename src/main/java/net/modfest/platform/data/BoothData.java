package net.modfest.platform.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ColumnPos;
import net.modfest.util.Optionals;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class BoothData {
	public static final Codec<ColumnPos> COLUMN_CODEC = RecordCodecBuilder.create(instance ->
	  instance.group(
	    Codec.INT.fieldOf("x").forGetter(ColumnPos::x),
	    Codec.INT.fieldOf("z").forGetter(ColumnPos::z)
	  ).apply(instance, ColumnPos::new)
	);
	public static final StreamCodec<FriendlyByteBuf, ColumnPos> COLUMN_STREAM_CODEC = new StreamCodec<>() {
		public ColumnPos decode(final FriendlyByteBuf input) {
			return new ColumnPos(input.readInt(), input.readInt());
		}

		public void encode(final FriendlyByteBuf output, final ColumnPos value) {
			output.writeInt(value.x());
			output.writeInt(value.z());
		}
	};
	public static final Codec<BoothData> CODEC = RecordCodecBuilder.create(instance ->
	  instance.group(
		  COLUMN_CODEC.optionalFieldOf("marker_pos").forGetter(Optionals.wrap(BoothData::markerPos)),
		  WarpCoordinates.CODEC.optionalFieldOf("warp").forGetter(Optionals.wrap(BoothData::warp)),
		  Codec.STRING.optionalFieldOf("item_icon").forGetter(Optionals.wrap(BoothData::itemIcon)),
		  BoothStatus.CODEC.optionalFieldOf("status").forGetter(Optionals.wrap(BoothData::status))
	  ).apply(instance, BoothData::new)
	);
	public static final StreamCodec<FriendlyByteBuf, BoothData> STREAM_CODEC = StreamCodec.composite(
		COLUMN_STREAM_CODEC.apply(ByteBufCodecs::optional), Optionals.wrap(BoothData::markerPos),
		WarpCoordinates.STREAM_CODEC.apply(ByteBufCodecs::optional), Optionals.wrap(BoothData::warp),
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), Optionals.wrap(BoothData::itemIcon),
		BoothStatus.STREAM_CODEC.apply(ByteBufCodecs::optional), Optionals.wrap(BoothData::status),
		BoothData::new
	);

	private @Nullable ColumnPos markerPos;
	private @Nullable WarpCoordinates warp;
	private @Nullable String itemIcon;
	private @Nullable BoothStatus status;

	public BoothData() {}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public BoothData(Optional<ColumnPos> markerPos, Optional<WarpCoordinates> warp, Optional<String> itemIcon, Optional<BoothStatus> status) {
		this.markerPos = markerPos.orElse(null);
		this.warp = warp.orElse(null);
		this.itemIcon = itemIcon.orElse(null);
		this.status = status.orElse(null);
	}

	public BoothData(@Nullable ColumnPos markerPos, @Nullable WarpCoordinates warp, @Nullable String itemIcon, @Nullable BoothStatus status) {
		this.markerPos = markerPos;
		this.warp = warp;
		this.itemIcon = itemIcon;
		this.status = status;
	}

	public @Nullable ColumnPos markerPos() {
		return markerPos;
	}

	public void setMarkerPos(@Nullable ColumnPos markerPos) {
		this.markerPos = markerPos;
	}

	public @Nullable WarpCoordinates warp() {
		return warp;
	}

	public void setWarp(@Nullable WarpCoordinates warp) {
		this.warp = warp;
	}

	public @Nullable String itemIcon() {
		return itemIcon;
	}

	public void setItemIcon(@Nullable String itemIcon) {
		this.itemIcon = itemIcon;
	}

	public @Nullable BoothStatus status() {
		return status;
	}

	public void setStatus(@Nullable BoothStatus status) {
		this.status = status;
	}
}
