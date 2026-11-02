# AGENTS.md — Advanced Memory Card 多版本协作协议

本文件面向所有在本仓库工作的智能体（Agent）。它在**所有分支**生效，优先级低于用户的明确指令，高于 README、docs 与个人经验。

**开工前必须做的事：**

1. 确认当前在哪个分支、哪个版本目录（见第一节）。
2. 读本文件对应版本的硬约束（第三节）与契约（第四节）。
3. 涉及界面、协议、i18n 时，先读源码，再动手。

---

## 一、仓库拓扑

单远程仓库：`git@github.com:suntide-20210418/AdvancedMemoryCard.git`。
四个长期版本分支，各自有独立工作副本：

| 分支 | 工作目录 | Minecraft | 加载器 | Java | 定位 |
| --- | --- | --- | --- | --- | --- |
| `suntide/1.21.1` | `AdvancedMemoryCard-1.21.1` | 1.21.1 | NeoForge 21.1.219 | 21 | **行为基准** |
| `suntide/1.20.1` | `AdvancedMemoryCard-1.20.1` | 1.20.1 | Forge 47.4.10 | 17 | 基准回移 |
| `suntide/1.12.2-1.1.0` | `AdvancedMemoryCard-1.12.2` | 1.12.2 | Forge 14.23.5.2860 | 8 | 旧版重写 |
| `suntide/1.7.10-GTNH` | `AdvancedMemoryCard-1.7.10` | 1.7.10 | Forge 10.13.4.1614 | 8（Jabel） | GTNH 兼容 |

**移植方向：1.21.1 → 1.20.1 / 1.12.2 / 1.7.10。** 基准是行为规格，不是可直接拷贝的代码。
1.21.1 / 1.20.1 使用 AE2 的 `ScreenStyle` + vanilla `AbstractWidget`（由 AE2 的容器分发）；1.12.2 / 1.7.10 没有这套机制，使用自绘 `Widget` + `GuiDraw` + `ConfigUiLayout`。**不要把这两类 UI 代码逐字互抄。**

包名与 ID 四版一致：

```
com.suntide_20210418.advancedmemorycard
mod_id = advanced_memory_card
```

---

## 二、第一原则

1. **源码是唯一权威。** 结论必须来自实际实现、字节码或运行时证据。禁止用"通常 / 应该 / 我记得"兜底。没验证就说没验证。
2. **先读后改。** 改任何方法前，先确认它的调用方、返回语义与历史包袱（旧存档兼容、指纹稳定性、异步回调顺序）。
3. **不擅自删"看起来不对"的代码。** 很多分支保留双保险（如 `setOutput` 的 AT + 反射），删除前必须有编译或实机证据。
4. **不加没有数据支撑的抽象。** 没有明确需求或性能数据时，不引入缓存索引、通用接口、额外框架层。
5. **不动 `.workbuddy/`。** 那是项目数据，不是缓存。
6. **不做顺手重构。** 一个提交只解决一类问题；跨文件的机械改名单独提交。

---

## 三、分支硬约束

### 3.1 1.21.1（基准）

- Java 21，NeoForge 21.1.219，ModDevGradle 2.0.140，Parchment `2024.11.17`。
- 使用 Mixin（`advanced_memory_card.mixins.json`，`compatibilityLevel: JAVA_21`）：
  - `P2PTunnelPartMixin` 公开 `P2PTunnelPart#setOutput`；
  - `AEBasePartAccessor` 直写 `AEBasePart#customName`，绕开 `importSettings` 的副作用。
- 资源由 datagen 生成到 `src/generated/resources/`：`ModRecipesProvider`、`ModItemModelsProvider`、`ModEnusLangProvider`、`ModZhcnLangProvider`。
- 改语言、配方、模型后必须跑 `./gradlew runData`，并把生成结果一起提交。

### 3.2 1.20.1

- Java 17，Forge 47.4.10，ForgeGradle `[6.0.16,6.2)`，Parchment `2023.09.03-1.20.1`。
- 应用 Spotless 8.0.0，但 `enforceCheck = false`；CI 不跑 `spotlessCheck`。格式化用 `./gradlew spotlessApply`，不要手改格式。
- 语言同样走 datagen（`src/generated/resources/.../lang/*.json`）。

### 3.3 1.12.2

