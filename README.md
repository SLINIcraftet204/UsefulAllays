# UsefulAllays

**UsefulAllays** is a Paper plugin idea focused on making Minecraft Allays more useful and more pet-like without turning them into an overpowered automation system.

The goal is simple:

> Make Allays more useful and more like pets.

UsefulAllays is designed around claimable Allays, owner protection, follow/teleport behavior and configurable helper mechanics. The project is intentionally structured so server owners can adjust most gameplay values through configuration instead of needing code changes.

## What the plugin is meant to do

UsefulAllays turns Allays into manageable server pets/helper mobs:

- Players can claim free Allays with a configurable item.
- Claimed Allays store their owner directly on the entity.
- Claimed Allays can follow their owner and teleport after warps, homes or world changes.
- Allays can be protected from interaction or damage by other players.
- Levels, pickup radius, filter slots and other values are config-driven.
- The codebase is prepared for GUI-based settings, upgrades, trust players and item filters.

## What this project is not

UsefulAllays is not meant to be a fully automatic quarry, item duper or chunk-loader replacement. The balancing philosophy is that Allays should be helpful companions, not a way to bypass normal Survival progression.

## Compatibility target

- Minecraft/Paper: `1.21.11`
- Java: `21`
- Platform focus: Paper first

Other Bukkit-like server platforms may work later, but Paper is the intended target.

## Building

```bash
./gradlew build
```

The compiled plugin jar will be generated in:

```text
build/libs/
```

If you do not use the Gradle Wrapper yet, import the project into IntelliJ IDEA or run it with a locally installed Gradle version.

## Installation

1. Build the plugin.
2. Copy the jar from `build/libs/` into your server's `plugins/` directory.
3. Start the Paper server once.
4. Edit `plugins/UsefulAllays/config.yml` and `messages.yml` if needed.
5. Restart or run `/usefulallays reload`.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/usefulallays` | Shows basic plugin information | `usefulallays.command` |
| `/usefulallays reload` | Reloads config and messages | `usefulallays.admin.reload` |
| `/usefulallays list` | Shows loaded claimed Allays for the player | `usefulallays.command` |

Alias:

```text
/ua
```

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `usefulallays.command` | Allows basic command usage | true |
| `usefulallays.claim` | Allows claiming free Allays | true |
| `usefulallays.gui` | Allows opening the Allay GUI | true |
| `usefulallays.upgrade` | Allows using upgrade actions | true |
| `usefulallays.admin.reload` | Allows reloading the plugin | op |
| `usefulallays.admin.bypass` | Bypasses ownership restrictions | op |
| `usefulallays.limit.1` | Example permission for external limit handling | false |
| `usefulallays.limit.3` | Example permission for external limit handling | false |
| `usefulallays.limit.5` | Example permission for external limit handling | false |
| `usefulallays.limit.unlimited` | Example permission for unlimited Allays | op |

## Configuration philosophy

UsefulAllays should be configurable without making the default setup painful. The default configuration is intended to be safe for Survival servers and can be adjusted later as the plugin grows.

Main configuration areas:

- claiming behavior
- display names
- follow and teleport behavior
- protection rules
- collection behavior
- level values
- upgrade costs
- world restrictions

## Development structure

```text
src/main/java/at/slini204/usefulallays/
├── UsefulAllaysPlugin.java
├── command/
├── config/
├── data/
├── gui/
├── listener/
├── model/
├── service/
└── util/
```

The current structure separates config loading, persistent entity data, listeners, GUI logic and gameplay services so features can be expanded without turning the main plugin class into a mess.

## Planned feature areas

These are intentionally listed as feature areas instead of hard version promises:

- better GUI filter management
- trusted player access
- stay/home mode
- configurable upgrade paths
- optional economy integration
- optional item delivery mode
- better admin tooling
- optional database-backed Allay index


## License / Rights

This project is currently published as **All Rights Reserved / No License**.

The source code may be publicly visible for transparency and issue tracking, but the project is not licensed as open source. Redistribution, reuploads, modified versions, or commercial use are not permitted without explicit permission from the author.