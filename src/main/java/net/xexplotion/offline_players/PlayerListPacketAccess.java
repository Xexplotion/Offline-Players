package net.xexplotion.offline_players;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.List;

public interface PlayerListPacketAccess {
    void offline_players$setEntries(List<PlayerListS2CPacket.Entry> newEntries);
}
