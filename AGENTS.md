# PickupInfo-Bukkit

Bukkit plugin — displays item pickup and loss changes in the player's action bar.

Shows a message in the action bar when a player picks up an item, drops an item, or loses items via commands (e.g. `/clear`).

## Message format

- Pattern: `+2 Diamond` / `-2 Diamond`
- `+` and count are green, `-` and count are red, item name is default white — use Adventure Component API (not legacy § color codes)
- Multiple items separated by ` | ` with reset formatting, e.g.: `Component.text("+2", GREEN).append(Component.text(" Diamond"))`
- No external config files

## Build

```sh
mvn clean package
```

Produces `target/PickupInfo-1.0.0.jar`. Requires JDK 17+.

## Architecture

| File | Purpose |
|------|---------|
| `PickupInfoPlugin.java` | Main class, registers listener in `onEnable` |
| `PickupListener.java` | Three event handlers with per-player 1-tick accumulation |

### Events

| Event | Behavior |
|-------|----------|
| `EntityPickupItemEvent` | Adds `+N Item` to pending queue |
| `PlayerDropItemEvent` | Adds `-N Item` to pending queue |
| `PlayerCommandPreprocessEvent` | Detects `/clear` (or `/minecraft:clear`), snapshots target inventory, diffs next tick |

### Accumulation

Per-player `Map<UUID, List<Change>>` with a 1-tick delayed flush. Events in the same tick are batched into one action bar message separated by ` | `. For `/clear`, the diff is computed in a scheduled task on the next tick after command execution.

Item names: uses `ItemMeta#getDisplayName()` if a custom name exists, otherwise formats the Bukkit Material enum name (e.g. `DIAMOND_SWORD` → `Diamond Sword`).

## Dependencies

- `io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT` (provided)
