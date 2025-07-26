package net.xexplotion.offline_players;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StateSaverAndLoader extends PersistentState {
    public final Map<UUID, Long> logoutTimes = new HashMap<>();
    public final Map<UUID, String> uuidToUsername = new HashMap<>();


    public static StateSaverAndLoader get(ServerWorld world) {
        return world.getPersistentStateManager()
                .getOrCreate(StateSaverAndLoader::new, StateSaverAndLoader::new, "logout_times");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Long> entry : logoutTimes.entrySet()) {
            NbtCompound entryNbt = new NbtCompound();
            entryNbt.putUuid("uuid", entry.getKey());
            entryNbt.putLong("time", entry.getValue());
            entryNbt.putString("name", uuidToUsername.getOrDefault(entry.getKey(), ""));
            list.add(entryNbt);
        }
        nbt.put("logoutTimes", list);
        return nbt;
    }

    public StateSaverAndLoader() {}

    public StateSaverAndLoader(NbtCompound nbt) {
        NbtList list = nbt.getList("logoutTimes", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound entryNbt = (NbtCompound) element;

            UUID uuid = entryNbt.getUuid("uuid");
            long time = entryNbt.getLong("time");
            String name = entryNbt.getString("name");

            logoutTimes.put(uuid, time);
            uuidToUsername.put(uuid, name);
        }
    }

    public void setLogoutTime(UUID uuid, long time) {
        logoutTimes.put(uuid, time);
        markDirty();
    }

    public Long getLogoutTime(UUID uuid) {
        return logoutTimes.get(uuid);
    }
}