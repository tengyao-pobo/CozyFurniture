package tw.cozy.furniture;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * /furniture menu [木材]
 * /furniture give &lt;玩家&gt; &lt;種類&gt; [木材] [數量]
 * /furniture list
 * /furniture remove
 * /furniture reload
 */
public final class FurnitureCommand implements BasicCommand {

    private final CozyFurniture plugin;

    public FurnitureCommand(CozyFurniture plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu" -> menu(sender, args);
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "remove" -> remove(sender);
            case "reload" -> {
                if (!sender.hasPermission("cozyfurniture.admin")) {
                    Msg.error(sender, "你沒有權限。");
                    return;
                }
                plugin.reload();
                Msg.info(sender, "設定已重新載入。");
            }
            default -> help(sender);
        }
    }

    private void help(CommandSender sender) {
        Msg.info(sender, "可用指令：");
        Msg.info(sender, "/furniture menu [木材] - 開啟傢俱目錄");
        Msg.info(sender, "/furniture give <玩家> <種類> [木材] [數量] - 給予傢俱");
        Msg.info(sender, "/furniture list - 列出所有傢俱與木材");
        Msg.info(sender, "/furniture remove - 移除準心對準的傢俱");
        Msg.info(sender, "/furniture reload - 重新載入設定");
    }

    private void menu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "只有玩家可以開啟目錄。");
            return;
        }
        if (!sender.hasPermission("cozyfurniture.menu")) {
            Msg.error(sender, "你沒有權限。");
            return;
        }
        WoodType wood = args.length >= 2 ? WoodType.parse(args[1]) : WoodType.OAK;
        CatalogMenu.open(player, wood == null ? WoodType.OAK : wood);
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cozyfurniture.admin")) {
            Msg.error(sender, "你沒有權限。");
            return;
        }
        if (args.length < 3) {
            Msg.error(sender, "用法：/furniture give <玩家> <種類> [木材] [數量]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.error(sender, "找不到玩家 " + args[1]);
            return;
        }
        FurnitureType type = FurnitureType.byId(args[2]);
        if (type == null) {
            Msg.error(sender, "未知的傢俱種類：" + args[2]);
            return;
        }
        WoodType wood = args.length >= 4 ? WoodType.parse(args[3]) : WoodType.OAK;
        if (wood == null) {
            Msg.error(sender, "未知的木材：" + args[3]);
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[4])));
            } catch (NumberFormatException e) {
                Msg.error(sender, "數量必須是數字。");
                return;
            }
        }
        target.getInventory().addItem(FurnitureItems.create(type, wood, amount)).values()
                .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        Msg.info(sender, "已給予 " + target.getName() + " " + amount + " 個" + wood.displayName() + type.displayName() + "。");
    }

    private void list(CommandSender sender) {
        StringBuilder types = new StringBuilder();
        for (FurnitureType t : FurnitureType.values()) {
            types.append(t.id()).append("（").append(t.displayName()).append("） ");
        }
        StringBuilder woods = new StringBuilder();
        for (WoodType w : WoodType.values()) {
            woods.append(w.id()).append("（").append(w.displayName()).append("） ");
        }
        Msg.info(sender, "傢俱：" + types.toString().trim());
        Msg.info(sender, "木材：" + woods.toString().trim());
    }

    private void remove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "只有玩家可以使用。");
            return;
        }
        if (!sender.hasPermission("cozyfurniture.admin")) {
            Msg.error(sender, "你沒有權限。");
            return;
        }
        RayTraceResult hit = player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(),
                8, FluidCollisionMode.NEVER, true, 0.0,
                e -> e instanceof Interaction && Keys.isFurniture(e));
        if (hit == null || !(hit.getHitEntity() instanceof Interaction it)) {
            Msg.error(sender, "準心沒有對準任何傢俱。");
            return;
        }
        Interaction root = plugin.furniture().findRoot(it);
        if (root == null) {
            it.remove();
            Msg.info(sender, "已清除殘留的碰撞箱。");
            return;
        }
        plugin.furniture().breakFurniture(root, false);
        Msg.info(sender, "已移除傢俱。");
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length <= 1) {
            out.addAll(List.of("menu", "give", "list", "remove", "reload"));
        } else if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            } else if (args.length == 3) {
                Arrays.stream(FurnitureType.values()).forEach(t -> out.add(t.id()));
            } else if (args.length == 4) {
                Arrays.stream(WoodType.values()).forEach(w -> out.add(w.id()));
            } else if (args.length == 5) {
                out.addAll(List.of("1", "8", "16"));
            }
        } else if (args[0].equalsIgnoreCase("menu") && args.length == 2) {
            Arrays.stream(WoodType.values()).forEach(w -> out.add(w.id()));
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
