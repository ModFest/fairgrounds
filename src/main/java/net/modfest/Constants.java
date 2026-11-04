package net.modfest;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class Constants {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final HashFunction SHA_512 = Hashing.sha512();
	public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();
	public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();

	public static class Platform {
		public static final Path CONFIG_FILE = CONFIG_DIR.resolve("fairgrounds/platform.json");
		public static final Path USERS_CACHE_FILE = GAME_DIR.resolve("platform_user_data_cache.bin");
		public static final Path SUBMISSIONS_CACHE_FILE = GAME_DIR.resolve("platform_submissions_cache.bin");
	}
}
