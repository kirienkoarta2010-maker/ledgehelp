package com.example.ledgehelp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Клиентская таблица поз игроков (используется только для отрисовки и блокировки управления). */
public final class PoseState {
    public static final int NONE = 0;
    public static final int HANG = 1;   // висит на краю
    public static final int REACH = 2;  // помощник протягивает руку вниз
    public static final int HAUL = 3;   // помощник тянет
    public static final int CLIMB = 4;  // висящего подтягивают наверх

    private static final Map<UUID, Integer> POSES = new ConcurrentHashMap<>();

    public static void set(UUID id, int pose) {
        if (pose == NONE) POSES.remove(id);
        else POSES.put(id, pose);
    }

    public static int get(UUID id) {
        return POSES.getOrDefault(id, NONE);
    }

    public static void clear() {
        POSES.clear();
    }

    private PoseState() {}
}
