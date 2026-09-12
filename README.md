# PurrMinions

Paper 1.21+ — Kotlin — automated minion workers for resource gathering.

Place minions that collect resources while you're offline. Fuel them, manage their storage, and let them work for you.

## Features

- **Automated Workers**: Place minions to gather resources automatically
- **Fuel System**: Keep minions running with coal or other fuel
- **Offline Production**: Minions work even when you're offline
- **Storage Management**: Each minion has internal storage for collected items
- **Player Limits**: Configurable limits per player and per chunk
- **Multiple Types**: Mining, Farming, Fishing, and more minion types

## Commands

- `/minion list` - View your active minions
- `/minion upgrade` - Upgrade minion tier *(not yet implemented)*

**Note**: Minion placement currently requires placing custom items (via `/purritems give`). Commands for direct placement coming soon.

## Minion Types

| Type | Collects | Fuel Consumption |
|------|----------|------------------|
| Mining | Ores, stone | 1 coal/hour |
| Farming | Crops | 1 coal/hour |
| Fishing | Fish | 1 coal/hour |

## Configuration

Configuration support is planned for future releases. Currently, limits are hardcoded in the plugin.

## Building

```bash
gradle shadowJar
# → build/libs/purrminions-1.0.0.jar
```

Requires `PurrCore` for database (HikariCP sqlite/mysql/pg).

## Testing

```bash
gradle test
# 80 unit tests covering domain, repository, manager, ticker
```

Coverage: 75% (9/12 files) - all critical business logic tested.
