package uk.co.dotcode.customvillagertrades.fabric;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import uk.co.dotcode.customvillagertrades.events.CVTCommands;

public final class CommandRegistryFabric {

	private CommandRegistryFabric() {
		// Utility class
	}

	public static void register() {

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> {

					CVTCommands.register(
							dispatcher,
							registryAccess
					);
				}
		);
	}
}