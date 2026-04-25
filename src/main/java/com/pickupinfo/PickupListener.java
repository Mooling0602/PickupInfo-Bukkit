package com.pickupinfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.*;

import org.bukkit.event.server.ServerCommandEvent;

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
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().trim();
        String lower = command.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("give ") && !lower.startsWith("minecraft:give ")
            && !lower.startsWith("clear") && !lower.startsWith("minecraft:clear")) {
            return;
        }

        String[] parts = command.split(" ");
        String cmdBase = parts[0].toLowerCase(Locale.ROOT).replace("minecraft:", "");

        Player target = null;
        if (cmdBase.equals("give") && parts.length >= 3) {
            target = Bukkit.getPlayerExact(parts[1]);
        } else if (cmdBase.equals("clear") && parts.length >= 2) {
            target = Bukkit.getPlayerExact(parts[1]);
        }

        if (target == null || !target.isOnline()) return;

        Player affected = target;
        plugin.getLogger().info("Console affecting " + affected.getName() + ": " + command);

        // Snapshot inventory before command executes
        UUID uuid = affected.getUniqueId();
        Map<Integer, ItemStack> snapshot = new HashMap<>();
        for (int i = 0; i < target.getInventory().getSize(); i++) {
            ItemStack item = target.getInventory().getItem(i);
            if (item != null) {
                snapshot.put(i, item.clone());
            }
        }
        inventorySnapshots.put(uuid, snapshot);

        // Diff next tick after command executes
        plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> processInventoryDiff(affected));
    }

    private void processInventoryDiff(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Integer, ItemStack> snapshot = inventorySnapshots.remove(uuid);
        if (snapshot == null) return;

        List<Change> changes = new ArrayList<>();
        Set<Integer> checkedSlots = new HashSet<>();

        // Check slots from snapshot (detect both additions and removals)
        for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) {
            int slot = entry.getKey();
            checkedSlots.add(slot);
            ItemStack oldItem = entry.getValue();
            ItemStack newItem = player.getInventory().getItem(slot);

            int oldAmount = oldItem.getAmount();
            int newAmount = (newItem != null && newItem.isSimilar(oldItem)) ? newItem.getAmount() : 0;

            if (newAmount > oldAmount) {
                ItemInfo info = getItemInfo(oldItem);
                changes.add(new Change(info.key, info.translationKey, newAmount - oldAmount, true));
            } else if (oldAmount > newAmount) {
                ItemInfo info = getItemInfo(oldItem);
                changes.add(new Change(info.key, info.translationKey, oldAmount - newAmount, false));
            }
        }

        // Check slots that were empty but now have items (new items)
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (checkedSlots.contains(i)) continue;
            ItemStack newItem = player.getInventory().getItem(i);
            if (newItem != null) {
                ItemInfo info = getItemInfo(newItem);
                changes.add(new Change(info.key, info.translationKey, newItem.getAmount(), true));
            }
        }

        if (!changes.isEmpty()) {
            plugin.getLogger().info(player.getName() + " console diff: " + changes.stream()
                .map(c -> (c.added ? "+" : "-") + c.count + " " + c.key)
                .reduce((a, b) -> a + " | " + b).orElse(""));
            flushChanges(player, mergeChanges(changes));
        } else {
            plugin.getLogger().info(player.getName() + " console diff: empty (command likely failed)");
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

    @SuppressWarnings("removal") // getTranslationKey() — no replacement in 1.20.4 API
    private static ItemInfo getItemInfo(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            Component displayName = item.getItemMeta().displayName();
            String plain = PlainTextComponentSerializer.plainText().serialize(displayName);
            return new ItemInfo(plain, plain, null);
        }
        String translationKey = item.getType().getTranslationKey();
        return new ItemInfo(translationKey, translationKey, translationKey);
    }

    private record ItemInfo(String name, String key, String translationKey) {}

    private record Change(String key, String translationKey, int count, boolean added) {}
}
