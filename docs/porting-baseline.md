# 移植基线（1.21.1）

本文是四个版本共用的**语义契约**。下游移植时逐条对照；凡与本文不一致的实现，视为缺陷而不是"版本差异"。
版本特有能力裁剪（例如 1.12.2 无 `IPart#importSettings`）写在 `docs/porting-plan.md`，不写在这里。

---

## 1. 复制模式：粘贴选项掩码

### 1.1 位序（不可改动，跨版本必须一致）

| bit | 枚举 | 导出内容 | `preserveTargetValue` |
| --- | --- | --- | --- |
| 0 | `UPGRADES` | 升级卡 | false |
| 1 | `SETTINGS` | 通用配置（`IConfigurableObject`） | false |
| 2 | `PRIORITY` | 优先级 | false |
| 3 | `CONFIG_INV` | 配置槽（过滤/标记） | false |
| 4 | `PATTERNS` | 样板 | **true** |
| 5 | `P2P_TYPE` | P2P 隧道类型 | **true** |
| 6 | `P2P_FREQUENCY` | P2P 频段 | false |
| 7 | `CUSTOM_NAME` | 自定义名称 | **true** |

`ALL = 0xFF`。

### 1.2 `preserveTargetValue` 的含义（这条最容易移植错）

AE2 的导入实现把「数据缺失」分成两种处理：

- **跳过型（false）**：取不到该键 → 对应步骤不执行，目标保持原值。因此「不应用」= 从交给导入方的数据里**移除该键**。
- **清空型（true）**：取不到该键 → AE2 把目标写成空值。具体三处：
  - `PatternProviderLogic#importSettings` 用 `getOrDefault(EXPORTED_PATTERNS, EMPTY)`，缺失会先清空样板再按空内容回填；
  - `P2PTunnelPart#onUseItemOn` 必须读到 `EXPORTED_P2P_TYPE` 才进入「换型 + 导入」分支，缺失会让整个粘贴动作不执行（连升级/设置一起失效）；
  - `AEBaseBlockEntity` / `AEBasePart#importSettings` 直接把名称赋给 `customName`，缺失即清名。

  因此这三项在**取消勾选**时不能靠移除表达，必须先用**目标自身导出的值**回填该键，语义才等于「这项不复制、目标保持原样」。

### 1.3 卡片级键名与存放位置

| 键 | 含义 | 位置 |
| --- | --- | --- |
| `apply_options_mask` | 位掩码 | 卡片**根标签** |
| `auto_open_options` | 复制完成后是否自动弹选项界面 | 卡片**根标签** |
| `mode_data` | 当前模式数据（`type` + `start_pos`/`end_pos`/`is_copying`） | 卡片根标签下的子标签 |

放在根标签而不是 `mode_data` 里，原因：`CardMode#cycleMode` 每次切模式都用新建的模式实例整体重写 `mode_data`，选项会被重置成默认值。
写回时必须「读出现有根标签 → 替换 `mode_data` → 写回」，不得整体覆盖（`CardMode#saveDataToStack`）。

### 1.4 缺省值

- 掩码缺省 `0`（全不选）→ 粘贴不改动任何可选项，但仍会写回「重命名 / P2P 类型 / 频段」这些不在选项内的部分。
- 低于 1.21.1 的 GUI 若在首帧可能读到空值（`Container` 先于 Screen 构造），掩码缺省必须按 `0xFF` 处理，并且 `mask == 0` 按全选解释，避免"打开界面 → 确认 → 什么都不粘贴"。
- `auto_open_options` 缺省 `true`。

### 1.5 粘贴流程

1. 从卡片算 `applyMask`。
2. `createFilteredCard`：`card.copy()` 后对所有未勾选项 `remove(component)`。
3. 若存在「未勾选的清空型选项」，对每个目标单独 `withTargetValues(filtered, targetExport, mask)` 回填后再导入。
4. 方块实体走 `importSettings(SettingsFrom.MEMORY_CARD, source, player)`；部件走 `part.onUseItemOn(cardCopy, player, hand, pos.getCenter())`，且**必须传真实手持**（AE2 在 OFF_HAND 时忽略）。
5. 结束时清空 `start/end/is_copying` 并写回卡片。

---

## 2. P2P 频段规则

### 2.1 绑定角色仲裁（所有隧道类型统一，**不豁免 ME**）

