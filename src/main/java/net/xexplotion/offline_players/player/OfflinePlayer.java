package net.xexplotion.offline_players.player;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.world.ServerWorld;

public class OfflinePlayer extends FakePlayer {
    public OfflinePlayer(ServerWorld world, GameProfile profile) {
        super(world, profile);
    }
//
//    @Override
//    public boolean isSpectator() {
//        return true;
//    }



}
