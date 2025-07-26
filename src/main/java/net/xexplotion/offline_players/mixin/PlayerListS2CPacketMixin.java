package net.xexplotion.offline_players.mixin;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.xexplotion.offline_players.PlayerListPacketAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerListS2CPacket.class)
public class PlayerListS2CPacketMixin implements PlayerListPacketAccess {
//    @Mutable
    @Mutable @Shadow @Final private List<PlayerListS2CPacket.Entry> entries;

    @Override
    public void offline_players$setEntries(
            @Nullable List<PlayerListS2CPacket.Entry> newEntries) {
//        entries.clear();
        assert newEntries != null;
//        entries.addAll(newEntries);
        entries = new ArrayList<>(newEntries);
    }
}
