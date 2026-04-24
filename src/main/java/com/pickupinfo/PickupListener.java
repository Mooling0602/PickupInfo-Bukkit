package com.pickupinfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.*;

public class PickupListener implements Listener {

    private final Plugin plugin;
    private final Map<UUID, List<Change>> pendingChanges = new HashMap<>();
    private final Map<UUID, Map<Integer, ItemStack>> inventorySnapshots = new HashMap<>();
    private ScheduledTask flushTask;

    public PickupListener(Plugin plugin) {
        this.plugin = plugin;
        flushTask = plugin.getServer().getGlobalRegionScheduler()
            .runAtFixedRate(plugin, t -> flushAll(), 1L, 20L);
    }

    public void cancel() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        ItemInfo info = getItemInfo(item);
        plugin.getLogger().info(player.getName() + " picked up " + item.getAmount() + " x " + info.name);
        addChange(player, info.key, info.translationKey, item.getAmount(), true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        ItemInfo info = getItemInfo(item);
        plugin.getLogger().info(event.getPlayer().getName() + " dropped " + item.getAmount() + " x " + info.name);
        addChange(event.getPlayer(), info.key, info.translationKey, item.getAmount(), false);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().toLowerCase(Locale.ROOT).split(" ")[0];
        if (!cmd.equals("/clear") && !cmd.equals("/minecraft:clear")) return;

        String[] parts = event.getMessage().split(" ");
        Player target = event.getPlayer();
        if (parts.length >= 2) {
            Player parsed = Bukkit.getPlayerExact(parts[1]);
            if (parsed != null && parsed.isOnline()) {
                target = parsed;
            }
        }
        plugin.getLogger().info(target.getName() + " executed " + cmd + " (source: " + event.getPlayer().getName() + ")");

        Player finalTarget = target;
        UUID uuid = finalTarget.getUniqueId();
        Map<Integer, ItemStack> snapshot = new HashMap<>();
        for (int i = 0; i < finalTarget.getInventory().getSize(); i++) {
            ItemStack item = finalTarget.getInventory().getItem(i);
            if (item != null) {
                snapshot.put(i, item.clone());
            }
        }
        inventorySnapshots.put(uuid, snapshot);

        plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> processClearDiff(finalTarget));
    }

    private void processClearDiff(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Integer, ItemStack> snapshot = inventorySnapshots.remove(uuid);
        if (snapshot == null) return;

        Map<String, Integer> diff = new LinkedHashMap<>();
        Map<String, String> diffKeys = new HashMap<>();

        for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) {
            int slot = entry.getKey();
            ItemStack oldItem = entry.getValue();
            ItemStack newItem = player.getInventory().getItem(slot);

            int oldAmount = oldItem.getAmount();
            int newAmount = (newItem != null && newItem.isSimilar(oldItem)) ? newItem.getAmount() : 0;

            if (oldAmount > newAmount) {
                ItemInfo info = getItemInfo(oldItem);
                diff.merge(info.key, oldAmount - newAmount, Integer::sum);
                diffKeys.putIfAbsent(info.key, info.translationKey);
            }
        }

        if (!diff.isEmpty()) {
            plugin.getLogger().info(player.getName() + " /clear diff: " + diff);
            List<Change> changes = new ArrayList<>();
            diff.forEach((key, count) -> changes.add(new Change(key, diffKeys.get(key), count, false)));
            flushChanges(player, changes);
        } else {
            plugin.getLogger().info(player.getName() + " /clear diff: empty (no items lost?)");
        }
    }

    private void flushAll() {
        if (pendingChanges.isEmpty()) return;

        Map<UUID, List<Change>> copy = new HashMap<>(pendingChanges);
        pendingChanges.clear();

        for (Map.Entry<UUID, List<Change>> entry : copy.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                flushChanges(player, mergeChanges(entry.getValue()));
            }
        }
    }

    private static List<Change> mergeChanges(List<Change> changes) {
        Map<String, int[]> merged = new LinkedHashMap<>();
        for (Change c : changes) {
            int[] data = merged.computeIfAbsent(c.key, k -> new int[2]);
            if (c.added) {
                data[0] = data[0] + c.count;
            } else {
                data[1] = data[1] + c.count;
            }
        }
        List<Change> result = new ArrayList<>();
        for (Map.Entry<String, int[]> e : merged.entrySet()) {
            String key = e.getKey();
            int[] data = e.getValue();
            String tk = null;
            for (Change c : changes) {
                if (c.key.equals(key)) {
                    tk = c.translationKey;
                    break;
                }
            }
            if (data[0] > 0) result.add(new Change(key, tk, data[0], true));
            if (data[1] > 0) result.add(new Change(key, tk, data[1], false));
        }
        return result;
    }

    private void addChange(Player player, String key, String translationKey, int count, boolean added) {
        UUID uuid = player.getUniqueId();
        pendingChanges.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new Change(key, translationKey, count, added));
    }

    private void flushChanges(Player player, List<Change> changes) {
        Component message = buildMessage(changes);
        if (message != null) {
            String plain = changes.stream().map(c -> (c.added ? "+" : "-") + c.count + " " + c.key).reduce((a, b) -> a + " | " + b).orElse("");
            plugin.getLogger().info("ActionBar -> " + player.getName() + ": " + plain);
            player.sendActionBar(message);
        }
    }

    private Component buildMessage(List<Change> changes) {
        if (changes.isEmpty()) return null;

        Component result = Component.empty();
        for (int i = 0; i < changes.size(); i++) {
            if (i > 0) {
                result = result.append(Component.text(" | "));
            }
            Change c = changes.get(i);
            Component nameComponent = c.translationKey != null
                ? Component.translatable(c.translationKey)
                : Component.text(c.key);
            String sign = c.added ? "+" : "-";
            NamedTextColor color = c.added ? NamedTextColor.GREEN : NamedTextColor.RED;
            result = result.append(
                Component.text(sign, color)
                    .append(Component.text(c.count, NamedTextColor.YELLOW))
                    .append(Component.text(" "))
                    .append(nameComponent.color(NamedTextColor.WHITE))
            );
        }
        return result;
    }

    private static ItemInfo getItemInfo(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String customName = item.getItemMeta().getDisplayName();
            return new ItemInfo(customName, customName, null);
        }
        String translationKey = item.getType().getTranslationKey();
        return new ItemInfo(translationKey, translationKey, translationKey);
    }

    private record ItemInfo(String name, String key, String translationKey) {}

    private record Change(String key, String translationKey, int count, boolean added) {}
}
