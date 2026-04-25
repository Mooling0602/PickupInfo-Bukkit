- English
- [Chinese (Simplified)](README_zh_CN.md)

# PickupInfo

A Bukkit plugin that displays item changes (pickups, drops, and command `/give` or `/clear`) in the player's action bar.

## Version compatibility
- Paper, Leaves, etc. (>=1.20.4)
- Folia, Luminol, etc. (>=1.20.4)

## Features

- Shows `+N Item` when picking up items or getting them via `/give`
- Shows `-N Item` when dropping items or losing them via `/clear`
- Batches multiple changes in 20 tick into a single action bar message
- Items' name will be displayed with player's client language automatically.

## Requirements

- Paper 1.20.4+ (or compatible server with Adventure API)
- JDK 17+

## Build

```sh
mvn clean package
```

The JAR is output to `target/PickupInfo-<version>.jar`.

## Usage

1. Place `PickupInfo-*.jar` in your server's `plugins/` folder
2. Restart or run `/reload`
3. The action bar shows the item changes automatically

### Message examples

| Action | Action bar display |
|--------|-------------------|
| Pick up 2 diamonds | `+2 Diamond` |
| Drop 1 cobblestone | `-1 Cobblestone` |
| Run `/clear` | `-15 Dirt \| -3 Oak Log` |
| Pick up multiple items at once | `+5 Wheat \| +3 Bone` |

## License

GPLv3

## Code generator

### AI models
- **DeepSeek-V4-Flash**

### CLI tools
- OpenCode
- Claude Code CLI
