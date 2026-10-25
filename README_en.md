# Advanced Memory Card

**English** | [简体中文](README.md)

An Applied Energistics 2 (AE2) add-on that gives the AE2 Memory Card two major capabilities: **bulk region pasting** and **centralized P2P tunnel configuration**.

## 🌟 Features

### 🔧 Two Working Modes

The mode and the per-card preferences are stored in the item's `CUSTOM_DATA` component (`mode_data` plus a few preference keys), and can be switched at any time:

| Mode | Description |
| --- | --- |
| **Copy Mode** | Copy one AE2 device's configuration into the card, select a region, then paste that configuration onto every AE2 block and part inside it at once |
| **Config Mode (P2P Tunnel Config)** | Right-click a P2P tunnel part to open a GUI for centrally managing and configuring P2P tunnels in the current AE network |

### 📋 Copy Mode

- **Bulk region paste**: Shift + Right-click to copy one AE2 device's configuration → right-click two opposite corners → right-click air to apply it in bulk.
- **Covers blocks and parts**: Every `AEBaseBlockEntity` (ME controllers, interfaces, storage buses, etc.) and every part on an `IPartHost` inside the region is processed.
- **Volume limit protection**: If the region exceeds the server config `maxCopyVolume` (default 2048 blocks), the paste is rejected and the selection is reset, preventing accidental oversized selections and lag.
- **Visual selection**: The start point, end point, targeted block, and out-of-range blocks each use their own color.
- **Real-time feedback**: The item tooltip and chat bar report marked coordinates, region size, and the number of blocks pasted.

#### Paste options (8 toggles, saved per card)

Which parts of the copied configuration get applied is controlled by 8 toggles:

| Option | AE2 component | Description |
| --- | --- | --- |
| Upgrades | `EXPORTED_UPGRADES` | Upgrade cards |
| Settings | `EXPORTED_SETTINGS` | Generic settings of `IConfigurableObject` |
| Priority | `EXPORTED_PRIORITY` | Priority |
| Config inventory | `EXPORTED_CONFIG_INV` | Config slot contents (filter/marker items, etc.) |
| Patterns | `EXPORTED_PATTERNS` | Patterns of a pattern provider |
| P2P type | `EXPORTED_P2P_TYPE` | Tunnel type (decides whether a part is swapped) |
| P2P frequency | `EXPORTED_P2P_FREQUENCY` | Tunnel frequency |
| Name | `EXPORTED_CUSTOM_NAME` | Custom name |

