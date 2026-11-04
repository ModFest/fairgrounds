package net.modfest.platform.data;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.modfest.Constants;
import net.modfest.buffer.HashingByteBuf;
import net.modfest.config.PlatformConfig;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.modfest.Constants.Platform.SUBMISSIONS_CACHE_FILE;
import static net.modfest.Constants.Platform.USERS_CACHE_FILE;

public class PlatformDataManager {
	public static final Logger LOGGER = LoggerFactory.getLogger("Fairgrounds Platform Data");
	public static final int CONNECT_TIMEOUT_MS = 5000;
	public static final int READ_TIMEOUT_MS = 5000;

	private static final ScheduledExecutorService EXECUTOR;

	private byte[] usersHash = new byte[0];
	private byte[] submissionsHash = new byte[0];
	private final Map<UUID, UserData> users = new HashMap<>();
	private final Map<String, SubmissionData> submissions = new HashMap<>();
	private final PlatformConfig config;
	private final PlatformApi api;

	private @Nullable ScheduledFuture<?> updater;

	public PlatformDataManager(PlatformConfig config) {
		this.config = config;
		this.api = new PlatformApi(config);
	}

	public CompletableFuture<PlatformDataManager> start() {
		return stopUpdater().thenApply(_ -> {
			this.loadCacheInternal();
			updater = EXECUTOR.scheduleAtFixedRate(() -> {
				try {
					updateData();
				} catch (Error e) {
					LOGGER.error("Failed to update platform data, Java error, not attempting to recover", e);
					// We do not want to catch errors, so we will rethrow it and let the task fail
					throw e;
				} catch (Throwable e) {
					LOGGER.error("Failed to update platform data, uncaught exception", e);
				}
			}, 0, config.fetchPeriodMinutes(), TimeUnit.MINUTES);
			return this;
		});
	}

	public CompletableFuture<PlatformDataManager> save() {
		return CompletableFuture.supplyAsync(() -> {
			LOGGER.info("Saving platform data");
			saveUserDataCache();
			saveSubmissionCache();
			return this;
		});
	}

	public CompletableFuture<PlatformDataManager> stopUpdater() {
		return CompletableFuture.supplyAsync(() -> {
			if (updater == null) return this;

			ScheduledFuture<?> updater = this.updater;
			this.updater = null;
			updater.cancel(false);
			if (updater.getDelay(TimeUnit.NANOSECONDS) <= 0) {
				try {
					// 5 seconds for each fetch + 1 extra second for processing n stuff
					updater.get(11, TimeUnit.SECONDS);
				} catch (ExecutionException | InterruptedException _) {
				} catch (TimeoutException _) {
					LOGGER.warn("Updater thread took too long to stop, interrupting it.");
					updater.cancel(true);
				}
			}

			return this;
		});
	}

	public @Nullable UserData getUser(UUID uuid) {
		synchronized (users) {
			return users.get(uuid);
		}
	}

	public Map<String, SubmissionData> getSubmissions() {
		return submissions;
	}

	public @Nullable SubmissionData getSubmission(String name) {
		synchronized (submissions) {
			return submissions.get(name);
		}
	}

	public void updateSubmission(SubmissionData submission) {
		SubmissionData old = this.submissions.get(submission.id());
		if (old == null) {
			LOGGER.error("Tried to update data for {} but could not find it in the map", submission.id());
			return;
		}

		if (submission == old) return;

		submission.authorAccounts().clear();
		submission.authorAccounts().addAll(old.authorAccounts());
		this.submissions.put(submission.id(), submission);
	}

	public PlatformApi api() {
		return api;
	}

	private void loadCacheInternal() {
		readUserDataCache();
		readSubmissionCache();
	}

