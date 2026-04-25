- [English](README.md)
- 中文（简体）

# PickupInfo

一个 Bukkit 插件，在玩家动作栏（ActionBar）显示物品变化信息（拾取、丢弃、指令给予、指令清除）。

## 适用版本
- Paper, Leaves, etc. (>=1.20.4)
- Folia, Luminol, etc. (>=1.20.4)

## 功能

- 拾取或通过 `/give` 获得物品时显示 `+N 物品名`
- 丢弃或通过 `/clear` 清除物品时显示 `-N 物品名`
- 20 tick 内的多次变化合并为一条动作栏消息
- 物品名称将自动以玩家的客户端语言显示

## 环境要求

- Paper 1.20.4+（或兼容 Adventure API 的服务端）
- JDK 17+

## 构建

```sh
mvn clean package
```

编译产物为 `target/PickupInfo-<version>.jar`。

## 使用方法

1. 将 `PickupInfo-*.jar` 放入服务端 `plugins/` 目录
2. 重启服务器或执行 `/reload`
3. 动作栏会自动显示物品变化

### 消息示例

| 操作 | 动作栏显示 |
|------|-----------|
| 拾取 2 个钻石 | `+2 钻石` |
| 丢弃 1 个圆石 | `-1 圆石` |
| 执行 `/clear` | `-15 泥土 \| -3 橡木原木` |
| 同时拾取多种物品 | `+5 小麦 \| +3 骨头` |

## 许可证

GPLv3

## 代码生成说明

### AI 模型
- **DeepSeek-V4-Flash**

### CLI 工具
- OpenCode
- Claude Code CLI 
