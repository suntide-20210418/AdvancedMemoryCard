# Advanced Memory Card

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.7.10-blue)
![Forge Version](https://img.shields.io/badge/Forge-10.13.4.1614-orange)
![Mod Version](https://img.shields.io/badge/Version-1.2.0--1.7.10--GTNH-green)
![License](https://img.shields.io/badge/License-GPL--3.0-green)

**English** | [简体中文](README.md)

An Applied Energistics 2 (AE2) add-on that enhances the AE2 Memory Card with two major features: **bulk configuration copying** and **advanced P2P tunnel management**.

> **Current version: `1.2.0-1.7.10-GTNH`** · Minecraft 1.7.10 / Forge 10.13.4.1614
> See [CHANGELOG.md](CHANGELOG.md) for what changed.

## 🌟 Features

### 🔧 Two Working Modes

The mod stores its state as a "mode" inside the item's NBT, and you can switch between the two at any time:

| Mode | Description |
| --- | --- |
| **Copy Mode** | Select a region and paste a copied AE2 device configuration onto every AE2 block and part inside it at once |
| **Config Mode (P2P Channel Management)** | Right-click a P2P tunnel part to open a GUI for centrally managing and configuring P2P tunnels in the network |

### 📋 Copy Mode

- **Bulk region copy**: Copy a single AE2 device's configuration into the card first, then select two opposite corner blocks to define a region and right-click on air to apply the configuration in bulk.
- **Supports AE2 blocks and parts**: Every `AEBaseTile` in the region (ME controllers, interfaces, storage buses, etc.) as well as every part on an `IPartHost` is processed.
- **Volume limit protection**: If the region volume exceeds the server config `maxCopyVolume` (2048 blocks by default), the paste is rejected with a message, so a huge accidental selection cannot stall the game.
- **Visual selection box**: The selection is color-coded in real time (red = first point / white = second point / green = ready to paste), with separate colors for the targeted block and for blocks outside the limit.
- **Real-time feedback**: the chat bar reports marked coordinates, region size and how many blocks were actually pasted, while the item tooltip shows the current mode.
- **Options panel pops up after copying**: After each successful copy, a per-card switch decides whether the panel opens immediately, so you can adjust what this paste should apply.

#### Paste Options (7 items, saved per card)

| Option | Effect |
| --- | --- |
| Settings | Output face, fuzzy mode and other general settings |
| Priority | Priority of storage buses / interfaces |
| Config items | Contents of filter, marking and similar config slots |
| Patterns | The interface's pattern slots |
| P2P type | P2P tunnel type (if ticked and different from the target, the part is torn down and rebuilt) |
| P2P frequency | P2P frequency |
| Name | The block's custom name |

- **Unticked items keep the target's current value** instead of being cleared — the biggest difference from AE2's own memory card, and the reason this mod writes every section itself through `SettingsApplier` rather than reusing `uploadSettings`, which clears a section as soon as the key is missing.
- **A fresh card has nothing ticked**: press `G` to open the panel and pick the items on first use; with an empty mask, pasting only prints a hint and does not consume the selection.
- **"Patterns" has no effect on 1.7.10**: AE2 rv3's `downloadSettings` only writes `display` / `configManager` / `priority` / `config`, and patterns belong to the save path, so they never reach the memory card. The option stays on the panel, but ticking it will not change the target.
- **"Name" applies to blocks only**: the part side has no `display` section, so part names stay as they are.

### 🕸 Config Mode (P2P Channel Management)

Right-click any **P2P tunnel part** to open the configuration GUI — a 440×312 custom-drawn panel — and manage every P2P device in the current AE network visually:

- **Four-column P2P tree**: status dot / name / frequency / summary, grouped by type and channel, with expand-collapse and search filtering (a search matches the full frequency hex).
- **Pairing bar**: "source → target + frequency dropdown" at the top; pick a source, a target and a frequency to bind in one go.
- **Inspector**: the right-hand side shows the selected device's frequency, role, connection state and channel alias, plus buttons to rename, switch to input, teleport and highlight.
- **Batch binding**: Ctrl-click a non-device row to enter batch mode, box-select several devices and bind them all to one frequency at once; the status line reports "batch succeeded" or "partially succeeded".
- **64-bit frequencies**: a frequency is a 64-bit integer, shown and passed around as **16 hex digits** (e.g. `A3F019B7C2D40815`). Pick the target frequency from the dropdown in the pairing bar; without an alias the tree shows only `…` plus the last 6 digits (e.g. `…A3F019`), while the full value lives in the inspector and the tooltip. `0000…0000` (unbound) is not a valid binding target and is rejected.
- **Channel grouping**: the channel key is "frequency + P2P type", so different types sharing one frequency form separate groups and do not interfere.
- **Inline rename**: edit the name directly in the inspector and press `Enter` to commit or `Esc` to cancel; an alias can also be set for a whole channel.
- **Switch to input**: promote the selected P2P to the input side of its frequency; the previous input is demoted to output automatically.
- **Assign a new channel / auto-configure I/O**: give the selected device a free frequency and make it the input; for ME P2P, the role can also be decided automatically from whether the external network is connected to an ME Controller.
- **Highlight & locate**: render the P2P channel in the world (self / input / output in different colors), optionally turn your head toward the target, and send position and teleport links to chat. With `server.allowTeleport` off, the teleport button is hidden and the server refuses the request.
- **Status line**: every action is reported at the bottom (bound / no target / invalid frequency / no network / switched to input, and so on).
- **Cross-dimension location**: the encoded position carries dimension information, so P2P devices are found and highlighted correctly across dimensions.

### ⌨️ Key Bindings

| Key | Action |
| --- | --- |
| `V` | Switch between Copy Mode and Config Mode while holding the card |
| `G` | Open the paste options panel in Copy Mode |
| `Shift + Right-click` | In Copy Mode: copy the target device's configuration into the card (native AE2 behaviour); otherwise: quickly switch modes |

> Both `V` and `G` can be rebound under "Options → Controls → Key Binds → Advanced Memory Card".

## 📦 Installation

### Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614 (GTNH Forge) |
| Applied Energistics 2 | GTNH `Applied-Energistics-2-Unofficial` rv3 (built against `rv3-beta-1000-GTNH`) |

> The mod references `appeng.*` P2P and part implementations directly, so AE2 is a **required** dependency and the mod will not load without it. NEI (NotEnoughItems) is only a runtime dependency of the development environment; you do not need it to play.

### Steps

1. Download `advanced_memory_card-1.2.0-1.7.10-GTNH.jar` from the releases page.
2. Place the jar into Minecraft's `mods` folder.
3. Make sure the dependencies above are installed, matching your modpack's versions.
4. Launch the game and find the item under the "Advanced Memory Card" creative tab.

## 🛠 Configuration

Config files live in `config/advanced_memory_card/`:

### Server config (`server.cfg`, `[server]` section)

| Option | Default | Description |
| --- | --- | --- |
| `maxCopyVolume` | `2048` | Maximum region volume (block count) allowed in Copy Mode; larger selections are rejected |
| `sendHighlightToChat` | `true` | Whether to send position info and a teleport link to chat when highlighting a P2P device |
| `allowTeleport` | `true` | Whether the config GUI may teleport you to a P2P device; when off, the button is hidden and the server refuses teleports |

### Client config (`client.cfg`)

Used to personalize the UI and rendering appearance. The groups are:

| Group | Contents |
| --- | --- |
| `client.highlight` | Auto-turn head on highlight, highlight duration (1–60 seconds) |
| `client.palette` | Semantic palette of the new Config Mode UI: primary / secondary / tertiary text, accent, row selected and hover, panel surface, divider, status colors, chip backgrounds, overlay colors |
| `client.treeColors` / `client.treeLayout` | P2P tree background / selected / hover colors; indent, row height, icon spacing, expand-icon width, channel warning threshold, name length limit |
| `client.panelColors` / `client.panelLayout` | Inspector background and text colors, status colors, layout dimensions |
| `client.buttonColors` | Generic button default / hover / pressed / text colors |
| `client.copyColors` | Copy Mode selection box: start, end, targeted and out-of-range points plus the three selection states |
| `client.highlightColors` | P2P highlight colors for self / input / output |
| `client.itemColors` | Item base color and tint color |

> The new Config Mode UI takes its colors from `client.palette`; the older groups `client.treeColors` / `client.panelColors` / `client.buttonColors` are still written for compatibility with existing configs.
> Colors are hexadecimal values (ARGB for GUI classes, RGB for rendering classes); edit the files directly and restart the game for changes to take effect.

## 📖 Quick Usage Guide

### Copy Mode

1. Hold the card in Copy Mode and **Shift + Right-click** a source AE2 device to copy its configuration.
2. (Optional) Press `G`, or let the panel pop up, and tick the items to apply this time.
3. **Right-click** the first corner block to mark the start point (red).
4. **Right-click** the opposite corner block to mark the end point (white); the selection is now ready to paste (green).
5. **Right-click** on air to apply the configuration to every AE2 device in the region.

### Config Mode

1. Switch to Config Mode and **right-click** any P2P tunnel part to open the GUI.
2. Browse or search the network's P2P devices in the four-column tree on the left, and select a node to inspect it on the right.
3. Pick a source and target in the pairing bar (Ctrl to select several for batch binding), or rename, switch to input, teleport or highlight from the inspector.

## 🔧 Development & Build (for developers)

```bash
# Run the client (dev environment)
./gradlew runClient

# Run a dedicated server
./gradlew runServer

# Build the release jar
./gradlew build

# Format check / auto-fix
./gradlew spotlessCheck
./gradlew spotlessApply
```

- The build uses the **GTNH Gradle convention plugin**: `build.gradle.kts` is just `plugins { id("com.gtnewhorizons.gtnhconvention") }`, the plugin version is declared in `settings.gradle.kts` (`gtnhsettingsconvention` 2.0.20), and the Gradle wrapper is pinned to 9.3.1.
- **JDK 25** is required to run Gradle; `enableModernJavaSyntax = jabel` lets the sources use modern syntax while still compiling to Java 8 bytecode.
- AE2 is consumed as GTNH's `com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-1000-GTNH:dev`; NEI is only a `runtimeOnlyNonPublishable` entry for the dev environment.
- AE2 package-private members (such as `PartP2PTunnel#setOutput`) are reached through `src/main/resources/META-INF/advanced_memory_card_at.cfg`, and the reflection entry points are centralized in `P2PManager`.
- This branch has neither Mixin nor datagen: no `mixins.json`, no `runData`, no `src/generated/resources`. Models, textures and language files are maintained directly under `src/main/resources`, and metadata uses `mcmod.info`.

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md).

## 📄 License

This project is open-sourced under the [GNU GPL-3.0](LICENSE.txt) license.