```
existingInput = P2PService.getInput(freq)
shouldBeOutput = existingInput != null && existingInput != 本机
if (shouldBeOutput != 本机.isOutput()) setOutput(shouldBeOutput)
updateFreq(本机, freq)
host.markForSave()
```

为什么 ME 也必须走：`P2PService.inputs` 是 `HashMap<Short, P2PTunnelPart>`，一个频段只存一个输入端，后登记的把先前的挤出映射；`outputs` 是独立 Multimap。跳过仲裁的结果是「把第二个 ME P2P 绑到已有通道」只会再登记一个输入端，`outputs` 永远为空，而 `isConnected` 对输入端要求「至少一个同频输出端」，于是整条通道永久显示未连接，并且这个坏状态会随 `setOutput`/`updateFreq` 的 `markForSave` 写进存档。

### 2.2 `freq == 0` 一律拒绝

`0000` 表示未绑定，不是可绑定目标。`bind("0000")` 必须不改变 `getFrequency()`。
AE2 的 `P2PService` 会把 `freq=0` 的设备登记进 `inputs[0]`/`outputs[0]`，让一堆未绑定设备在 GUI 里互相"连通"；把它们当作绑定目标等于静默解绑。

### 2.3 服务映射补登（`synchronizeServiceMappings`）

部件的 freq/output 持久化在方块 NBT 里，`P2PService` 的 `inputs`/`outputs` 是纯运行时映射，只在节点并入网格时重建。重进存档期间运行时映射尚未建全，`isConnected()` 结果不可信。

规则：
- 输出端：已在 `outputs` 中则跳过；否则 `updateFreq`（`SetMultimap` 重复登记幂等）。
- 输入端：**只在 `getInput(freq) == null` 时补登**。已经有人占位就不抢，否则会把同频段真正的输入端挤出映射，表现为"重进存档前后状态/别名会变"。
- `freq == 0` 不参与对齐。

### 2.4 坏通道修复（`repairChannelInputs`）

触发条件：同一频段有 ≥2 个输入端，且该频段**没有任何输出端**（`registered.getOutputs().isEmpty()`），且登记端节点在线（离线时 `getOutputs()` 恒空，无法判断）。

处理：保留一个输入端，其余同类型部件 `setOutput(true)` 后 `updateFreq`。
保留规则：同频段候选里**有且仅有一个**带非空自定义名的，取它；否则取 `P2PService` 当前登记的输入端。
跨类型不改写（不同隧道的输入/输出概念不通用）。

### 2.5 其他

- 频段展示格式：`%04X`（1.21.1 / 1.20.1 / 1.12.2）；1.7.10 为 `%016X`。
- 频道别名 = 该频段中第一个输入端部件的自定义名；没有则回退频段 hex。
- 改名：只写 `customName` 字段，不经过 `importSettings`（那条路会顺带执行升级/优先级/配置导入，并在带频段数据时 `setOutput(true)`）。写完必须 `markForSave()` + `markForUpdate()`。
- `fixZeroFrequencyAnomalies`：只对 `freq == 0` 且当前仍判定为连接的部件，先 `updateFreq(part, 0)` 摘除旧映射，再交给 `processSingleP2PPart` 按「外部网络有控制器 → 输入端；否则输出端」决定角色。外部节点/外部网络未就绪时**不做任何写入**。

---

## 3. 客户端/服务端边界

- 渲染只能在客户端：高亮由客户端 Screen 用**已同步的** `P2PInfo` 反查 `IPartHost.getPart(Direction)` 后触发，服务端只负责发聊天坐标与传送链接。
- 服务端类不得引用 `net.minecraft.client.*`；`DistExecutor` 包一层不算数（专用服加载 `P2PManager` 时会解析方法签名）。
- 传送链接格式：`/execute in <dimension id> run tp @p <x> <y> <z>`。

---

## 4. UI 约定

- 8 个选项的排列顺序与 §1.1 表格一致（bit0 → bit7），翻译键 `copy.options.<name>`（小写下划线）。
- 未配置过的卡默认全部**不勾选**（掩码 0），与服务端 `getApplyMask` 缺省一致。
- 掩码提交用**整张掩码**而不是 delta（幂等，重复包/丢包不会写歪）。
- 界面打开期间卡片可能被换走：所有写操作都从 `InteractionHand` 重新取栈，不缓存构造期的 `ItemStack`。
