package tw.cozy.furniture;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tw.cozy.furniture.model.Geo;
import tw.cozy.furniture.model.Model;
import tw.cozy.furniture.model.Part;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 鞦韆的擺動動畫。
 * <p>
 * 擺角 = 振幅 × sin(相位)，相位以單擺週期 2π√(L/g) 前進；振幅依照狀態平滑變化：
 * 無人時隨風輕晃，有人坐時維持中等幅度，按住 W 會越盪越高，按 S 煞車。
 * 展示實體的變換插值讓客戶端畫面保持流暢。
 */
public final class SwingManager {

    private static final int UPDATE_INTERVAL = 2;
    private static final double GRAVITY = 9.8;

    private final CozyFurniture plugin;
    private final Map<String, State> states = new HashMap<>();
    private long ticks;

    public SwingManager(CozyFurniture plugin) {
        this.plugin = plugin;
    }

    private static final class State {
        final Model model;
        final Location pivot;
        final float yaw;
        final Map<Integer, BlockDisplay> parts = new HashMap<>();
        final double omega;
        final double windSeed;
        double phase;
        double amplitude;
        double angle;
        double lastVelocitySign = 1;
        @Nullable Player rider;
        @Nullable ItemDisplay seat;

        State(Model model, Location pivot, float yaw) {
            this.model = model;
            this.pivot = pivot;
            this.yaw = yaw;
            this.omega = Math.sqrt(GRAVITY / Math.max(0.5, model.ropeLength));
            this.windSeed = ThreadLocalRandom.current().nextDouble(1000);
            this.phase = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        }
    }

    // ─────────────────── 實體追蹤（區塊載入 / 卸載） ───────────────────

    public void track(Entity e) {
        if (!(e instanceof BlockDisplay d) || !Keys.ROLE_MOVING.equals(Keys.role(d))) {
            return;
        }
        FurnitureType type = Keys.type(d);
        String id = Keys.id(d);
        Integer index = d.getPersistentDataContainer().get(Keys.INDEX, PersistentDataType.INTEGER);
        if (type == null || id == null || index == null || !type.model().swing) {
            return;
        }
        State s = states.computeIfAbsent(id, k -> {
            State st = new State(type.model(), d.getLocation(), d.getYaw());
            st.amplitude = plugin.settings().swingIdle * type.model().windiness;
            return st;
        });
        s.parts.put(index, d);
    }

    public void untrack(Entity e) {
        if (!(e instanceof BlockDisplay d) || !Keys.ROLE_MOVING.equals(Keys.role(d))) {
            return;
        }
        State s = states.get(Keys.id(d));
        if (s != null) {
            s.parts.values().removeIf(p -> p.getUniqueId().equals(d.getUniqueId()));
        }
    }

    public void forget(String id) {
        states.remove(id);
    }

    // ─────────────────────────── 騎乘 ───────────────────────────

    public void setRider(String id, Player player, ItemDisplay seat) {
        State s = states.get(id);
        if (s != null) {
            s.rider = player;
            s.seat = seat;
        }
    }

    public void clearRider(String id, @Nullable Player player) {
        State s = states.get(id);
        if (s != null && (player == null || player.equals(s.rider))) {
            s.rider = null;
            s.seat = null;
        }
    }

    /** 目前擺角下的座位世界座標；若不是鞦韆則回傳 null。 */
    public @Nullable Location seatLocation(@Nullable String id) {
        State s = id == null ? null : states.get(id);
        return s == null ? null : seatLocation(s);
    }

    private Location seatLocation(State s) {
        Quaternionf r = Geo.xRot((float) s.angle);
        Vector3f rel = new Vector3f(s.model.seats.getFirst()).sub(s.model.pivot).rotate(r);
        Vector3f world = Geo.rotY(rel, s.yaw);
        Location loc = s.pivot.clone().add(world.x, world.y, world.z);
        loc.setYaw(s.yaw);
        loc.setPitch(0f);
        return loc;
    }

    // ─────────────────────────── 動畫 ───────────────────────────

    public void tick() {
        ticks++;
        Settings cfg = plugin.settings();
        for (Iterator<State> it = states.values().iterator(); it.hasNext(); ) {
            State s = it.next();
            s.parts.values().removeIf(p -> !p.isValid());
            if (s.rider != null && (!s.rider.isOnline() || s.seat == null || !s.seat.isValid()
                    || !s.seat.getPassengers().contains(s.rider))) {
                s.rider = null;
                s.seat = null;
            }
            if (s.parts.isEmpty() && s.rider == null) {
                it.remove();
                continue;
            }

            updateAmplitude(s, cfg);
            s.phase += s.omega / 20.0;
            if (s.phase > Math.PI * 2) {
                s.phase -= Math.PI * 2;
            }
            s.angle = s.amplitude * Math.sin(s.phase);

            if (s.rider != null) {
                double sign = Math.signum(Math.cos(s.phase));
                if (sign < 0 && s.lastVelocitySign > 0 && s.amplitude > 14) {
                    String sound = s.model.ropeLength > 3 ? "block.wood.step" : "block.chain.step";
                    s.pivot.getWorld().playSound(Sound.sound(Key.key(sound), Sound.Source.BLOCK, 0.35f, 0.7f),
                            s.pivot.getX(), s.pivot.getY(), s.pivot.getZ());
                }
                s.lastVelocitySign = sign;
            }

            if (ticks % UPDATE_INTERVAL == 0) {
                apply(s);
            }
        }
    }

    private void updateAmplitude(State s, Settings cfg) {
        if (s.rider != null) {
            Input input = s.rider.getCurrentInput();
            if (input.isForward()) {
                s.amplitude = Math.min(cfg.swingMax, s.amplitude + 0.3);
            } else if (input.isBackward()) {
                s.amplitude = Math.max(0, s.amplitude - 0.7);
            } else if (s.amplitude > cfg.swingRider) {
                s.amplitude = Math.max(cfg.swingRider, s.amplitude - 0.03);
            } else {
                s.amplitude = Math.min(cfg.swingRider, s.amplitude + 0.12);
            }
            return;
        }
        // 無人：隨風飄動，風勢以兩個慢速正弦疊加模擬陣風
        double t = ticks / 20.0 + s.windSeed;
        double wind = 0.65 + 0.25 * Math.sin(t * 0.21) + 0.1 * Math.sin(t * 0.83 + 1.7);
        double target = cfg.swingIdle * s.model.windiness * wind;
        if (s.pivot.getWorld().hasStorm()) {
            target *= cfg.stormMultiplier;
        }
        double step = s.amplitude > target ? 0.08 : 0.03;
        s.amplitude += Math.max(-step, Math.min(step, target - s.amplitude));
    }

    private void apply(State s) {
        boolean watched = false;
        for (BlockDisplay d : s.parts.values()) {
            if (!d.getTrackedBy().isEmpty()) {
                watched = true;
                break;
            }
        }
        if (watched) {
            Quaternionf r = Geo.xRot((float) s.angle);
            for (Map.Entry<Integer, BlockDisplay> en : s.parts.entrySet()) {
                int index = en.getKey();
                if (index < 0 || index >= s.model.moving.size()) {
                    continue;
                }
                Part part = s.model.moving.get(index);
                Vector3f rel = new Vector3f(part.center()).sub(s.model.pivot);
                BlockDisplay d = en.getValue();
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(UPDATE_INTERVAL);
                d.setTransformation(Geo.transform(part, r, rel));
            }
        }
        if (s.seat != null && s.rider != null) {
            Location loc = seatLocation(s).add(0, plugin.settings().seatOffset, 0);
            s.seat.teleport(loc);
        }
    }
}
