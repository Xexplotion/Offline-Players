package net.xexplotion.offline_players.player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import net.xexplotion.offline_players.PlayerListPacketAccess;

import java.util.List;
import java.util.UUID;

public class PlayerUtil {
    public static void addOrUpdateFakePlayer(ServerWorld world, String username, UUID playerUUID, int secondsAgo) {
        PlayerRemoveS2CPacket removeS2CPacket = new PlayerRemoveS2CPacket(List.of(playerUUID));
        world.getServer().getPlayerManager().sendToAll(removeS2CPacket);

        if(username == null) return;

        GameProfile offlineProfile = new GameProfile(playerUUID, username);
        offlineProfile.getProperties().put("offline", new Property("offline", "true"));
        offlineProfile.getProperties().put("offlineUUID", new Property("uuid", playerUUID.toString()));

        OfflinePlayer offlinePlayer = new OfflinePlayer(world, offlineProfile);

        PlayerListS2CPacket addPlayerPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, offlinePlayer);
        PlayerListS2CPacket updateListedPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_LISTED, offlinePlayer);
        PlayerListS2CPacket updateLatencyPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_LATENCY, offlinePlayer);
        PlayerListS2CPacket updateDisplayNamePacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, offlinePlayer);
        PlayerListS2CPacket.Entry newEntry = new PlayerListS2CPacket.Entry(
                offlinePlayer.getUuid(),
                offlinePlayer.getGameProfile(),
                true,
                -1,
                GameMode.SPECTATOR,
                getAlignedTabName(offlinePlayer.getDisplayName().getString(), formatTimeAgo(secondsAgo)),
                null
        );

        ((PlayerListPacketAccess) updateDisplayNamePacket).offline_players$setEntries(List.of(newEntry));
        ((PlayerListPacketAccess) updateLatencyPacket).offline_players$setEntries(List.of(newEntry));

        world.getServer().getPlayerManager().sendToAll(addPlayerPacket);
        world.getServer().getPlayerManager().sendToAll(updateListedPacket);
        world.getServer().getPlayerManager().sendToAll(updateLatencyPacket);
        world.getServer().getPlayerManager().sendToAll(updateDisplayNamePacket);
    }

    public static Text getAlignedTabName(String username, String extraInfo) {
        final int targetLength = 10; // pick max expected name length (e.g., 16 chars)
        String padded = username;

        int spacesNeeded = targetLength - username.length();
        for (int i = 0; i < spacesNeeded + 3; i++) { // +3 = your custom padding
            padded += " ";
        }

        // Optional: color components separately
        return Text.literal(padded + extraInfo).formatted(Formatting.GRAY);
//        PlayerListHeaderS2CPacket
    }
    public static String formatTimeAgo(int millisAgo) {
        int totalSeconds = millisAgo;
        int secondsInMinute = 60;
        int secondsInHour = 60 * secondsInMinute;
        int secondsInDay = 24 * secondsInHour;
        int secondsInWeek = 7 * secondsInDay;
        int secondsInMonth = 30 * secondsInDay;
        int secondsInYear = secondsInDay * 365; // Simplified year

        int years = totalSeconds / secondsInYear;
        int months = totalSeconds / secondsInMonth;
        int weeks = (totalSeconds % secondsInMonth) / secondsInWeek;
        int days = (totalSeconds % secondsInWeek) / secondsInDay;
        int hours = (totalSeconds % secondsInDay) / secondsInHour;
        int minutes = (totalSeconds % secondsInHour) / secondsInMinute;
        int seconds = totalSeconds % secondsInMinute;

        StringBuilder sb = new StringBuilder();
        if (years > 0) {
            sb.append(years).append("yr ");
            if (months > 0) sb.append(months).append("mo");
        }
        if (months > 0) {
            sb.append(months).append("mo ");
            if (weeks > 0) sb.append(weeks).append("w");
        } else if (weeks > 0) {
            sb.append(weeks).append("w ");
            if (days > 0) sb.append(days).append("d");
        } else if (days > 0) {
            sb.append(days).append("d ");
            if (hours > 0) sb.append(hours).append("h");
        } else if (hours > 0) {
            sb.append(hours).append("h ");
            int roundedMinutes = (minutes >= 45) ? 0 : (minutes >= 15 ? 30 : 0);
            if (roundedMinutes > 0) sb.append(roundedMinutes).append("m");
        } else if (minutes > 0) {
            sb.append(minutes).append("m ");
            int roundedSeconds = (seconds >= 45) ? 0 : (seconds >= 15 ? 30 : 0);
            if (roundedSeconds > 0) sb.append(roundedSeconds).append("s");
        } else {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }
}
