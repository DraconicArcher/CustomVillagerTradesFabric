package uk.co.dotcode.customvillagertrades.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.fabricmc.loader.api.FabricLoader;
import uk.co.dotcode.customvillagertrades.CVT;
import uk.co.dotcode.customvillagertrades.ConfigHandler;

public class CVTFabric implements ModInitializer {

	@Override
	public void onInitialize() {

		// Register commands
		CommandRegistryFabric.register();

		// Set up configuration paths
		ConfigHandler.setup(
				FabricLoader.getInstance()
						.getConfigDir()
						.toFile()
		);

// Load configuration
		ConfigHandler.init();


		// Register server lifecycle
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {

			ConfigHandler.ACCESS =
					server.registryAccess();

			CVT.TRADE_UTIL =
					new TradeUtilImpl(
							ConfigHandler.ACCESS
					);

			ConfigHandler.finalizeTrades();

			CVT.init();
		});
	}
}