- **Java 8 目标。** `build.gradle` 固定 `sourceCompatibility = targetCompatibility = '1.8'`。
- **禁止 Java 9+ 语法**：`var`、switch 表达式、`case ->`、`instanceof X y` 模式匹配、record、文本块、`List.of` 等。
- ForgeGradle 3.0.197，MCP `snapshot 20171003-1.12`，AE2 Extended Life（rv6 系）。
- 本地构建可能因 `maven.minecraftforge.net` 不可达而失败（TLS handshake 被终止）；此时依赖 CI 的 `./gradlew build`，不要为了"让它过"去改仓库或降级依赖。
- 语言文件是 `.lang`，小写文件名，首行 `#PARSE_ESCAPES`（见第六节）。

### 3.4 1.7.10

- GTNH 约定插件：`build.gradle.kts` 只有 `plugins { id("com.gtnewhorizons.gtnhconvention") }`，插件版本在 `settings.gradle.kts`（`gtnhsettingsconvention 2.0.20`），Gradle wrapper 9.3.1。
- `enableModernJavaSyntax = jabel`：**源码可以用现代语法，产物仍是 Java 8 字节码。** 但仍不得使用 Jabel 不支持的语言特性（record、sealed、switch 模式匹配等）；拿不准就写 Java 8 风格。
- 语言文件是 `.lang`，**大写**文件名 `en_US.lang` / `zh_CN.lang`，**没有** `#PARSE_ESCAPES`。

---

## 四、跨版本契约（不得改）

以下语义在四个分支必须一致。改动任何一条都算破坏协议。

### 4.1 服务端语义（C1）

1. **双锚点分离。** `p2pTunnelPartBinding`（扫描锚点）与 `pendingBindTarget`（配对源）是两个独立字段。`getBindTarget()` 优先返回 pending，且要求它仍在本次扫描结果内，否则回退到扫描锚点。UI 选中设备只改 pending，不动扫描锚点。
2. **`bindTo(target, freq)` 固定顺序。** 拒绝 `freq == 0` → 取 grid 与 P2P 服务 → 角色仲裁（已有输入端且不是目标时，本机为输出端）→ 设 output → `updateFreq` → `markForSave`。顺序不可交换。
3. **`switchToInput(target)` 必须先降级全部旧输入端，再提升目标。** 否则旧输入端会变成"`output == false` 却不在 inputs 里"的孤儿并被写进存档。降级要遍历设备表，末尾再用 `getInput(freq)` 兜底。
4. **频段未变化时，隧道网络重算必须显式触发。** 各版 `updateFreq` 行为不同；凡未内部触发者，角色变更后必须手动调用一次网络变更回调。
5. **`renameP2P` 只写自定义名，不改角色。** 不得走 settings 导入路径（会顺带升级、优先级、配置槽导入，并在带频段数据时把设备设为输出端）。写完必须同时落盘标记与方块更新标记。
6. **`freq == 0` 一律拒绝绑定。** `0000` 表示未绑定，不是可绑定目标；绑定它不得改变 `getFrequency()`。

### 4.2 状态码（C2）

16 个码，编号与语义跨版本一致，**不得重排**。

| 码 | 常量 | 参数 | severity |
| --- | --- | --- | --- |
| 0 | `NONE` | — | NEUTRAL |
| 1 | `BIND_OK` | 频段 | OK |
| 2 | `BIND_NO_TARGET` | — | WARN |
| 3 | `BIND_INVALID_FREQ` | 频段 | WARN |
| 4 | `BIND_NO_GRID` | — | ERROR |
| 5 | `SOURCE_SELECTED` | 设备名 | OK |
| 6 | `ASSIGNED` | 频段 | OK |
| 7 | `RENAMED` | 新名 | OK |
| 8 | `REFRESHED` | — | OK |
| 9 | `RENAME_FAILED` | — | WARN |
| 10 | `BIND_BATCH_OK` | `ok\|freq` | NEUTRAL |
| 11 | `BIND_BATCH_PARTIAL` | `ok\|fail\|freq` | WARN |
| 12 | `SWITCHED_TO_INPUT` | 频段 | OK |
| 13 | `SWITCH_NO_CHANNEL` | 频段 | WARN |
| 14 | `SWITCH_ALREADY_INPUT` | — | WARN |
| 15 | `SWITCH_FAILED` | — | ERROR |

severity 只有 `NEUTRAL / OK / WARN / ERROR` 四值。10、11 不得漏映射（否则批量成功显示为灰）。

### 4.3 action 名称与参数格式（C3）

四版 action 名称完全一致：

```
refresh_p2p            (无参数)
update_item_info       (无参数)
bind_frequency         频段（4 位十六进制）
bind_frequency_batch   频段;位置1;位置2;...
set_channel_alias      频段|别名
set_p2p_alias          位置::别名
set_pending_bind       位置
clear_pending_bind     (无参数)
auto_config_io         (无参数)
assign_freq            (无参数)
switch_to_input        位置
teleport_p2p           位置
highlight_p2p          位置
highlight_p2p_tunnel   频段
```

