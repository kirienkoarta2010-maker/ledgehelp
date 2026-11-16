package com.example.ledgehelp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Вся серверная логика: повисание на краю, протягивание руки, вытягивание, подсказки. */
@EventBusSubscriber(modid = LedgeHelp.MODID)
public final class LedgeServer {
    private static final int REACH_TICKS = 20; // рука тянется вниз (1 сек)
    private static final int HAUL_TICKS = 30;  // подтягивание (1.5 сек)

    private static final int HINT_NONE = 0;
    private static final int HINT_GRAB = 1;
    private static final int HINT_HELP = 2;

    private record Hang(BlockPos ledge, Direction dir, Vec3 pos) {}

    private static final class Helper {
        final UUID target;
        int ticks;
        Helper(UUID target) { this.target = target; }
    }

    private static final Set<UUID> HOLDING = new HashSet<>();
    private static final Map<UUID, Hang> HANGING = new HashMap<>();
    private static final Map<UUID, Helper> HELPERS = new HashMap<>();
    private static final Set<UUID> BEING_HELPED = new HashSet<>();
    private static final Set<UUID> CLIMBING = new HashSet<>();
    private static final Map<UUID, Integer> POSES = new HashMap<>();
    private static final Map<UUID, Integer> HINTS = new HashMap<>();
    private static final Set<UUID> DEBUG = new HashSet<>();

    /** Включает/выключает отладочные сообщения над хотбаром (команда /ledgehelp debug). */
    public static boolean toggleDebug(ServerPlayer p) {
        UUID id = p.getUUID();
        if (DEBUG.remove(id)) return false;
        DEBUG.add(id);
        return true;
    }

    private static void debug(ServerPlayer p, String msg) {
        if (DEBUG.contains(p.getUUID())) p.displayClientMessage(Component.literal("[LedgeHelp] " + msg), true);
    }

    public static void setHolding(ServerPlayer p, boolean holding) {
        if (holding) {
            if (HOLDING.add(p.getUUID())) debug(p, "клавиша G принята сервером");
        } else {
            HOLDING.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.isSpectator()) return;
        UUID id = p.getUUID();
        boolean holding = HOLDING.contains(id);

        Helper helper = HELPERS.get(id);
        if (helper != null) {
            setHint(p, HINT_NONE);
            tickHelper(p, helper, holding);
            return;
        }

        Hang hang = HANGING.get(id);
        if (hang != null) {
            setHint(p, HINT_NONE);
            tickHanging(p, hang, holding);
            return;
        }

        if (p.onGround()) {
            ServerPlayer target = findTarget(p);
            setHint(p, target != null ? HINT_HELP : HINT_NONE);
            if (holding && target != null) startHelping(p, target);
        } else {
            Hang found = findLedge(p);
            setHint(p, found != null ? HINT_GRAB : HINT_NONE);
            if (holding && found != null) grab(p, found);
            else if (holding && p.tickCount % 20 == 0) debug(p, "в воздухе, G зажата, но край не найден");
        }
    }

    // ---------- повисание ----------

    /** Ищет край, за который можно схватиться (со всех четырёх сторон). */
    private static Hang findLedge(ServerPlayer p) {
        if (p.isInWater() || p.getAbilities().flying || p.isPassenger()) return null;
        Level level = p.level();
        Direction facing = p.getDirection();
        Direction[] order = { facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite() };
        double[] dists = { 0.7, 1.2, 1.7 };                       // как далеко от игрока может быть стена
        double[] heights = { 0.6, 1.1, 1.6, 2.1, 2.6, 3.0 };      // на каких высотах от ног ищем

        for (Direction dir : order) {
            for (double dist : dists) {
                for (double h : heights) {
                    BlockPos ledge = BlockPos.containing(
                            p.getX() + dir.getStepX() * dist, p.getY() + h, p.getZ() + dir.getStepZ() * dist);
                    if (!solidTop(level, ledge) || !isFree(level, ledge.above()) || !isFree(level, ledge.above(2))) continue;

                    double top = ledge.getY() + 1.0;
                    double reach = top - p.getY();
                    if (reach < 0.9 || reach > 3.0) continue; // руки должны достать

                    Vec3 pos = new Vec3(ledge.getX() + 0.5 - dir.getStepX() * 0.8, top - 1.9,
                            ledge.getZ() + 0.5 - dir.getStepZ() * 0.8);
                    if (!level.noCollision(p, p.getBoundingBox().move(pos.subtract(p.position())))) continue;

                    return new Hang(ledge, dir, pos);
                }
            }
        }
        return null;
    }

    private static void grab(ServerPlayer p, Hang hang) {
        HANGING.put(p.getUUID(), hang);
        p.setNoGravity(true);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        teleport(p, hang.pos(), hang.dir());
        setPose(p, PoseState.HANG);
        debug(p, "схватился за край");
    }

    private static void tickHanging(ServerPlayer p, Hang hang, boolean holding) {
        if (CLIMBING.contains(p.getUUID())) return; // двигает помощник
        if (!holding || p.isShiftKeyDown() || !solidTop(p.level(), hang.ledge())) {
            release(p);
            return;
        }
        p.setNoGravity(true);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        if (p.position().distanceToSqr(hang.pos()) > 0.04) teleport(p, hang.pos(), hang.dir());
    }

