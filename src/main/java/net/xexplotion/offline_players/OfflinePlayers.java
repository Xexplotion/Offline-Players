package net.xexplotion.offline_players;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.xexplotion.offline_players.player.PlayerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class OfflinePlayers implements ModInitializer {
	public static final String MOD_ID = "offline_players";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerContext.SERVER = server;
		});

		ServerTickEvents.START_SERVER_TICK.register(server -> {
			long tick = server.getTicks();
			StateSaverAndLoader state = StateSaverAndLoader.get(server.getOverworld());
			if(tick % 20 == 1) {
				if(server.getPlayerManager().getPlayerList().isEmpty()) return;
				for (UUID offlinePlayerUUID : state.logoutTimes.keySet()) {
					long logoutTime = state.getLogoutTime(offlinePlayerUUID);
					long secondsAgo = (System.currentTimeMillis() - logoutTime) / 1000;
					if(shouldUpdateLogoutTime((int) secondsAgo)) {
						ServerPlayerEntity offlinePlayer = server.getPlayerManager().getPlayer(offlinePlayerUUID);
						if(offlinePlayer != null) {
							continue;
						}

						PlayerUtil.addOrUpdateFakePlayer(
								server.getOverworld(),
								state.uuidToUsername.get(offlinePlayerUUID),
								offlinePlayerUUID,
                                (int) secondsAgo
                        );
					}
				}
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register(((serverPlayNetworkHandler, minecraftServer) -> {
			StateSaverAndLoader state = StateSaverAndLoader.get(serverPlayNetworkHandler.player.getServerWorld());
			UUID uuid = serverPlayNetworkHandler.player.getUuid();
			String name = serverPlayNetworkHandler.player.getGameProfile().getName();
			state.uuidToUsername.remove(uuid);
			state.uuidToUsername.put(uuid, name);
			state.markDirty();
		}));

	}


	public static boolean shouldUpdateLogoutTime(int seconds) {
		if (seconds <= 0) return false;
		if (seconds <= 10) return true;
		if (seconds <= 40 && seconds % 10 == 0) return true;
		if (seconds <= 70 && seconds % 30 == 0) return true;
        return seconds > 70 && seconds % 60 == 0;
    }

}