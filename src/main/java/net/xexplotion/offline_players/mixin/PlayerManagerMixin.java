package net.xexplotion.offline_players.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.xexplotion.offline_players.OfflinePlayers;
import net.xexplotion.offline_players.StateSaverAndLoader;
import net.xexplotion.offline_players.player.PlayerUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Shadow @Final private List<ServerPlayerEntity> players;


    @Shadow public abstract void sendToAll(Packet<?> packet);

    @Shadow public abstract List<ServerPlayerEntity> getPlayerList();

    @Shadow public abstract @Nullable ServerPlayerEntity getPlayer(String name);

    @Shadow @Final private Map<UUID, ServerPlayerEntity> playerMap;

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void connect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
//        if(player.)
        ServerPlayerEntity offlinePlayer = getPlayer(player.getDisplayName().getString());

        if(offlinePlayer == null) {
            OfflinePlayers.LOGGER.error("Unable to remove offline player. Is the server lagging? Are they online?");
            return;
        }
//
//        offlinePlayer.getServerWorld().removePlayer(offlinePlayer, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
//        offlinePlayer.onDisconnect();
//        this.players.remove(offlinePlayer);
//        this.playerMap.remove(offlinePlayer.getUuid(), offlinePlayer);

        this.sendToAll(new PlayerRemoveS2CPacket(List.of(offlinePlayer.getUuid())));
    }

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void postConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        StateSaverAndLoader serverState = StateSaverAndLoader.get(player.getServerWorld());

        this.sendToAll(new PlayerListHeaderS2CPacket(
                Text.literal("Players online: " + player.getServerWorld().getPlayers().size()),
                Text.literal("")
        ));

        for (UUID offlinePlayerUUID : serverState.logoutTimes.keySet()) {
            long logoutTime = serverState.getLogoutTime(offlinePlayerUUID);
            long secondsAgo = (System.currentTimeMillis() - logoutTime) / 1000;
            ServerPlayerEntity offlinePlayer = server.getPlayerManager().getPlayer(offlinePlayerUUID);
            if(offlinePlayer != null) {

                OfflinePlayers.LOGGER.error("Cannot spawn offline player for " + offlinePlayer.getDisplayName().getString() + ". Are they online?");
                continue;
            }

            PlayerUtil.addOrUpdateFakePlayer(
                    server.getOverworld(),
                    serverState.uuidToUsername.get(offlinePlayerUUID),
                    offlinePlayerUUID,
                    (int) secondsAgo
            );
        }

    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void disconnect(ServerPlayerEntity player, CallbackInfo ci) {
        StateSaverAndLoader serverState = StateSaverAndLoader.get(player.getServerWorld());
        serverState.setLogoutTime(player.getUuid(), System.currentTimeMillis());
        serverState.uuidToUsername.remove(player.getUuid(), player.getDisplayName().getString());
        serverState.uuidToUsername.put(player.getUuid(), player.getDisplayName().getString());

//
        if(getPlayer(player.getGameProfile().getName()) != null) return;

        long secondsAgo = (System.currentTimeMillis() - serverState.getLogoutTime(player.getUuid())) / 1000;


        PlayerUtil.addOrUpdateFakePlayer(player.getServerWorld(), player.getDisplayName().getString(), player.getUuid(), (int) secondsAgo);

        this.sendToAll(new PlayerListHeaderS2CPacket(
                Text.literal("Players online: " + player.getServerWorld().getPlayers().size()),
                Text.literal("")
        ));
    }


}
