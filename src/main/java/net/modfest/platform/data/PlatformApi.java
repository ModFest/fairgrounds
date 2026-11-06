package net.modfest.platform.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.Util;
import net.modfest.Constants;
import net.modfest.config.PlatformConfig;
import net.modfest.platform.JsonBodyHandler;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static net.modfest.platform.data.PlatformDataManager.LOGGER;
import static net.modfest.util.StringTemplate.format;

public final class PlatformApi {
	private static final String GET_USERS = "users";
	private static final String GET_SUBMISSIONS = "event/{eventId}/submissions";
	private static final String PATCH_BOOTH = "event/{eventId}/booth/{subId}";

	private static final Duration CONNECT_TIMEOUT = Duration.of(5, ChronoUnit.SECONDS);
	private static final Duration READ_TIMEOUT = Duration.of(5, ChronoUnit.SECONDS);
	private static final HttpClient CLIENT = HttpClient.newBuilder()
		.connectTimeout(CONNECT_TIMEOUT)
		.executor(Util.backgroundExecutor())
		.build();

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
	// TODO: async
	// I was originally going to use ETag but the platform doesn't implement it, and the current setup would require some work to properly implement it - Luna
	/// @return Data or an error with an exception, data is null if the server response's hash matches the cached one (only on GET requests)
	private <T, D extends @Nullable Object> DataOrThrowable<T> connect(String method, String endpoint, D data, @Nullable Encoder<D> encoder, Decoder<T> decoder) {
		URI uri = platformUri.resolve(endpoint);
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
			.setHeader("Mf-Event-Token", config.platformToken())
			.timeout(READ_TIMEOUT);

		try {
			requestBuilder.uri(uri);
		} catch (IllegalArgumentException e) {
			return DataOrThrowable.throwable("Scheme for " + uri + " is not supported", e);
		}

		try {
			requestBuilder.method(method, createBodyPublisher(data, encoder));
			if (encoder != null) {
				requestBuilder.header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
			}
		} catch (IllegalArgumentException e) {
			return DataOrThrowable.throwable("The " + method + " method is invalid or restricted", e);
		} catch (IOException e) {
			return DataOrThrowable.error(e.getMessage());
		}

		HttpRequest request = requestBuilder.build();

		try {
			LOGGER.debug("Opening {} connection to {}", method, uri);
			HttpResponse<JsonElement> response = sendSync(request, new JsonBodyHandler());

			JsonElement body = response.body();

			int status = response.statusCode();
			if (status >= 400) {
				if (!body.isJsonObject()) {
					return DataOrThrowable.error(body.toString());
				}

				JsonElement error = body.getAsJsonObject().get("data");
				return DataOrThrowable.error(String.valueOf(error != null ? error : body));
			}

			// Methods like POST and PATCH are unlikely to return a matching value so it's not worth it saving the hash
			if (method.equalsIgnoreCase("get")) {
				// Using hashes isn't ideal since it does the entire data fetch and download, but I couldn't use ETag
				// This is mostly to avoid constantly reprocessing and swapping the data in the data manager
				String hash = Constants.SHA_512.hashString(body.toString(), StandardCharsets.UTF_8).toString();
				String cachedHash = hashes.getIfPresent(endpoint);
				if (hash.equals(cachedHash)) return DataOrThrowable.data(null);

				hashes.put(endpoint, hash);
			}

			return DataOrThrowable.of(decoder.parse(JsonOps.INSTANCE, body));
		} catch (IllegalArgumentException e) {
			return DataOrThrowable.throwable("Invalid request to " + uri, e);
		} catch (HttpTimeoutException e) {
			return DataOrThrowable.throwable("Timed out while connecting to " + uri, e);
		} catch (ConnectException | SSLHandshakeException | ProtocolException e) {
			return DataOrThrowable.throwable("Failed to initiate connection to " + uri, e);
		} catch (InterruptedException e) {
			return DataOrThrowable.throwable("Interrupted while connecting to " + uri, e);
		} catch (IOException e) {
			return DataOrThrowable.throwable("IO error while connecting to " + uri, e);
		} catch (ExecutionException e) {
			return DataOrThrowable.throwable("Failed to process response from " + uri, e);
		} catch (Exception e) {
			return DataOrThrowable.throwable("Unknown error while connecting to " + uri, e);
		}
	}

	private <T extends @Nullable Object> BodyPublisher createBodyPublisher(T data, @Nullable Encoder<T> encoder) throws IOException {
		if (encoder == null) return BodyPublishers.noBody();

		DataResult<JsonElement> encoded = encoder.encodeStart(JsonOps.INSTANCE, data);
		JsonElement body = encoded.getOrThrow(error -> new IOException("Failed to encode data for request; " + error));

		return BodyPublishers.ofString(body.toString());
	}

	private static <T> HttpResponse<T> sendSync(HttpRequest req, BodyHandler<T> responseHandler) throws IOException, InterruptedException, ExecutionException {
		if (Thread.interrupted()) throw new InterruptedException();

		CompletableFuture<HttpResponse<T>> future = null;
		try {
			future = CLIENT.sendAsync(req, responseHandler);
			return future.get();
		} catch (InterruptedException ie) {
			future.cancel(true);
			throw ie;
		} catch (ExecutionException e) {
			final Throwable throwable = e.getCause();
			final String msg = throwable.getMessage();

			switch (throwable) {
				case IllegalArgumentException _ -> throw new IllegalArgumentException(msg, throwable);
				case SecurityException _ -> throw new SecurityException(msg, throwable);
				case HttpConnectTimeoutException _ -> {
					HttpConnectTimeoutException hcte = new HttpConnectTimeoutException(msg);
					hcte.initCause(throwable);
					throw hcte;
				}
				case HttpTimeoutException _ -> throw new HttpTimeoutException(msg);
				case ConnectException _ -> {
					ConnectException ce = new ConnectException(msg);
					ce.initCause(throwable);
					throw ce;
				}
				case SSLHandshakeException _ ->
					// special case for SSLHandshakeException
					throw new SSLHandshakeException(msg, throwable);
				case SSLException sslException ->
					// any other SSLException is wrapped in a plain
					// SSLException
					throw new SSLException(msg, throwable);
				case ProtocolException _ -> {
					ProtocolException pe = new ProtocolException(msg);
					pe.initCause(throwable);
					throw pe;
				}
				case IOException _ -> throw new IOException(msg, throwable);
				default -> throw new ExecutionException(msg, throwable);
			}
		}
	}
}
