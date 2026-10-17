package tw.cozy.furniture;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tw.cozy.furniture.model.Model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** 坐下功能：生成一個隱形的展示實體讓玩家騎乘。 */
public final class SeatManager {

    public record Seat(String furnitureId, int index, ItemDisplay entity) {
    }

    private final CozyFurniture plugin;
    private final Map<UUID, Seat> seats = new HashMap<>();

    public SeatManager(CozyFurniture plugin) {
        this.plugin = plugin;
    }

    public boolean isOccupied(String id, int index) {
        for (Seat s : seats.values()) {
            if (s.furnitureId.equals(id) && s.index == index) {
                return true;
            }
        }
        return false;
    }

    /** @return 是否成功坐下 */
    public boolean sit(Player player, Interaction root, @Nullable Location click) {
        if (player.isInsideVehicle() || seats.containsKey(player.getUniqueId())) {
            return false;
        }
        FurnitureType type = Keys.type(root);
        String id = Keys.id(root);
        Model model = type.model();

        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < model.seats.size(); i++) {
            if (isOccupied(id, i)) {
                continue;
            }
            Location loc = seatLocation(root, i);
            double d = click == null ? i : loc.distanceSquared(click);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        if (best < 0) {
            Msg.barError(player, "座位上已經有人了。");
            return false;
        }

        Location loc = seatLocation(root, best);
        loc.add(0, plugin.settings().seatOffset, 0);
        int index = best;
        ItemDisplay seat = root.getWorld().spawn(loc, ItemDisplay.class, e -> {
            e.setPersistent(false);
            e.setTeleportDuration(2);
            Keys.tag(e, id, type, Keys.wood(root), Keys.ROLE_SEAT);
        });
        player.setRotation(loc.getYaw(), player.getPitch());
        if (!seat.addPassenger(player)) {
            seat.remove();
            return false;
        }
        seats.put(player.getUniqueId(), new Seat(id, index, seat));
        if (model.swing) {
            plugin.swings().setRider(id, player, seat);
            Msg.bar(player, "按住 W 用力盪、S 煞車、Shift 離開");
        }
        return true;
    }

    public boolean isSeated(UUID player) {
        return seats.containsKey(player);
    }

    private Location seatLocation(Interaction root, int index) {
        Location swing = plugin.swings().seatLocation(Keys.id(root));
        return swing != null ? swing : plugin.furniture().staticSeat(root, index);
    }

    public boolean isSeat(Entity e) {
        return e instanceof ItemDisplay && Keys.ROLE_SEAT.equals(Keys.role(e));
    }

    /** 玩家離開座位（下車事件或強制）。 */
    public void release(Player player, boolean teleportUp) {
        Seat seat = seats.remove(player.getUniqueId());
        if (seat == null) {
            return;
        }
        plugin.swings().clearRider(seat.furnitureId, player);
        plugin.beds().wakeUp(player);
        Location exit = seat.entity.getLocation().add(0, 0.35, 0);
        exit.setYaw(player.getYaw());
        exit.setPitch(player.getPitch());
        if (seat.entity.getPassengers().contains(player)) {
            seat.entity.removePassenger(player);
        }
        seat.entity.remove();
        if (teleportUp) {
            plugin.furniture().runLater(() -> {
                if (player.isOnline() && !player.isInsideVehicle()) {
                    player.teleport(exit);
                }
            });
        }
    }

    public void ejectAll(String furnitureId) {
        for (Iterator<Map.Entry<UUID, Seat>> it = seats.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Seat> en = it.next();
            Seat seat = en.getValue();
            if (seat.furnitureId.equals(furnitureId)) {
                it.remove();
                plugin.swings().clearRider(furnitureId, null);
                seat.entity.eject();
                seat.entity.remove();
            }
        }
    }

    public void ejectEverything() {
        for (Seat seat : seats.values()) {
            seat.entity.eject();
            seat.entity.remove();
        }
        seats.clear();
    }
}