`位置` 的编码固定为：

```
x|y|z|direction.ordinal()|dimensionId
```

**服务端必须重新校验一切客户端参数**：频段非 `0000`、传送受 `server.allowTeleport` 约束、位置能解析到真实 `PartP2PTunnel`。不要因为"按钮已经灰了"就省略校验。

### 4.4 交互语义（C4）

- 左键点设备行 = 设为配对源（只改 pending）。
- 左键点频段行 = 填进配对条目标槽（普通模式）／整条频段加入多选（批量模式）。
- `Ctrl` 单击设备行 = 加选／取消加选；跨类型时整组换掉，**不混合类型**。
- `Ctrl` 拖动 = 框选；方向由起手行决定（已全选则本次为取消），每次从按下瞬间的快照重算。
- 批量模式：单击即加选、拖动即框选；频段行的展开折叠挪到右键。
- 回车 = 绑定；Esc = 取消改名／收起下拉。

### 4.5 布局常量（C5）

只允许集中在布局常量类（1.12.2/1.7.10 为 `ConfigUiLayout`），禁止散落到控件。

| 区域 | 值 |
| --- | --- |
| 面板 | `440 × 312` |
| 配对条 | `x=10 y=20 w=420 h=52` |
| 搜索 | `x=18 y=76 w=120 h=13` |
| 树 | `x=18 y=102 w=231 h=175` |
| 滚动条 | `x=236 y=102 w=14 h=175` |
| 检查器 | `x=250 y=75 w=179 h=207` |
| 操作条 | `x=10 y=288 w=420 h=17` |
| 列头偏移 | `10` |

树列参数化项：`rowHeight=14`、`indentWidth=10`、`nameMaxLength=14`、`freqColumnWidth=40`。

### 4.6 数据格式（C6）

- 频段统一为 `String.format("%04X", freq & 0xFFFF)`，大写 4 位。
- `0000` = 未绑定，不是可绑定目标。
- `TreeNode.getNodeId()` 必须是稳定标识，禁止用 `hashCode()`。P2P 分支的 id 形如：

```
p2p:<dimensionId>@<x>,<y>,<z>:<direction.ordinal()>:<frequency>
```

`p2pNodeId(P2PInfo)` 必须与上述字符串**逐字一致**，否则重建树后选中行会丢失。

---

## 五、共享文件与漂移检查

1.12.2 与 1.7.10 共用以下文件，以 **1.12.2 为权威副本**（1.7.10 复制后只跑格式化）：

```
ConfigUiLayout、GuiDraw、
PairBarWidget、ActionBarWidget、FlatButton、
P2PChannelUsage、TreeNode、ActionStatus、NodeType
```

> 注意：`docs/config-ui-porting.md` 的冻结清单里还列了 `InputHandler`、`PointerState`，但这两个类在两老版中均不存在。以本清单（实际存在的文件）为准；看到文档与磁盘不一致时，先核对再引用。

允许不同、但类名／方法签名／常量字段名必须一致：

```
ConfigModeScreen、P2PTreeWidget、DetailPanelWidget、
P2PTypeNames、TranslateHelper、ClientConfig、
P2PManager、ConfigModeMenu、ConfigModeSyncPacket
```

跨副本校验：

```bash
for f in <清单>; do
  diff -u "AdvancedMemoryCard-1.12.2/$f" "AdvancedMemoryCard-1.7.10/$f" || echo "DRIFT: $f"
done
```

有漂移时，先判断是"分支差异"还是"单边漏改"，再决定同步方向；不要无脑覆盖。

---

## 六、i18n 规则

### 6.1 文件格式

| 分支 | 位置 | 文件名 | 转义 |
| --- | --- | --- | --- |
| 1.21.1 | `src/generated/resources/assets/advanced_memory_card/lang/` | `en_us.json` / `zh_cn.json` | JSON + datagen |
| 1.20.1 | 同上 | 同上 | JSON + datagen |
| 1.12.2 | `src/main/resources/assets/advanced_memory_card/lang/` | `en_us.lang` / `zh_cn.lang` / `ru_ru.lang` | 首行 `#PARSE_ESCAPES`，`\n` 生效 |
| 1.7.10 | 同上 | `en_US.lang` / `zh_CN.lang` | **无** `#PARSE_ESCAPES`；`TranslateHelper` 手动 replace `\\n` |

### 6.2 键前缀（最容易踩的坑）