    // ---------- помощь ----------

    /** Ищет висящего игрока, которому можно помочь. */
    private static ServerPlayer findTarget(ServerPlayer p) {
        Vec3 pp = p.position();
        for (ServerPlayer t : p.serverLevel().players()) {
            if (t == p) continue;
            UUID tid = t.getUUID();
            Hang hang = HANGING.get(tid);
            if (hang == null || CLIMBING.contains(tid) || BEING_HELPED.contains(tid)) continue;
            if (Math.abs(pp.y - (hang.ledge().getY() + 1.0)) > 0.6) continue;
            double dx = t.getX() - pp.x, dz = t.getZ() - pp.z;
            if (dx * dx + dz * dz > 3.5 * 3.5) continue;
            return t;
        }
        return null;
    }

    private static void startHelping(ServerPlayer p, ServerPlayer target) {
        HELPERS.put(p.getUUID(), new Helper(target.getUUID()));
        BEING_HELPED.add(target.getUUID());
        setPose(p, PoseState.REACH);
    }

    private static void tickHelper(ServerPlayer p, Helper h, boolean holding) {
        ServerPlayer t = p.server.getPlayerList().getPlayer(h.target);
        Hang hang = HANGING.get(h.target);
        if (t == null || hang == null || !t.isAlive()) { endHelp(p, h); return; }

        boolean haul = h.ticks >= REACH_TICKS;
        // пока рука только тянется, помощник может отменить; потом - уже не отменить
        if (!haul && (!holding || !p.onGround() || p.distanceToSqr(t) > 25.0)) { endHelp(p, h); return; }

        h.ticks++;
        if (h.ticks < REACH_TICKS) { setPose(p, PoseState.REACH); return; }

        setPose(p, PoseState.HAUL);
        setPose(t, PoseState.CLIMB);
        CLIMBING.add(t.getUUID());

        double prog = Math.min(1.0, (h.ticks - REACH_TICKS) / (double) HAUL_TICKS);
        BlockPos l = hang.ledge();
        Vec3 a = hang.pos();
        double up = ease(Math.min(1.0, prog / 0.7));              // сначала вверх
        double in = ease(Math.max(0.0, (prog - 0.7) / 0.3));      // потом на площадку
        Vec3 v = new Vec3(
                Mth.lerp(in, a.x, l.getX() + 0.5),
                Mth.lerp(up, a.y, l.getY() + 1.02),
                Mth.lerp(in, a.z, l.getZ() + 0.5));
        teleport(t, v, hang.dir());

        if (prog >= 1.0) {
            release(t);
            endHelp(p, h);
        }
    }

    private static void endHelp(ServerPlayer p, Helper h) {
        HELPERS.remove(p.getUUID());
        BEING_HELPED.remove(h.target);
        setPose(p, PoseState.NONE);
        ServerPlayer t = p.server.getPlayerList().getPlayer(h.target);
        if (t != null && CLIMBING.contains(h.target)) release(t); // помощь оборвалась на подъёме
    }

    private static void release(ServerPlayer t) {
        UUID id = t.getUUID();
        HANGING.remove(id);
        CLIMBING.remove(id);
        BEING_HELPED.remove(id);
        t.setNoGravity(false);
        t.fallDistance = 0;
        setPose(t, PoseState.NONE);
    }

    // ---------- уборка ----------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            p.sendSystemMessage(Component.literal("Ledge Help " + LedgeHelp.VERSION
                    + " загружен. Клавиша G: схватиться за край / помочь. Команда /ledgehelp debug включает отладку."));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) cleanup(p);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) cleanup(p);
    }

    private static void cleanup(ServerPlayer p) {
        UUID id = p.getUUID();
        HOLDING.remove(id);
        Helper h = HELPERS.get(id);
        if (h != null) endHelp(p, h);
        if (HANGING.containsKey(id)) release(p);
        POSES.remove(id);
        HINTS.remove(id);
    }

    // ---------- утилиты ----------

    private static void setPose(ServerPlayer p, int pose) {
        UUID id = p.getUUID();
        if (POSES.getOrDefault(id, PoseState.NONE) == pose) return;
        if (pose == PoseState.NONE) POSES.remove(id);
        else POSES.put(id, pose);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new PoseSyncPayload(id, pose));
    }

    private static void setHint(ServerPlayer p, int hint) {
        UUID id = p.getUUID();
        if (HINTS.getOrDefault(id, HINT_NONE) == hint) return;
        if (hint == HINT_NONE) HINTS.remove(id);
        else HINTS.put(id, hint);
        PacketDistributor.sendToPlayer(p, new HintPayload(hint));
    }

    private static void teleport(ServerPlayer p, Vec3 v, Direction dir) {
        p.connection.teleport(v.x, v.y, v.z, dir.toYRot(), p.getXRot());
    }

    private static boolean solidTop(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean isFree(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static double ease(double x) {
        return x * x * (3.0 - 2.0 * x);
    }

    private LedgeServer() {}
}