	private void readUserDataCache() {
		File file = USERS_CACHE_FILE.toFile();
		if (!file.exists()) return;

		try (FileInputStream fileStream = new FileInputStream(file)) {
			// Skip if the file is empty
			if (fileStream.getChannel().size() == 0) return;

			HashingByteBuf hasher = createHashingBuf(fileStream.getChannel());
			FriendlyByteBuf byteBuf = new FriendlyByteBuf(hasher);

			if (byteBuf.readLong() != UserData.MAGIC) {
				LOGGER.warn("User data cache file is invalid");
				return;
			}

			byte[] hash = byteBuf.readByteArray(hasher.hashBits() >> 3);
			hasher.resetHasher();

			// Stream codecs are in binary, so we can use them to make the
			// code for saving and writing the cache file easier to work with
			Map<UUID, UserData> users = UserData.MAP_STREAM_CODEC.decode(byteBuf);

			// Synchronize the map update to avoid concurrency issues
			synchronized (users) {
				this.users.clear();
				this.users.putAll(users);
			}

			byte[] calculatedHash = hasher.hash().asBytes();
			if (!Arrays.equals(hash, calculatedHash)) throw new IllegalStateException("User cache hash mismatch");
			usersHash = calculatedHash;
		} catch (FileNotFoundException _) {
		} catch (IOException e) {
			LOGGER.warn("Failed to read the user data cache", e);
		} catch (Exception e) {
			LOGGER.error("Unexpected error whilst reading user data cache file", e);
		}
	}

	private void readSubmissionCache() {
		File file = SUBMISSIONS_CACHE_FILE.toFile();
		if (!file.exists()) return;

		try (FileInputStream fileStream = new FileInputStream(file)) {
			// Skip if the file is empty
			if (fileStream.getChannel().size() == 0) return;

			HashingByteBuf hasher = createHashingBuf(fileStream.getChannel());
			FriendlyByteBuf byteBuf = new FriendlyByteBuf(hasher);

			long magic = byteBuf.readLong();
			if (magic != SubmissionData.MAGIC) {
				LOGGER.warn("Submission cache file is invalid");
				return;
			}

			byte[] hash = byteBuf.readByteArray(hasher.hashBits() >> 3);
			hasher.resetHasher();

			// Stream codecs are in binary, so we can use them to make the
			// code for saving and writing the cache file easier to work with
			Map<String, SubmissionData> submissions = SubmissionData.MAP_STREAM_CODEC.decode(byteBuf);

			// Synchronize the map update to avoid concurrency issues
			synchronized (submissions) {
				this.submissions.clear();
				this.submissions.putAll(submissions);
			}

			byte[] calculatedHash = hasher.hash().asBytes();
			if (!Arrays.equals(hash, calculatedHash)) throw new IllegalStateException("Submission cache hash mismatch");
			submissionsHash = calculatedHash;
		} catch (FileNotFoundException _) {
		} catch (IOException e) {
			LOGGER.warn("Failed to read the submission cache", e);
		} catch (Exception e) {
			LOGGER.error("Unexpected error whilst reading submission cache file", e);
		}
	}

	private void saveUserDataCache() {
		File file = USERS_CACHE_FILE.toFile();
		if (!file.exists()) {
			//noinspection ResultOfMethodCallIgnored
			file.getParentFile().mkdirs();
		}

		try (FileOutputStream fileStream = new FileOutputStream(file)) {
			HashingByteBuf hasher = createHashingBuf();
			FriendlyByteBuf byteBuf = new FriendlyByteBuf(hasher);

			byteBuf.writeLong(UserData.MAGIC);
			byteBuf.writeBytes(new byte[hasher.hashBits() >> 3]);
			hasher.resetHasher();

			UserData.MAP_STREAM_CODEC.encode(byteBuf, users);

			byte[] hash = hasher.hash().asBytes();
			if (Arrays.equals(hash, usersHash)) {
				LOGGER.debug("Skipping user data save as contents are identical");
				return;
			}

			// Move back to the hash position and write it
			hasher.resetHasher();
			byteBuf.markWriterIndex();
			byteBuf.writerIndex(Long.SIZE >> 3);
			byteBuf.writeByteArray(hash);
			byteBuf.resetWriterIndex();

			fileStream.write(byteBuf.array());
		} catch (FileNotFoundException e) {
			LOGGER.error("Could not create or open the user data cache file for saving", e);
		} catch (IOException e) {
			LOGGER.warn("Failed to read the user data cache", e);
		} catch (Exception e) {
			LOGGER.error("Unexpected error whilst saving user data cache file", e);
		}
	}