`TranslateHelper` 的 `GUI_PREFIX = "gui." + BASE_KEY + "."`，而各子前缀又拼了一次 `advanced_memory_card.`。因此新 UI 的完整键是**双前缀**：

```
gui.advanced_memory_card.advanced_memory_card.pair.source
gui.advanced_memory_card.advanced_memory_card.action_bar.hint
gui.advanced_memory_card.advanced_memory_card.status_line.idle
gui.advanced_memory_card.advanced_memory_card.tree_col.name
```

写成单前缀 `gui.advanced_memory_card.pair.source` **不会报错，但也不会被解析**，界面上直接显示原始 key。新键一律对照 `TranslateHelper.Keys` 里的实际拼接结果，不要凭手感写。

### 6.3 键集合一致性

- en / zh 必须同键集合；新增一个键必须同时补两种语言。
- 1.12.2 的 `ru_ru.lang` 目前按"只保证 en/zh"处理；新增键可不同步，但不得删除已有键。
- 不允许把原始 key 留在界面上。提交前跑一次自检：

```bash
# 以 1.12.2 为例：确认新 UI 键都是双前缀
for p in pair action_bar status_line tree_col tree; do
  echo "$p: $(grep -cE "^gui\.advanced_memory_card\.advanced_memory_card\.${p}\." \
    src/main/resources/assets/advanced_memory_card/lang/en_us.lang)"
done
```

### 6.4 1.21.1 / 1.20.1 的额外规则

语言 JSON 是 datagen 产物。**改 `src/main/java/.../datagen/ModEnusLangProvider.java` 与 `ModZhcnLangProvider.java`，然后 `./gradlew runData`**；不要只手改 `src/generated/resources`，否则下次 datagen 会被覆盖。生成结果必须提交。

### 6.5 类型名解析差异

- 1.12.2：`P2PTypeNames` 走 `item.ae2.<path>.name`，**不读** `p2p_type.*` 键。
- 1.7.10：使用 `gui.advanced_memory_card.advanced_memory_card.p2p_type.*` 键。

不要把一个分支的键集直接搬到另一个分支并假设会被使用。

---

## 七、UI 与输入规则

1. **分派顺序固定**：下拉浮层 → 滚动条 → 树 → 检查器 → 操作条。
   - 滚动条矩形压在树右缘的 `14px` 留白上；必须先判滚动条，否则滑块拖不动。
2. **`Widget` 基类不派发 `onClick`。** 1.12.2 / 1.7.10 的 `Widget#mouseClicked` 只返回 `false`；`P2PTreeWidget` 必须在自己的 `mouseClicked` 里显式调用 `onClick(mouseX, mouseY)` 并返回 `true`。基准里这是 AE2 `AbstractWidget` 负责的。
3. **不要每 tick 重建整棵树。** 用 `syncVersion` / 数据指纹做节流；只有服务端数据变化或搜索词变化时才重建。
4. **选中态按 id 恢复，不按引用。** 树每 tick 可能重建，`TreeNode` 每次都是新对象。
5. **`P2PPosition` 的相等性必须包含维度。** 主世界与下界可以存在同坐标、同朝向的两台 P2P。
6. **Ctrl 判定统一由 Screen 采样**（1.12.2/1.7.10 用 `GuiScreen.isCtrlKeyDown()`），控件不各自读键盘状态。

---

## 八、配置规则

- 新界面的语义色统一走 `client.palette.*`（`paletteTextPrimary`、`paletteAccent`、`paletteRowSelected`、`paletteChipOkBg` 等）。
- 旧的 `client.treeColors` / `panelColors` / `buttonColors` **保留**，用于旧配置升级不崩；不要删除或改键名。
- `server.allowTeleport` 控制传送能力；`server.sendHighlightToChat` 控制高亮是否写聊天栏。服务端必须再判一次 `allowTeleport`。
- 修改默认值要有视觉或行为理由；不要为了"看起来更整齐"改配色默认值。

---

## 九、构建与验证

| 分支 | 构建 | 运行客户端 | 数据生成 |
| --- | --- | --- | --- |
| 1.21.1 | `./gradlew build` | `./gradlew runClient` | `./gradlew runData` |
| 1.20.1 | `./gradlew build` | `./gradlew runClient` | `./gradlew runData` |
| 1.12.2 | `./gradlew build` | `./gradlew runClient` | — |
| 1.7.10 | `./gradlew build` | `./gradlew runClient` | — |

