# PickupInfo-Bukkit

Bukkit plugin — displays item pickup and loss changes in the player's action bar.

Shows a message in the action bar when a player picks up an item, drops an item, or loses items via commands (e.g. `/clear`).

> This message is written by human(user).
>
> If you read this file, you should know the following informations are out of date.
>
> You can ignore this file, or if you want to continue to reuse it, you should now update with actual informations from the repository.
>
> Ask user for everything you don't cleared with.

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

## Release process

1. Update version in `pom.xml` if needed
2. Commit with message `release: v<version>` (e.g. `release: v1.0.0`)
3. Create annotated tag: `git tag -a <version> -m "commit <hash>"`  (tag has **no** `v` prefix; e.g. `1.0.0`)
4. Push the tag: `git push origin <version>`
5. GitHub Actions builds and creates a Release automatically
