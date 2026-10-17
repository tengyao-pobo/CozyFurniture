package tw.cozy.furniture;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 床：右鍵設定重生點；夜晚或雷雨時躺上床休息，
 * 睡覺人數達到 playersSleepingPercentage 遊戲規則後跳到早上。
 */
public final class BedManager {

    /** 全員睡著後，再等幾秒才天亮（與原版相同的 5 秒）。 */
    private static final int SLEEP_TICKS = 100;

    private final CozyFurniture plugin;
    private final Map<UUID, World> sleepers = new HashMap<>();
    private final Map<World, Integer> readyTicks = new HashMap<>();

    public BedManager(CozyFurniture plugin) {
        this.plugin = plugin;
    }

    public void use(Player player, Interaction root, @Nullable Location click) {
        World world = root.getWorld();
        Location spawn = root.getLocation().add(0, 0.6, 0);
        spawn.setYaw(player.getYaw());
        player.setRespawnLocation(spawn, true);

        if (world.getEnvironment() != World.Environment.NORMAL) {
            Msg.bar(player, "已設定重生點（這個維度無法睡覺）");
            return;
        }
        if (world.isDayTime() && !world.isThundering()) {
            Msg.bar(player, "已設定重生點。只能在夜晚或雷雨時睡覺");
            return;
        }
        if (plugin.seats().sit(player, root, click)) {
            sleepers.put(player.getUniqueId(), world);
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false, false));
            Msg.bar(player, "已設定重生點，晚安… (Shift 起床)");
        }
    }

    public void wakeUp(Player player) {
        sleepers.remove(player.getUniqueId());
    }

    /** 每秒執行一次：計算各世界的睡覺人數。 */
    public void tick() {
        sleepers.entrySet().removeIf(en -> {
            Player p = Bukkit.getPlayer(en.getKey());
            return p == null || !plugin.seats().isSeated(p.getUniqueId()) || p.getWorld() != en.getValue();
        });
        Map<World, List<Player>> byWorld = new HashMap<>();
        for (UUID id : sleepers.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                byWorld.computeIfAbsent(p.getWorld(), w -> new ArrayList<>()).add(p);
            }
        }
        readyTicks.keySet().removeIf(w -> !byWorld.containsKey(w));

        for (Map.Entry<World, List<Player>> en : byWorld.entrySet()) {
            World world = en.getKey();
            List<Player> inBed = en.getValue();
            if (world.isDayTime() && !world.isThundering()) {
                wake(inBed, "天亮了！");
                continue;
            }
            int total = 0;
            int sleeping = 0;
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || p.isSleepingIgnored()) {
                    continue;
                }
                total++;
                if (sleepers.containsKey(p.getUniqueId()) || p.isSleeping()) {
                    sleeping++;
                }
            }
            Integer percent = world.getGameRuleValue(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            int needed = Math.max(1, (int) Math.ceil(total * (percent == null ? 100 : percent) / 100.0));
            for (Player p : inBed) {
                Msg.bar(p, "睡覺中… " + sleeping + " / " + needed + " 位玩家");
            }
            if (sleeping >= needed) {
                int t = readyTicks.merge(world, 20, Integer::sum);
                if (t >= SLEEP_TICKS) {
                    readyTicks.remove(world);
                    world.setFullTime((world.getFullTime() / 24000 + 1) * 24000);
                    world.setStorm(false);
                    world.setThundering(false);
                    wake(inBed, "早安！新的一天開始了。");
                }
            } else {
                readyTicks.remove(world);
            }
        }
    }

    private void wake(List<Player> players, String message) {
        for (Player p : players) {
            sleepers.remove(p.getUniqueId());
            plugin.seats().release(p, true);
            Msg.bar(p, message);
            p.playSound(Sound.sound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, 0.4f, 1.6f));
        }
    }
}
