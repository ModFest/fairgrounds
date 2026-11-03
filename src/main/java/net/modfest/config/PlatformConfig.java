package net.modfest.config;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.modfest.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static net.modfest.Constants.Platform.CONFIG_FILE;

public record PlatformConfig(
	String eventId,
	String platformUrl,
	String platformToken,
	int fetchPeriodMinutes
) {
	private static final PlatformConfig EMPTY = new PlatformConfig("", "", "", 3);
	public static final Logger LOGGER = LoggerFactory.getLogger(PlatformConfig.class);
	// Temp config solution, idk
	public static final Codec<PlatformConfig> CODEC = RecordCodecBuilder.create(instance ->
	  instance.group(
	    Codec.STRING.fieldOf("event_id").forGetter(PlatformConfig::eventId),
	    Codec.STRING.fieldOf("platform_url").forGetter(PlatformConfig::platformUrl),
	    Codec.STRING.fieldOf("platform_token").forGetter(PlatformConfig::platformToken),
	    Codec.INT.fieldOf("fetch_period_minutes").forGetter(PlatformConfig::fetchPeriodMinutes)
	  ).apply(instance, PlatformConfig::new)
	);

	public static PlatformConfig load() {
		if (!CONFIG_FILE.toFile().exists()) {
			PlatformConfig config = EMPTY;
			config.save();
			return config;
		}

		try {
			var config = CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(CONFIG_FILE)));
			if (config.isError()) {
				LOGGER.error("Failed to parse config JSON; {}", config.error().orElseThrow().message());
				return EMPTY;
			}

			return config.getOrThrow();
		} catch (IOException e) {
			LOGGER.error("Failed to load platform config file", e);
			return EMPTY;
		}
	}

	public void save() {
		try {
			File file = CONFIG_FILE.toFile();
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}

			var encoded = CODEC.encodeStart(JsonOps.INSTANCE, this);
			if (encoded.isError()) {
				LOGGER.error("Failed to serialize config to JSON; {}", encoded.error().get().message());
				return;
			}

			Files.writeString(CONFIG_FILE, Constants.GSON.toJson(encoded.getOrThrow()), StandardCharsets.UTF_8);

		} catch (IOException e) {
			LOGGER.error("Failed to save platform config file", e);
		}
	}
}
