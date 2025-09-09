package elevate.dropPlayerHead;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

public final class DropPlayerHead extends JavaPlugin implements Listener {

    private static final String PERM_SET = "drophead.set";
    private NamespacedKey fromPluginKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fromPluginKey = new NamespacedKey(this, "from_plugin");
        double chance = getChance();

        if (chance < 0 || chance > 100) {
            getLogger().warning("Config 'drop_chance' out of range, resetting to 10");
            getConfig().set("drop_chance", 10.0);
            saveConfig();
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DropHead enabled. Chance: " + getChance() + "%");
    }

    private double getChance() {
        return getConfig().getDouble("drop_chance", 10.0D);
    }

    private void setChance(double pct) {
        FileConfiguration cfg = getConfig();
        cfg.set("drop_chance", pct);
        saveConfig();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        double roll = ThreadLocalRandom.current().nextDouble(100.0);

        if (roll >= getChance()) return;

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(deceased);
            meta.setDisplayName(ChatColor.GOLD + deceased.getName() + "'s Head");
            meta.getPersistentDataContainer().set(fromPluginKey, PersistentDataType.BYTE, (byte)1);
            head.setItemMeta(meta);
        }

        //Adds to natural drops?
        event.getDrops().add(head);
    }

    //Command: /drophead
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("drophead")) return false;

        if (args.length == 0 || args[0].equalsIgnoreCase("get")) {
            sender.sendMessage(ChatColor.YELLOW + "Current drop chance: " + getChance() + "%");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp() && !sender.hasPermission(PERM_SET)) {
                sender.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }

            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "DropHead config reloaded. Chance: " + getChance() + "%");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.isOp() && !sender.hasPermission(PERM_SET)) {
                sender.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /drophead set <0-100>");
                return true;
            }
            try {
                double pct = Double.parseDouble(args[1]);
                if (pct < 0 || pct > 100) {
                    sender.sendMessage(ChatColor.RED + "Percent must be between 0 and 100.");
                    return true;
                }
                setChance(pct);
                sender.sendMessage(ChatColor.GREEN + "Drop chance set to " + pct + "%");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Not a number: " + args[1]);
            }
            return true;
        }

        //help
        sender.sendMessage(ChatColor.AQUA + "DropHead commands:");
        sender.sendMessage(ChatColor.GRAY + "  /drophead get");
        sender.sendMessage(ChatColor.GRAY + "  /drophead set <0-100>");
        sender.sendMessage(ChatColor.GRAY + "  /drophead reload");
        return true;
    }

    //tab completer
    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("drophead")) return null;
        if (args.length == 1) return java.util.Arrays.asList("get", "set", "reload");
        if (args.length == 2 && "set".equalsIgnoreCase(args[0])) return java.util.Arrays.asList("0","5","10","25","50","75","100");
        return java.util.Collections.emptyList();
    }
}