	private void saveSubmissionCache() {
		File file = SUBMISSIONS_CACHE_FILE.toFile();
		if (!file.exists()) {
			//noinspection ResultOfMethodCallIgnored
			file.getParentFile().mkdirs();
		}

		try (FileOutputStream fileStream = new FileOutputStream(file)) {
			HashingByteBuf hasher = createHashingBuf();
			FriendlyByteBuf byteBuf = new FriendlyByteBuf(hasher);

			byteBuf.writeLong(SubmissionData.MAGIC);
			byteBuf.writeBytes(new byte[hasher.hashBits() >> 3]);
			hasher.resetHasher();

			SubmissionData.MAP_STREAM_CODEC.encode(byteBuf, submissions);

			byte[] hash = hasher.hash().asBytes();
			if (Arrays.equals(hash, submissionsHash)) {
				LOGGER.debug("Skipping submission save as contents are identical");
				return;
			}

			// Move back to the hash position and write it
			hasher.resetHasher();
			byteBuf.markWriterIndex();
			byteBuf.writerIndex(Long.SIZE >> 3);
			byteBuf.writeByteArray(hash);
			byteBuf.resetWriterIndex();

			fileStream.write(byteBuf.array());
		} catch (FileNotFoundException e) {
			LOGGER.error("Could not create or open the submission cache file for saving", e);
		} catch (IOException e) {
			LOGGER.warn("Failed to read the submission cache", e);
		} catch (Exception e) {
			LOGGER.error("Unexpected error whilst saving submission cache file", e);
		}
	}

	// TODO: make both fetches independent of each other, so a failed fetch doesn't fail both
	// TODO: implement retries (maybe)
	private void updateData() {
		var usersResponse = api.getUsers();
		if (usersResponse.isError()) {
			usersResponse.stderr(LOGGER, "Failed to get user data");
			return;
		}

		var submissionsResponse = api.getSubmissions();
		if (submissionsResponse.isError()) {
			submissionsResponse.stderr(LOGGER, "Failed to get submission data");
			return;
		}

		List<UserData> userData = usersResponse.data();
		List<SubmissionData> submissionData = submissionsResponse.data();

		if (userData == null && submissionData == null) {
			// Upstream hasn't changed for either, so there is no reason to update
			return;
		}

		Map<UUID, UserData> users = new HashMap<>();
		Map<String, UserData> userById = new HashMap<>();
		if (userData != null) {
			for (UserData user : userData) {
				userById.put(user.id(), user);
				for (UUID uuid : user.minecraftAccounts()) {
					users.put(uuid, user);
				}
			}
		} else {
			for (UserData user : this.users.values()) {
				userById.put(user.id(), user);
				// If user data didn't update, then submissions did
				user.submissions().clear();
			}
		}

		Map<String, SubmissionData> submissions = new HashMap<>();
		if (submissionData != null) {
			for (SubmissionData submission : submissionData) {
				submissions.put(submission.id(), submission);
				for (String author : submission.authors()) {
					UserData user = userById.get(author);
					user.submissions().add(submission);

					for (UUID account : user.minecraftAccounts()) {
						submission.authorAccounts().add(account);
					}
				}
			}
		} else {
			for (SubmissionData submission : this.submissions.values()) {
				// If submission data didn't update, then users did
				submission.authorAccounts().clear();

				for (String author : submission.authors()) {
					UserData user = userById.get(author);
					user.submissions().add(submission);

					for (UUID account : user.minecraftAccounts()) {
						submission.authorAccounts().add(account);
					}
				}
			}
		}

		if (userData != null) {
			synchronized (this.users) {
				this.users.clear();
				this.users.putAll(users);
			}
		}

		if (submissionData != null) {
			synchronized (this.submissions) {
				this.submissions.clear();
				this.submissions.putAll(submissions);
			}
		}
	}

	private HashingByteBuf createHashingBuf() {
		return new HashingByteBuf(Constants.SHA_512, Unpooled.buffer());
	}

	private HashingByteBuf createHashingBuf(FileChannel channel) throws IOException {
		MappedByteBuffer byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

		return new HashingByteBuf(Constants.SHA_512, Unpooled.wrappedBuffer(byteBuffer));
	}

	static {
		var executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
			BasicThreadFactory.builder()
				.namingPattern("Platform Data Updater %d")
				.daemon()
				.uncaughtExceptionHandler((thread, e) -> LOGGER.error("{} crashed with an unhandled exception", thread.getName(), e))
				.build()
		);
		executor.setRemoveOnCancelPolicy(true);
		EXECUTOR = Executors.unconfigurableScheduledExecutorService(executor);
	}
}