- **Opening it**: Press `G` while holding the card in Copy Mode (rebindable in Controls). By default it also pops up **on every copy** (Shift + Right-click to copy a device's settings onto the card).
- **Auto-open toggle**: The cog button attached to the left of the panel — `COG` = pop up after every copy, `COG_DISABLED` = never, also saved per card.
- **All unchecked by default**: A card that was never configured applies nothing, so you must opt in explicitly; the selection is written to the card and reused afterwards.
- **"Not applied" means "target keeps its value"**: Unticking an option keeps it out of the paste. For patterns / P2P type / name, AE2 treats a missing component as "clear the target", so the paste refills the target's own current value instead — which is equivalent to leaving it untouched.
- **Independent of mode switching**: The options live in the card's root tag (outside `mode_data`), so switching modes or copying again does not reset them.

### 🕸 Config Mode (P2P Tunnel Config)

Right-click any **P2P tunnel part** to open the configuration GUI and visually manage every P2P device in the current AE network:

- **P2P tree list**: Automatically scans the network and lists all P2P devices grouped by type / channel, with expand-collapse and search filtering.
- **Detail panel**: Shows the selected P2P's type, frequency, input/output role, and connection status.
- **Rename / alias**: Set a custom name for an individual P2P, or an alias for a whole channel (its input side).
- **Bind frequency**: Bind the selected P2P to a 4-digit hexadecimal frequency (e.g. `0A1B`). Every P2P type arbitrates its input/output role automatically: if the target frequency already has another input, the current tunnel becomes an output, otherwise it becomes the input.
- **Assign new channel**: Assign a brand-new free frequency to the selected non-ME P2P and set it as the input side in one click.
- **Auto-configure I/O (ME P2P only)**: Decides whether an ME P2P should act as input or output based on whether its external network is connected to an ME Controller, and assigns a frequency automatically.
- **Highlight & locate**: Renders P2P tunnels in the world (self / input / output in different colors), optionally turns your head toward the target, and posts location info plus a teleport link to chat.
- **Cross-dimension**: The encoding carries dimension info, so P2P devices can be located and highlighted across dimensions.

### ⌨️ Controls

| Key | When it works | Effect |
| --- | --- | --- |
| `V` | Holding the card | Switch between Copy Mode and Config Mode |
| `G` | Holding the card in Copy Mode | Open the paste options panel |
| Shift + Right-click (air) | Holding the card | Switch modes quickly |
| Shift + Right-click (AE block / part) | Copy Mode | Copy that device's configuration into the card (native AE2 behavior) and auto-open the options panel if enabled |
| Right-click (air) | Copy Mode | Bulk paste; reports a message if the selection is incomplete |
| Right-click (P2P part) | Config Mode | Open the P2P configuration GUI |

> Both `V` and `G` can be rebound under Options → Controls → Key Binds → Advanced Memory Card.

## 📦 Installation

### Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.219` or higher (`[21.1.219,)`) |
| Applied Energistics 2 (AE2) | A build matching MC 1.21.1 (developed against `19.2.17`; declared as `*` in `neoforge.mods.toml`) |

> No other runtime dependencies are declared. Jade, GuideME, and JEI are pulled in by the dev environment only; the release jar does not depend on them.

### Steps

1. Download the latest `advanced_memory_card-<version>.jar` from the releases page.
2. Put the `.jar` into your Minecraft `mods` folder.
3. Make sure NeoForge and AE2 are installed.
4. Launch the game and find `advanced_memory_card:advanced_memory_card` in the "Advanced Memory Card" creative tab (`advanced_memory_card:advanced_memory_card_tab`).

## 🛠 Configuration

Two config files are provided (in the `config/` directory):

### Server config (`advanced_memory_card-server.toml`, `[server]` section)

| Option | Default | Description |
| --- | --- | --- |
| `maxCopyVolume` | `2048` | Maximum region volume (block count, range 1 – `Integer.MAX_VALUE`) allowed in Copy Mode; larger selections are rejected |
| `sendHighlightToChat` | `true` | Whether to post location info and a teleport link to chat when highlighting a P2P device |

### Client config (`advanced_memory_card-client.toml`)

Used to personalize the UI and rendering, organized by section:

- `[client.highlight]`: `rotateHeadOnHighlight` (auto-turn your head when highlighting), `p2pHighlightDurationSec` (1–60 seconds).
- `[client.treeColors]`: `bg` / `selected` / `hover`.
- `[client.treeLayout]`: `indentWidth`, `rowHeight`, `iconTextSpacing`, `expandIconWidth`, `channelWarningThreshold`, `p2pNameMaxLength`.
- `[client.panelColors]`: `bg` / `title` / `label` / `value` / `placeholder` / `separator` / `buttonAreaBg`, plus `notActive` / `notConnected` / `connected` under `[client.panelColors.status]`.
- `[client.panelLayout]`: `buttonWidth`, `buttonHeight`, `buttonSpacing`, `lineHeight`, `paddingLeft`, `paddingTop`, `buttonCountMax`, etc.
- `[client.buttonColors]`: `bg` / `hover` / `click` / `text` (generic FlatButton buttons).
- `[client.copyColors]`: `startPos` / `endPos` / `targetedPos` / `overLimit`, plus `ready` / `first` / `second` under `[client.copyColors.selection]`.
- `[client.highlightColors]`: `self` / `input` / `output` (P2P highlight rendering).
- `[client.itemColors]`: `color` (base) and `tintColor` (tint).

> Colors are hexadecimal (ARGB for GUI classes, RGB for world rendering) and can be edited directly in the config files.

## 📖 Quick Usage Guide

### Copy Mode

1. Hold the card in Copy Mode and **Shift + Right-click** a source AE2 device to copy its configuration (the options panel pops up if enabled — tick what you want).
2. **Right-click** the first corner block to mark the start point (red).
3. **Right-click** the opposite corner to mark the end point (white); the selection is now ready to paste (green).
4. **Right-click air** to apply the configuration to every AE2 device inside the region.

> The panel can be closed at any time; the options stay on the card. Click the cog button on its left to switch it to `COG_DISABLED` if you don't want it to pop up every time.

### Config Mode

1. Switch to Config Mode and **right-click** any P2P tunnel part to open the configuration GUI.
2. Browse / search the network's P2Ps in the left tree and select a node to view its details.
3. Use the detail panel buttons to rename, bind a frequency, assign a new channel, auto-configure I/O, highlight & locate, and more.

## 🔧 Development & Build (for developers)

```bash
# Run the client (dev environment)
./gradlew runClient

# Run a dedicated server
./gradlew runServer

# Run the game test server
./gradlew gameTestServer

# Build the release jar
./gradlew build

# Generate recipes / item models / language files
./gradlew runData
```

- Built on **NeoForge 21.1.219** with **ModDevGradle 2.0.140**, Java 21 toolchain, Parchment mappings `2024.11.17`.
- Uses Mixin (`advanced_memory_card.mixins.json`, `compatibilityLevel: JAVA_21`) in two places:
  - `P2PTunnelPartMixin`: exposes AE2's `P2PTunnelPart#setOutput`;
  - `AEBasePartAccessor`: an `@Accessor` that writes `AEBasePart#customName` directly, used for P2P renaming without the side effects of `importSettings`.
- Resources are produced by the generators under `src/main/java/.../datagen` into `src/generated/resources/`: `ModRecipesProvider`, `ModItemModelsProvider`, `ModEnusLangProvider`, `ModZhcnLangProvider`.

## 📄 License

This project is open-sourced under the [GNU GPL-3.0](LICENSE.txt) license.
