package com.example.ledgehelp;

/** Клиентское состояние подсказки. Подсказка чуть "задерживается", чтобы не мигать за один-два тика. */
public final class ClientHintState {
    private static final int LINGER_TICKS = 10;
    private static int hint;
    private static int expireTick;
    private static int clientTick;

    public static void tick() {
        clientTick++;
    }

    public static void set(int newHint) {
        if (newHint != 0) {
            hint = newHint;
            expireTick = clientTick + LINGER_TICKS;
        }
    }

    public static boolean active() {
        return hint != 0 && clientTick <= expireTick;
    }

    public static void clear() {
        hint = 0;
        expireTick = 0;
    }

    private ClientHintState() {}
}