- CI：`.github/workflows/build.yml` 在任意分支 push / PR 时跑完整 `build`；`release.yml`（1.21.1/1.20.1）与 `release-tags.yml`（1.7.10）由 tag 触发。
- **CI 通过 ≠ 界面正确。** 布局、颜色、点击命中、tooltip 必须在 `runClient` 里看过；没有实机验证就在报告里明确写"未实机验证"。
- 1.12.2 本地构建失败时，先确认是不是依赖下载问题（`maven.minecraftforge.net`），不要改构建脚本绕过。
- 提交前至少做一次静态自检：
  - 1.12.2：`grep -nE "\bvar |case .* ->|instanceof [A-Za-z]+ [a-z]"` 应为空。
  - 1.12.2/1.7.10：`fontRendererObj` / `thePlayer` / `mouseMovedOrUp` / `xPosition` 不应出现在 1.12.2；反过来 1.12.2 的 `fontRenderer` / `player` / `mouseReleased` / `.x` 不应出现在 1.7.10。

---

## 十、提交与文档

### 10.1 提交范围

**只提交**：代码文件、资源文件、构建文件、文档文件、GitHub Actions。

**禁止提交**：`.workbuddy/`、`build/`、`run/`、`.gradle/`、`.idea/`、日志、临时产物、jar（发布产物由 CI 生成）。

### 10.2 提交信息

使用 Conventional Commits，scope 用模块名：

```
feat(config-ui): 配置模式界面按 1.21.1 基准重做
fix(config-ui): 树行点击重新派发到 onClick，滚动条先于树判定
fix(i18n): 修正 pair/action_bar/status_line 的键前缀
docs(porting): 记录 1.12.2 的键覆盖现状
```

一个提交只做一类事；格式化、改名、行为变更分开。

### 10.3 文档同步

- 行为变更必须更新 `CHANGELOG.md`；跨版本契约或布局变更必须更新 `docs/config-ui-porting.md`。
- **文档里的类名、常量、键名、数值必须回源码验证。** 不允许写"应该""大概""与某版一致"而不核对。
- 发现文档与代码不一致时，以代码为准修正文档，或在文档中明确标注"待验证"，不要留着假结论。

### 10.4 推送

- 完成工作后提交并推送到当前分支的 upstream。
- 不对共享分支做 `push --force`；需要改写历史时先征得用户同意。
- 发布流程（改版本号、更新日志、打 tag、创建 PR）按用户明确指令执行，不自行触发。

---

## 十一、常见陷阱清单

提交前逐条确认：

- [ ] 新 i18n 键是**双前缀** `gui.advanced_memory_card.advanced_memory_card.*`。
- [ ] en / zh 键集合一致；没有把原始 key 留在界面上。
- [ ] 1.12.2 没有 `var`、switch 表达式、模式匹配、record。
- [ ] 1.7.10 语言文件名是 `en_US.lang` / `zh_CN.lang`，且没有 `#PARSE_ESCAPES`。
- [ ] 1.12.2 用的是 `fontRenderer` / `player` / `mouseReleased` / `searchField.x`；1.7.10 才是 `fontRendererObj` / `thePlayer` / `mouseMovedOrUp` / `xPosition`。
- [ ] `GuiTextField` 构造参数数量与当前版本一致（1.12.2 需要 `componentId`）。
- [ ] `FlatButton` 的 `width/height` 是否可写（1.12.2 需要 setter）。
- [ ] `TreeNode.p2pNodeId()` 与 `getNodeId()` 的 P2P 分支逐字一致。
- [ ] `P2PPosition` 的 `equals/hashCode` 包含维度。
- [ ] `0000` 频段没有被当成可绑定目标。
- [ ] 传送与切换输入端在服务端做了二次校验。
- [ ] 树没有每 tick 重建；选中态按 id 恢复。
- [ ] 布局常量来自 `ConfigUiLayout`，没有散落魔数。
- [ ] 共享文件与另一老版 `diff` 无意外漂移。
- [ ] 没有提交 `.workbuddy/`、`build/`、`run/`、IDE 文件。

---

## 十二、智能体工作流

1. **定位**：确认分支、版本、工作目录；读本文件与该分支的 `docs/`。
2. **取证**：用 `Read` / `Grep` 找到真实实现与调用方；不要凭类名推测行为。
3. **计划**：涉及 3 个以上文件或跨版本契约时，先列出改动面与风险，再动手。
4. **最小改动**：只改必要的文件；不做顺手的重命名、格式化、重构。
5. **验证**：静态检查 → 构建 → （可行时）`runClient`；把没验证的部分写清楚。
6. **提交**：按第十节的范围与信息格式提交、推送。
7. **报告**：说明改了什么、依据是什么、哪些还没验证、下一步是什么。

**如果任务与以上协议冲突，先停下来说明冲突点，由用户裁决。**
