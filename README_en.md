# Advanced Memory Card

**English** | [简体中文](README.md)

An Applied Energistics 2 (AE2) add-on that gives the AE2 Memory Card two major capabilities: **bulk region pasting** and **centralized P2P tunnel configuration**.

> **Current version: `1.2.1-1.21.1`** · Minecraft 1.21.1 / NeoForge 21.1.219+ / AE2 1.21.1 line
> See [CHANGELOG.md](CHANGELOG.md) for what changed.

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

| Option | Applies | Description |
| --- | --- | --- |
| Upgrades | Upgrade cards | Upgrade slots |
| Settings | Generic settings | Generic settings of `IConfigurableObject` |
| Priority | Priority | Priority |
| Config inventory | Config slots | Config slot contents (filter/marker items, etc.) |
| Patterns | Patterns | Patterns of a pattern provider |
| P2P type | P2P type | Tunnel type (decides whether a part is swapped) |
| P2P frequency | P2P frequency | Frequency the tunnel is bound to |
| Name | Custom name | Custom name |

- **Opening it**: Press `G` while holding the card in Copy Mode (rebindable in Controls). By default it also pops up **on every copy** (Shift + Right-click to copy a device's settings onto the card).
- **Auto-open toggle**: The button in the vertical toolbar to the left of the panel — highlighted = pop up after every copy, greyed out = never, also saved per card.
- **All unchecked by default**: A card that was never configured applies nothing, so you must opt in explicitly; the selection is written to the card and reused afterwards.
- **"Not applied" means "target keeps its value"**: Unticking an option keeps it out of the paste. For patterns / P2P type / name, AE2 treats a missing component as "clear the target", so the paste refills the target's own current value instead — which is equivalent to leaving it untouched.
- **Independent of mode switching**: The options live in the card's root tag (outside `mode_data`), so switching modes or copying again does not reset them.

### 🕸 Config Mode (P2P Tunnel Config)

Right-click any **P2P tunnel part** to open the configuration GUI and visually manage every P2P device in the current AE network:

- **Pairing bar (source → target)**: A permanent bar at the top. The left slot is the source device (the one that will move), the right slot is the target channel; the arrow lights up once both are set. The × on the source slot resets it to the tunnel you right-clicked.
- **Column-based P2P list**: Scans the network and lists type / channel / device in three levels. Four columns: status dot, name, channel, summary. The summary column adapts: a channel row reads "device count + channels left", an **ME tunnel row shows its channel usage with an `IN` / `OUT` prefix** — an input shows its own external network's used/max, an output shows `own used / (input remaining + own used)`, i.e. how much of the share this channel can still hand it has been taken, while inputs and non-ME devices show the IN/OUT role. Non-ME channels have no channel concept and show the device count only. Type rows use the part's translated name (e.g. "ME P2P Tunnel") instead of the raw resource path. Expand-collapse, search filtering, and pixel-accurate truncation with an ellipsis for long names.
- **Channel dropdown**: Click the target slot (or its caret) to open a frequency list sorted by channel number, limited to channels of the same type as the current source; the last row is "New channel…". **Click the target slot again to collapse the list** — clicking anywhere else collapses it too.
- **Ctrl-click for batch binding**: Hold `Ctrl` to select several devices (same type only — picking a device of a different type clears the whole selection). The source slot then shows "N selected" and the footer offers "Bind N devices to 0A1B, press Enter". The server binds them one by one: the first becomes that channel's input, the rest become outputs.
- **Ctrl-drag to box-select**: hold `Ctrl`, press on a row and drag up or down — every device the drag passes over **has its selected state flipped by the drag as a whole**: starting on an unselected device sweeps selection on, starting on a selected device sweeps it off, and the whole drag goes one way. **Starting on a channel row takes the whole channel** (inputs and outputs alike), even while that channel is collapsed. Dragging back restores the rows you left behind, passing over a type header does not select that type, and the same-type-only rule still applies. Leaving the list keeps the drag pinned to the first/last visible row, so rows scrolled out of view are never swept into the selection.
- **Batch mode**: the footer has a "Batch select" button left of "Bind". Click it to enter batch mode — the button turns into "Exit batch" and stays lit, and clicking it again leaves the mode. In batch mode you no longer hold `Ctrl`: a single click adds or removes a device and dragging still box-selects. **Clicking or dragging over a channel row now selects every device of that channel** — click it again while the whole channel is selected to remove it. In batch mode a channel row is expanded or collapsed with a **right-click** (the left button is reserved for selecting devices). `Ctrl`-clicking a channel row or a type header also enters the mode; type headers still only expand/collapse, and leaving the mode keeps the current selection.
- **Rename refreshes immediately**: renaming a device or a channel re-scans the network right away, so the tree and the inspector show the new name without waiting for the next poll.
- **Follows your selection**: picking a device expands the list to it and selects it; if the row is already visible the list does not scroll (so the 2 Hz sync doesn't yank it around). After a bind moves the device to another group, the list follows it there.
- **Target stays put**: switching the source device keeps the selected target channel — it is only cleared when the new device has a different P2P type (cross-type binding is meaningless).
- **One-key binding**: With a target set, the bottom **Bind** button and the Enter key both commit. The server returns a result code, so the status line reports either "Bound to 0A1B" or the exact reason it failed (no source / not bindable / network not ready). **The target slot survives the bind**, so you can pick the next device and bind it to the same channel right away.
- **Inspector**: The right panel leads with status chips (connection / role / channel usage), then a property table, a two-column action grid (the button count depends on the node type; a lone trailing button spans the full row) and a channel-usage progress bar. Usage is only shown for ME P2P — other types simply do not have that concept — and the bar turns amber once the channel runs low. Renaming is in-place — the rest of the properties stay visible while you type, and Esc cancels.
- **Rename / alias**: Set a custom name for an individual P2P, or an alias for a whole channel (its input side).
- **Make input**: when the selected device is an **output**, the 4th slot of the inspector reads *Make Input*. Clicking it hands the input role of that channel to this device and demotes the previous input to an output — a channel only ever has one input, so the server **demotes first, promotes second** (doing it the other way round evicts the old input from `P2PService`'s input map, leaving a tunnel whose `output` flag says "input" while the service has never heard of it, and neither end moves anything). The button is absent when the device is already an input or has no channel bound (0000): the first direction would leave the channel without its only input and kill it outright, the second has no channel to take over.
- **Assign new channel**: Assign a brand-new free frequency to the pairing source and make it the input side in one click.
- **Auto-configure I/O (ME P2P only)**: Decides whether an ME P2P should act as input or output based on whether its external network is connected to an ME Controller, and assigns a frequency automatically.
- **Highlight doubles as locate**: clicking "Highlight" first closes the GUI, then renders the P2P tunnel in the world (self / input / output in different colors), then turns your head toward the target — the old "Locate" button is merged into it. Head rotation still honours the client option `rotateHeadOnHighlight`. Chat output is location info only; the clickable teleport link is gone. Highlighting a frequency node lists **every input and output** on that channel, each tagged `IN` / `OUT`.
- **Optional Teleport button**: the 5th slot on P2P device rows is a standalone "Teleport" button executed server-side (cross-dimension supported). Once you land, the client highlights that device and turns your view toward it — that turn ignores `rotateHeadOnHighlight` (the option only governs highlights; a teleport is a move you asked for, and landing right on the device without turning leaves you disoriented). It only appears when the server option `allowTeleport = true`, and the server re-checks every teleport request. Frequency nodes have no position of their own, so they offer no teleport.
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
| `sendHighlightToChat` | `true` | Whether to post location info to chat when highlighting a P2P device |
| `allowTeleport` | `false` | Whether the GUI's "Teleport" button may move the player to a P2P device (cross-dimension supported). When false the button is hidden and teleport requests are rejected server-side |

### Client config (`advanced_memory_card-client.toml`)

Used to personalize the UI and rendering, organized by section:

- `[client.highlight]`: `rotateHeadOnHighlight` (auto-turn your head when highlighting), `p2pHighlightDurationSec` (1–60 seconds).
- `[client.treeLayout]`: `rowHeight` (default 14), `indentWidth` (per level, default 10), `channelWarningThreshold`, `nameMaxLength`.
- `[client.palette]`: semantic palette — `textPrimary` / `textSecondary` / `textTertiary`, `accent` / `accentText`, `rowSelected` / `rowHover`, `surface` / `surfaceHover`, `divider`, `statusOk` / `statusWarn` / `statusDanger`, `chipOkBg` / `chipAccentBg` / `chipWarnBg` / `chipDangerBg`, `overlayBg` / `overlayBorder` / `overlayShadow`.
  > Defaults are chosen for contrast: every text colour clears 4.5:1 on the list background `#ADB0C4`. The old `treeColors` / `panelColors` / `buttonColors` / `panelLayout` sections are replaced by `palette`.
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

> The panel can be closed at any time; the options stay on the card. Grey out the toggle in the left toolbar if you don't want it to pop up every time.

### Config Mode

1. Switch to Config Mode and **right-click** any P2P tunnel part to open the configuration GUI.
2. Browse / search the network's P2Ps in the left tree and select a node to view its details.
3. Use the detail panel buttons to highlight (which also turns your head toward the target), rename, bind a frequency, assign a new channel, auto-configure I/O, and — when `allowTeleport` is enabled — teleport to the device (you arrive facing it, with the device highlighted).
4. To fix a batch of devices at once, click **Batch select** in the footer (or `Ctrl`-click a channel row / type header to enter it directly), select by clicking, dragging or clicking a channel row, then press Enter to bind.

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

## 📝 Changelog

Full history in [CHANGELOG.md](CHANGELOG.md). Latest release:

### 1.2.1-1.21.1

- **Added** a *Make Input* button: an output device can take over the input role of its channel in one click, and the previous input becomes an output (the server always demotes first, promotes second, so the old input is never evicted from `P2PService`'s input map and left as an orphan that never moves anything). The status line reports success / no channel / already an input / failure.
- **Changed** *Teleport* moved to the 5th slot, so device rows now read *Highlight / Rename / Assign channel / Make Input / Teleport*; a lone trailing secondary button spans the full row, fixing the empty bottom-right slot on five-button device rows and three-button frequency rows.

### 1.2.0-1.21.1

- **Added** a full Config Mode UI rewrite: source → target pair bar, frequency dropdown (with *New frequency…*), a status line that reports every action, inspector status chips, inline rename, follow-selection, and a four-column list (status / name / frequency / summary).
- **Added** a teleport button (enabled by the server's `allowTeleport` option, cross-dimension capable; the client highlights the device and turns your view once you have actually arrived), channel highlights that list both inputs and outputs in chat, Ctrl-drag box selection, and a bottom-bar batch mode.
- **Changed** highlight now also locates (close GUI → light up the world → turn the view), inspector buttons swapped (slot 2 rename, slot 4 teleport), no more teleport links in chat, type names read from the part's item name, and `[client.palette]` replaces the four old colour sections.
- **Fixed** an intermittent sync-packet encoding crash (`ConcurrentModificationException` disconnecting the client).

### 1.1.0-1.21.1

- **Added** a paste-options panel for Copy Mode (upgrades / settings / priority / config inventory / patterns / P2P type / P2P frequency / name), opened with `G`, auto-opened after copying, stored per card.
- **Fixed** P2P bindings being lost after relogging, channels permanently showing "not connected" when two inputs shared a frequency, renames not being saved, and part pasting failing when the card is in the off hand.
- **Note** cards never configured now default to nothing selected.

## 📄 License

This project is open-sourced under the [GNU GPL-3.0](LICENSE.txt) license.
