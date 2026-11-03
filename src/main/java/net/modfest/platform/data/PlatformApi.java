package net.modfest.platform.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import net.modfest.Constants;
import net.modfest.config.PlatformConfig;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.modfest.Constants.GSON;
import static net.modfest.platform.data.PlatformDataManager.LOGGER;
import static net.modfest.util.StringTemplate.format;

public final class PlatformApi {
	private static final String GET_USERS = "users";
	private static final String GET_SUBMISSIONS = "event/{eventId}/submissions";
	private static final String PATCH_BOOTH = "event/{eventId}/booth/{subId}";

	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int READ_TIMEOUT_MS = 5000;

	private final Cache<String, String> hashes = CacheBuilder.newBuilder()
		.expireAfterAccess(Duration.ofMinutes(15))
		.initialCapacity(2)
		.build();

	private final PlatformConfig config;
	private final URI platformUri;

	public PlatformApi(PlatformConfig config) {
		this.platformUri = URI.create(config.platformUrl());
		this.config = config;
	}

	public DataOrThrowable<@Nullable List<UserData>> getUsers() {
		return get(GET_USERS, UserData.LIST_CODEC);
	}

	public DataOrThrowable<@Nullable List<SubmissionData>> getSubmissions() {
		return get(format(GET_SUBMISSIONS, "eventId", config.eventId()), SubmissionData.LIST_CODEC);
	}

	/// @return The new submission data or an error with an exception
	public DataOrThrowable<SubmissionData> patchBoothData(String submissionId, BoothData boothData) {
		return connect(
			"PATCH",
			format(PATCH_BOOTH, "eventId", config.eventId(), "subId", submissionId),
			boothData, BoothData.CODEC,
			SubmissionData.CODEC
		);
	}

	/// @return Data or an error with an exception, data is null if the server response's hash matches the cached one (only on GET requests)
	private <T> DataOrThrowable<@Nullable T> get(String endpoint, Decoder<T> decoder) {
		return connect("GET", endpoint, null, null, decoder);
	}

	// TODO: build a better solution then arbitrarily returning null data on hash matches, maybe better solution for errors too?
	// I was originally going to use ETag but the platform doesn't implement it, and the current setup would require some work to properly implement it - Luna
	/// @return Data or an error with an exception, data is null if the server response's hash matches the cached one (only on GET requests)
	private <T, D extends @Nullable Object> DataOrThrowable<T> connect(String method, String endpoint, D data, @Nullable Encoder<D> encoder, Decoder<T> decoder) {
		URL url;
		HttpURLConnection connection;
		try {
			url = createUrl(endpoint);
		} catch (MalformedURLException e) {
			return DataOrThrowable.throwable("Could not build URL for endpoint " + endpoint, e);
		}
		try {
			connection = createConnection(method, url);
		} catch (IOException e) {
			return DataOrThrowable.throwable("Failed to open " + method + " connection to " + url, e);
		}

		if (encoder != null) {
			try {
				setConnectionBody(connection, data, encoder);
			} catch (IOException e) {
				return DataOrThrowable.throwable("Failed to write data to request body " + url, e);
			}
		}

		try {
			int status = connection.getResponseCode();
			if (status >= 400) {
				InputStream stream = connection.getErrorStream();
				String error = IOUtils.toString(stream, StandardCharsets.UTF_8);
				return DataOrThrowable.error(error);
			}

			if (!connection.getContentType().startsWith("application/json")) {
				return DataOrThrowable.error("Unexpected content type " + connection.getContentType());
			}

			InputStream stream = connection.getInputStream();
			String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
			// Methods like POST and PATCH are unlikely to return a matching value so it's not worth it saving the hash
			if (method.equalsIgnoreCase("get")) {
				// Using hashes isn't ideal since it does the entire data fetch and download, but I couldn't use ETag
				// This is mostly to avoid constantly reprocessing and swapping the data in the data manager
				String hash = Constants.SHA_512.hashString(content, StandardCharsets.UTF_8).toString();
				String cachedHash = hashes.getIfPresent(endpoint);
				if (hash.equals(cachedHash)) return DataOrThrowable.data(null);

				hashes.put(endpoint, hash);
			}

			return DataOrThrowable.of(decoder.parse(JsonOps.INSTANCE, JsonParser.parseString(content)));
		} catch (IOException e) {
			return DataOrThrowable.throwable("Failed to read from " + url, e);
		}
	}

	private URL createUrl(String endpoint) throws MalformedURLException {
		return platformUri.resolve(endpoint).toURL();
	}

	private HttpURLConnection createConnection(String method, URL url) throws IOException {
		LOGGER.debug("Opening {} connection to {}", method, url);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(method);
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setUseCaches(false);

		connection.addRequestProperty("Mf-Event-Token", config.platformToken());

		return connection;
	}

	private <T extends @Nullable Object> void setConnectionBody(HttpURLConnection connection, T data, Encoder<T> encoder) throws IOException {
		DataResult<JsonElement> encoded = encoder.encodeStart(JsonOps.COMPRESSED, data);
		String body = GSON.toJson(encoded.getOrThrow(error -> new IOException("Failed to encode data for request; " + error)));
		byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

		connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
		connection.setRequestProperty(HttpHeaders.CONTENT_LENGTH, Integer.toString(bodyBytes.length));
		connection.setDoOutput(true);

		try (OutputStream outputStream = connection.getOutputStream()) {
			IOUtils.write(bodyBytes, outputStream);
		}
	}
}
