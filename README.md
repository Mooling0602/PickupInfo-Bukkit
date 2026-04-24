- English
- [Chinese (Simplified)](README_zh_CN.md)

# PickupInfo

A Bukkit plugin that displays item changes (pickups, drops, and command clears) in the player's action bar.

## Features

- Shows **green** `+N Item` when picking up items
- Shows **red** `-N Item` when dropping items or losing them via `/clear`
- Batches multiple changes in the same tick into a single action bar message
- No configuration files — drop in and play

## Requirements

- Paper 1.20.4+ (or compatible server with Adventure API)
- JDK 17+

## Build

```sh
mvn clean package
```

The JAR is output to `target/PickupInfo-1.0.0.jar`.

## Usage

1. Place `PickupInfo-1.0.0.jar` in your server's `plugins/` folder
2. Restart or run `/reload`
3. Pick up or drop items — the action bar shows the changes automatically

### Message examples

| Action | Action bar display |
|--------|-------------------|
| Pick up 2 diamonds | `+2 Diamond` (green `+2`) |
| Drop 1 cobblestone | `-1 Cobblestone` (red `-1`) |
| Run `/clear` | `-15 Dirt \| -3 Oak Log` (all red) |
| Pick up multiple items at once | `+5 Wheat \| +3 Bone` |

## License

MIT
