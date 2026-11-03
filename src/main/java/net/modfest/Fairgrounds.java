package net.modfest;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.resources.Identifier;

import net.modfest.commands.FairgroundsCommands;
import net.modfest.config.PlatformConfig;
import net.modfest.platform.data.PlatformDataManager;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class Fairgrounds implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Fairgrounds");
	public static final String MOD_ID = "fairgrounds";

	private static PlatformConfig platformConfig = PlatformConfig.load();
	private static PlatformDataManager dataManager = new PlatformDataManager(platformConfig);

	@Override
	public void onInitialize() {
		FairgroundsCommands.init();
		dataManager.load();

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			dataManager.shutdown().thenCompose(_ -> dataManager.save());
		});
	}

	public static PlatformDataManager platformData() {
		return dataManager;
	}

	public static CompletableFuture<Void> reloadConfig() {
		platformConfig = PlatformConfig.load();

		return dataManager.shutdown()
			.thenCompose(_ -> dataManager.save())
			.thenCompose(_ -> new PlatformDataManager(platformConfig).load())
			.thenAccept(manager -> dataManager = manager);
	}

	public static Identifier id(String... path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, String.join("/", path));
	}
}
