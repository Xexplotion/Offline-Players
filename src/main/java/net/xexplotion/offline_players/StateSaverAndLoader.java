package net.xexplotion.offline_players;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StateSaverAndLoader extends PersistentState {
    public Map<UUID, Long> logoutTimes = new HashMap<>();
    public Map<UUID, String> uuidToUsername = new HashMap<>();

    private static final Type<StateSaverAndLoader> type = new Type<>(
            StateSaverAndLoader::createNew, // If there's no 'StateSaverAndLoader' yet create one and refresh variables
            StateSaverAndLoader::createFromNbt, // If there is a 'StateSaverAndLoader' NBT, parse it with 'createFromNbt'
            null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
    );

    public static StateSaverAndLoader get(ServerWorld world) {

        // The first time the following 'getOrCreate' function is called, it creates a brand new 'StateSaverAndLoader' and
        // stores it inside the 'PersistentStateManager'. The subsequent calls to 'getOrCreate' pass in the saved
        // 'StateSaverAndLoader' NBT on disk to our function 'StateSaverAndLoader::createFromNbt'.
        StateSaverAndLoader state = world.getPersistentStateManager().getOrCreate(type, OfflinePlayers.MOD_ID);

        // If state is not marked dirty, when Minecraft closes, 'writeNbt' won't be called and therefore nothing will be saved.
        // Technically it's 'cleaner' if you only mark state as dirty when there was actually a change, but the vast majority
        // of mod writers are just going to be confused when their data isn't being saved, and so it's best just to 'markDirty' for them.
        // Besides, it's literally just setting a bool to true, and the only time there's a 'cost' is when the file is written to disk when
        // there were no actual change to any of the mods state (INCREDIBLY RARE).
        state.markDirty();

        return state;
    }

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

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        return writeNbt(nbt);
    }

    public static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();
        NbtList list = tag.getList("logoutTimes", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound entryNbt = (NbtCompound) element;

            UUID uuid = entryNbt.getUuid("uuid");
            long time = entryNbt.getLong("time");
            String name = entryNbt.getString("name");

            state.logoutTimes.put(uuid, time);
            state.uuidToUsername.put(uuid, name);
        }

        return state;
    }

    public static StateSaverAndLoader createNew() {
        StateSaverAndLoader state = new StateSaverAndLoader();
        state.uuidToUsername = new HashMap<>();
        state.logoutTimes = new HashMap<>();
        return state;
    }

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