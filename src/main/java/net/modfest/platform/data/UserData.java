package net.modfest.platform.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.modfest.util.Optionals;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;

public class UserData {
	/* Magic format:
		FEFE - header marker
		xxx - Unique ID for the data class (12 bits)
		da - separator/visual marker
		xxx - cache version (12 bits)
		FEFE - header marker
	*/
	static final long MAGIC = 0xFEFE200da001FEFEL;
	public static final Codec<UserData> CODEC = RecordCodecBuilder.create(instance ->
	  instance.group(
	    Codec.STRING.fieldOf("id").forGetter(UserData::id),
	    Codec.STRING.optionalFieldOf("pronouns").forGetter(Optionals.wrap(UserData::pronouns)),
	    UUIDUtil.AUTHLIB_CODEC.listOf().fieldOf("minecraft_accounts").forGetter(UserData::minecraftAccounts),
	    Codec.STRING.optionalFieldOf("discord_id").forGetter(Optionals.wrap(UserData::discordId)),
	    Role.CODEC.fieldOf("role").forGetter(UserData::role)
	  ).apply(instance, UserData::new)
	);
	public static final Codec<List<UserData>> LIST_CODEC = CODEC.listOf();
	public static final StreamCodec<FriendlyByteBuf, UserData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, UserData::id,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), Optionals.wrap(UserData::pronouns),
		UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), UserData::minecraftAccounts,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), Optionals.wrap(UserData::discordId),
		Role.STREAM_CODEC, UserData::role,
		UserData::new
	);
	public static final StreamCodec<FriendlyByteBuf, Map<UUID, UserData>> MAP_STREAM_CODEC = ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, STREAM_CODEC);

	private final List<SubmissionData> submissions = new ArrayList<>();
	private final String id;
	private final @Nullable String pronouns;
	private List<UUID> minecraftAccounts;
	private final @Nullable String discordId;
	private final Role role;

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public UserData(String id, Optional<String> pronouns, List<UUID> minecraftAccounts, Optional<String> discordId, Role role) {
		this.id = id;
		this.pronouns = pronouns.orElse(null);
		this.minecraftAccounts = minecraftAccounts;
		this.discordId = discordId.orElse(null);
		this.role = role;
	}

	public UserData(String id, @Nullable String pronouns, List<UUID> minecraftAccounts, @Nullable String discordId, Role role) {
		this.id = id;
		this.pronouns = pronouns;
		this.minecraftAccounts = minecraftAccounts;
		this.discordId = discordId;
		this.role = role;
	}

	public String id() {
		return id;
	}

	public @Nullable String pronouns() {
		return pronouns;
	}

	public Role role() {
		return role;
	}

	public List<UUID> minecraftAccounts() {
		return minecraftAccounts;
	}

	public @Nullable String discordId() {
		return discordId;
	}

	public List<SubmissionData> submissions() {
		return submissions;
	}

	public enum Role implements StringRepresentable {
		NONE,
		VOLUNTEER,
		TEAM_MEMBER;

		public static final StringRepresentable.EnumCodec<Role> CODEC = StringRepresentable.fromEnum(Role::values);
		private static final IntFunction<Role> BY_ID = ByIdMap.continuous(Role::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
		public static final StreamCodec<ByteBuf, Role> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Role::ordinal);

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}
}
