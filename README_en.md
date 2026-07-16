# Advanced Memory Card

**English** | [简体中文](README.md)

An Applied Energistics 2 (AE2) add-on mod that supercharges the AE2 Memory Card with two major features: **bulk configuration copying** and **advanced P2P tunnel management**.

## 🌟 Features

### 🔧 Two Working Modes

The mod stores its state as a "mode" inside the item's NBT, and you can switch between them at any time:

| Mode | Description |
| --- | --- |
| **Copy Mode** | Select a region and paste a copied AE2 device configuration onto every AE2 block and part inside it at once |
| **Config Mode (P2P Tunnel Config)** | Right-click a P2P tunnel part to open a GUI for centrally managing and configuring P2P tunnels within the network |

### 📋 Copy Mode

- **Bulk region copy**: Copy a single AE2 device's configuration first, then select two opposite corner blocks to define a region, and right-click on air to apply the config in bulk.
- **Supports AE2 blocks and parts**: Every `AEBaseBlockEntity` (ME controllers, interfaces, storage buses, etc.) and every part on an `IPartHost` within the region is processed.
- **Volume limit protection**: If the region volume exceeds the server config `maxCopyVolume` (default 2048 blocks), the paste is rejected with a message to prevent accidental oversized selections and lag.
- **Visual selection box**: The selection is color-coded in real time (red = pick first point / white = pick second point / green = ready to paste), with distinct colors for the targeted block and out-of-range blocks.
- **Real-time feedback**: The item tooltip and chat bar display marked coordinates, region size, and the number of blocks pasted.

### 🕸 Config Mode (P2P Tunnel Config)

Right-click any **P2P tunnel part** to open the configuration GUI and visually manage all P2P devices in the current AE network:

- **P2P tree list**: Automatically scans the network and displays all P2P devices grouped by type / channel, with expand/collapse and search filtering.
- **Detail panel**: Selecting a node shows its type, frequency, input/output role, connection status, and more.
- **Rename / alias**: Set a custom name for an individual P2P device, or set an alias for an entire channel (the channel's input side) for easy identification.
- **Bind frequency**: Bind the selected P2P to a specific 4-digit hexadecimal frequency (e.g. `0A1B`). Non-ME P2Ps automatically and intelligently determine their input/output role.
- **Assign new channel**: Assign a brand-new free frequency to the selected non-ME P2P and set it as the input side with one click.
- **Auto-configure input/output (ME P2P only)**: Automatically decides whether an ME P2P should act as input or output based on whether its external network is connected to an ME Controller, and assigns a frequency automatically.
- **Highlight & locate**: Highlights P2P tunnels in the world (self / input / output rendered in different colors), optionally auto-turns your head toward the target, and sends location info and a teleport link to the chat bar.
- **Cross-dimension connections**: Fixes the vanilla issue where the client only sent coordinates, making cross-dimension P2Ps impossible to locate. The encoding now carries dimension info, allowing P2P devices to be correctly located and highlighted across dimensions.

### ⌨️ Mode Switching

- **`V` key** (default, rebindable in the game's Controls settings) — press while holding the Advanced Memory Card to switch modes.
- **Shift + Right-click** — quickly switch between the two modes (in Copy Mode, Shift + Right-click performs the native AE2 copy action instead of switching).

## 📦 Installation

### Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.10 or higher (loader range `[47,)`) |
| Applied Energistics 2 (AE2) | `15.4.10` or higher (`[15.4.10,)`) |
| mixinextras | `1.0.0+` (optional, for Mixin enhancements) |

> The mod also depends on Jade, GuideME, and JEI at runtime (common AE2-ecosystem add-ons; installing them is recommended for the best experience).

### Steps

1. Download the latest `advanced_memory_card-<version>.jar` from the releases page.
2. Place the `.jar` file into your Minecraft `mods` folder.
3. Make sure the required mods above are installed.
4. Launch the game and find the item under the "Advanced Memory Card" creative tab.

## 🛠 Configuration

The mod provides both client and server config files (located in the `config/` directory):

### Server config (`advanced_memory_card-server.toml`)

| Option | Default | Description |
| --- | --- | --- |
| `maxCopyVolume` | `2048` | Maximum region volume (block count) allowed in Copy Mode; larger selections are rejected |
| `sendHighlightToChat` | `true` | Whether to send location info and a teleport link to chat when highlighting a P2P device |

### Client config (`advanced_memory_card-client.toml`)

Mainly used to personalize UI and rendering appearance. You can adjust:

- **Highlight behavior**: `rotateHeadOnHighlight` (auto-turn head when highlighting), `p2pHighlightDurationSec` (highlight duration, 1–60 seconds).
- **P2P tree widget**: background / selected / hover colors, indent, row height, icon spacing, channel warning threshold, max name length, etc.
- **Detail panel**: background, title, label, value, and status (not active / not connected / connected) colors and layout dimensions.
- **Generic buttons (FlatButton)**: default / hover / pressed / text colors.
- **Copy Mode selection box**: colors for start point, end point, targeted point, out-of-range, and the three selection states.
- **P2P highlight rendering**: self / input / output colors.
- **Item color**: item base color and tint color.

> All colors are hexadecimal values (ARGB for GUI classes, RGB for rendering classes) and can be edited directly in the config files.

## 📖 Quick Usage Guide

### Copy Mode

1. Hold the Advanced Memory Card in Copy Mode and **Shift + Right-click** a source AE2 device to copy its configuration.
2. **Right-click** the first corner block to mark the start point (red).
3. **Right-click** the opposite corner block to mark the end point (white); it now enters the ready-to-paste state (green).
4. **Right-click** on air to apply the config in bulk to all AE2 devices in the region.

### Config Mode

1. Switch to Config Mode and **right-click** any P2P tunnel part to open the configuration GUI.
2. Browse / search the network's P2Ps in the left tree, and select a node to view its details on the right.
3. Use the detail panel buttons to rename, bind a frequency, assign a new channel, auto-configure input/output, highlight & locate, and more.

## 🔧 Development & Build (for developers)

```bash
# Run the client (dev environment)
./gradlew runClient

# Build the release jar
./gradlew build

# Generate data packs / language files
./gradlew runData
```

- Built on Forge Gradle 6.x with a Java 17 toolchain.
- Uses Mixin (`advanced_memory_card.mixins.json`) to extend AE2's `P2PTunnelPart`.
- Resource files are produced by the data generators under `src/main/java/.../datagen` into `src/generated/resources/`.

## 📄 License

This project is open-sourced under the [GNU GPL-3.0](LICENSE.txt) license.
