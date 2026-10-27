# Advanced Memory Card

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.12.2-blue)
![Forge Version](https://img.shields.io/badge/Forge-14.23.5.2860-orange)
![Mod Version](https://img.shields.io/badge/Version-1.1.0--1.12.2-green)
![License](https://img.shields.io/badge/License-GPL--3.0-green)

**English** | [简体中文](README.md)

An Applied Energistics 2 (AE2) add-on for Minecraft 1.12.2 that supercharges the AE2 Memory Card with two features: **bulk configuration copying** and **advanced P2P tunnel management**.

> **Current version: `1.1.0-1.12.2`** · Minecraft 1.12.2 / Forge 14.23.5.2860
> See [CHANGELOG.md](CHANGELOG.md) for details.

## 🌟 Features

### 🔧 Two modes

The mode is stored in the item's NBT and can be switched at any time:

| Mode | Description |
| --- | --- |
| **Copy Mode** | Select a region and paste a copied AE2 device configuration onto every AE2 block and part inside it |
| **Config Mode** | Right-click a P2P tunnel part to open a GUI that manages all P2P tunnels on the network |

### 📋 Copy Mode

- **Region paste** — copy one device into the card, mark two opposite corners, then right-click the air to apply.
- **Blocks and parts** — every `AEBaseTile` (controller, interface, storage bus, …) and every part on an `IPartHost` in the region is handled.
- **Volume limit** — regions larger than the server-side `maxCopyVolume` (default 2048 blocks) are rejected with a message.
- **Visual selection** — the region is drawn with distinct colours for each state (first corner / second corner / ready to paste), plus separate colours for valid and out-of-range targets.
- **Live feedback** — item tooltip and chat report marked positions, region size and how many targets were actually modified.
- **Auto-open options panel** — after each successful copy the panel opens unless disabled on the card.

#### Paste options (7 toggles, stored on the card)

| Option | Applies |
| --- | --- |
| Settings | Output side, fuzzy mode and other `IConfigManager` state |
| Priority | Storage bus / interface priority |
| Config slots | Filter and marking slots |
| Patterns | Interface pattern slots |
| P2P type | Tunnel type (rebuilds the part when it differs) |
| P2P frequency | Tunnel frequency |
| Name | Custom name (blocks only) |

- **Unchecked options keep the target's current value** — they are not cleared. This is the main difference from AE2's own memory card, and the reason the mod does not reuse `uploadSettings`.
- **A card that was never configured pastes nothing** — press `G` and select what to apply; an empty mask only prints a hint and keeps the selection.
- **Upgrades are not an option**: AE2 1.12.2 never exports `upgrades` into the memory card.
- **Name applies to blocks only**: AE2 does not export `display` for parts, so part names are left untouched.

### 🕸 Config Mode

Right-click any **P2P tunnel part** to open the GUI:

- **P2P tree** — scans the network and groups tunnels by type / channel, with expand-collapse and search filtering.
- **Detail panel** — type, frequency, role (input/output) and connection state of the selected node.
- **Rename / alias** — rename a single tunnel or an entire channel (the channel's input side); names are saved immediately.
- **Bind frequency** — bind to a 4-digit hex frequency (e.g. `0A1B`). If the frequency already has another input, the device becomes an output; `0000` (unbound) is rejected.
- **Assign new channel** — allocate a fresh free frequency and make the selected tunnel its input.
- **Auto configure I/O (ME P2P)** — decides input/output role based on whether the external network has an ME Controller.
- **Highlight** — renders the channel in the world (own / input / output use different colours), optionally turning the player's head, and prints a position message in chat.
- **Cross-dimension lookup** — position payloads carry the dimension id, so tunnels in other dimensions can be located.

### ⌨️ Key bindings

| Key | Action |
| --- | --- |
| `V` | Switch between Copy Mode and Config Mode while holding the card |
| `G` | Open the paste options panel (Copy Mode) |
| `Shift + right-click` | In Copy Mode: copy the target's settings into the card (AE2 behaviour); otherwise: quick mode switch |

> Both `V` and `G` can be rebound in Options → Controls → Key Bindings → Advanced Memory Card.

## 📦 Installation

### Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.12.2 |
| Forge | 14.23.5.2860 or newer |
| Applied Energistics 2 | AE2 Extended Life (CurseForge `570458`, developed against file `6302098`) |

> AE2 is a **required** dependency: the mod references `appeng.*` P2P and tile implementations directly.

### Steps

1. Download `advanced_memory_card-1.1.0-1.12.2.jar` from the releases page.
2. Drop it into the `mods` folder.
3. Install the dependencies listed above.
4. Launch the game — the item is available in the "Advanced Memory Card" creative tab.

## 🛠 Configuration

Config files live in `config/advanced_memory_card/`.

### Server (`server.cfg`, section `[server]`)

| Key | Default | Description |
| --- | --- | --- |
| `maxCopyVolume` | `2048` | Maximum region size (blocks) allowed for copy mode |
| `sendHighlightToChat` | `true` | Whether highlighting a P2P also prints its position to chat |

### Client (`client.cfg`)

Cosmetic settings, grouped as:

| Group | Contents |
| --- | --- |
| `client.highlight` | Auto rotate on highlight, highlight duration |
| `client.treeColors` / `client.treeLayout` | Tree background / selection / hover colours, indent, row height, icon spacing, name length |
| `client.panelColors` / `client.panelLayout` | Detail panel colours, status colours, layout metrics |
| `client.buttonColors` | Button default / hover / pressed / text colours |
| `client.copyColors` | Copy mode selection box and per-state colours |
| `client.highlightColors` | In-world highlight colours for own / input / output |
| `client.itemColors` | Item tint colours |

> Colours are hex values (ARGB for GUI, RGB for world rendering); changes take effect after a restart.

## 📖 Quick start

### Copy Mode

1. Hold the card in Copy Mode and **Shift + right-click** the source AE2 device.
2. (Optional) Press `G` or use the auto-opened panel to pick which options to apply.
3. **Right-click** one corner block to mark the start (red).
4. **Right-click** the opposite corner to mark the end (white) — the card is now ready (green).
5. **Right-click the air** to apply the configuration to every AE2 device inside the region.

### Config Mode

1. Switch to Config Mode and **right-click** any P2P tunnel part.
2. Browse or search the tree on the left; the right panel shows details of the selection.
3. Use the panel buttons to rename, bind a frequency, assign a new channel, auto-configure I/O or highlight.

## 🔧 Building from source

```bash
# Run the client (dev environment)
./gradlew runClient

# Run a dedicated server
./gradlew runServer

# Build the release jar
./gradlew build
```

- **ForgeGradle 3**, **JDK 8**, Gradle 4.9 (pinned by the wrapper).
  If your default JDK is newer, point `JAVA_HOME` (or `-Dorg.gradle.java.home=<jdk8>`) at JDK 8 — do not hardcode it in `gradle.properties`, or CI will fail immediately.
- **China mirrors are opt-in**: the Aliyun / BMCLAPI repositories in `build.gradle` are only used with `-DcnMirror=true`.
  BMCLAPI redirects some artifacts to a domain that no longer resolves, and ForgeGradle 3 does not fall back to the next repository.
- AE2 package-private members (e.g. `PartP2PTunnel#setOutput`) are reached through `src/main/resources/META-INF/accesstransformer.cfg`; the reflective entry points are centralised in `P2PManager`.
- This branch has no Mixin and no datagen: GUI art lives in `assets/advanced_memory_card/textures/gui/`, models and language files are maintained directly under `src/main/resources`.

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md).

## 📄 License

Licensed under [GNU GPL-3.0](LICENSE.txt).
