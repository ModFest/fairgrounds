package net.modfest.platform.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.modfest.util.Optionals;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SubmissionData {
	/* Magic format:
		FEFE - header marker
		xxx - Unique ID for the data class (12 bits)
		da - separator/visual marker
		xxx - cache version (12 bits)
		FEFE - header marker
	*/
	static final long MAGIC = 0xFEFE201da001FEFEL;
	public static final Codec<SubmissionData> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
			Codec.STRING.fieldOf("id").forGetter(SubmissionData::id),
			Codec.STRING.fieldOf("name").forGetter(SubmissionData::name),
			Codec.STRING.optionalFieldOf("mod_id").forGetter(Optionals.wrap(SubmissionData::modId)),
			Codec.STRING.fieldOf("description").forGetter(SubmissionData::description),
			Codec.STRING.listOf().fieldOf("authors").forGetter(SubmissionData::authors),
			BoothData.CODEC.optionalFieldOf("booth_data").forGetter(Optionals.wrap(SubmissionData::boothData))
		).apply(instance, SubmissionData::new)
	);
	public static final Codec<List<SubmissionData>> LIST_CODEC = CODEC.listOf();
	public static final StreamCodec<FriendlyByteBuf, SubmissionData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, SubmissionData::id,
		ByteBufCodecs.STRING_UTF8, SubmissionData::name,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), Optionals.wrap(SubmissionData::modId),
		ByteBufCodecs.STRING_UTF8, SubmissionData::description,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SubmissionData::authors,
		BoothData.STREAM_CODEC.apply(ByteBufCodecs::optional), Optionals.wrap(SubmissionData::boothData),
		SubmissionData::new
	);
	public static final StreamCodec<FriendlyByteBuf, Map<String, SubmissionData>> MAP_STREAM_CODEC = ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, STREAM_CODEC);

	private final String id;
	private final String name;
	private final @Nullable String modId;
	private final String description;
	private final List<String> authors;
	private final @Nullable BoothData boothData;
	private final List<UUID> authorAccounts = new ArrayList<>();

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public SubmissionData(String id, String name, Optional<String> modId, String description, List<String> authors, Optional<BoothData> boothData) {
		this.id = id;
		this.name = name;
		this.modId = modId.orElse(null);
		this.description = description;
		this.authors = authors;
		this.boothData = boothData.orElse(null);
	}

	public SubmissionData(String id, String name, @Nullable String modId, String description, List<String> authors, @Nullable BoothData boothData) {
		this.id = id;
		this.name = name;
		this.modId = modId;
		this.description = description;
		this.authors = authors;
		this.boothData = boothData;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public @Nullable String modId() {
		return modId;
	}

	public String description() {
		return description;
	}

	public List<String> authors() {
		return authors;
	}

	public @Nullable BoothData boothData() {
		return boothData;
	}

	public List<UUID> authorAccounts() {
		return authorAccounts;
	}
}
