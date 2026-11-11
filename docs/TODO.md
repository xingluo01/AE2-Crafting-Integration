# AE2-Crafting-Integration 需求待办表

> 状态图例：⬜ 未完成 | ⏸ 暂缓；优先级：P1 最高 | P2 | P3。
> 本表只放**还没做完、能验收的事**。写法见 `TODO_STYLE.md`；工程约束见 `ARCHITECTURE.md`；发版见 `RELEASE.md`。

## 一、未开始

### 9x9 通用分子装配室 — P1

- ⬜ 新增**9x9 合成网格**：方块实体维护 81 格输入并按配方匹配。
  - 判据：放入 9x9 原料能产出正确结果，空格不参与匹配。
- ⬜ 新增**催化剂槽排**：网格下方一排槽位，按工作台规则决定消耗或保留。
  - 判据：催化剂在合成后仍留在槽内，普通原料正常消耗。
- ⬜ 新增**网格 tick 与节点休眠**：方块实体接 `IGridTickable`，按 `getProgressPerTick()` 推进进度（档位表已就位：0..4 张加速卡对应 10 / 16.9 / 28.9 / 40 / 250 每 tick）。
  - 判据：放入原料后进度条推进；插满 4 张加速卡时每 tick 完成一次；无任务时节点休眠、不占 tick。
- ⬜ 新增**样板供应器对接**：实现 AE2 `ICraftingMachine` 并登记 `CRAFTING_MACHINE` 能力。
  - 判据：样板供应器能把样板推入装配室并完成一次合成。
- ⬜ 支持**合成拓展工作台**：配方匹配已在编码终端侧落地（`integration/extendedcrafting`，按 tier 等值匹配），装配室执行待接。
- ⬜ 支持**无尽重生工作台**：编码端已接（见「增广样板编码终端」的无尽重生条目），装配室执行待接。

### 增广样板编码终端 — P1

- ✅ **自有菜单类型与界面**（2026-11-12）：`AugmentedPatternEncodingTermMenu.TYPE` + `AugmentedPatternEncodingTermScreen`，界面文档 `screens/ae2_crafting_integration/pattern_encoding_terminal.json` 只 include AE2 的 `terminals/terminal.json`。
- ✅ **9x9 编码台与新外框**（2026-11-12）：菜单改为直接继承 `MEStorageMenu`，81 个 `FakeSlot` 复用 `PatternEncodingLogic#getEncodedInputInv()`（自带存档）；外框按用户重绘稿改成 512×512、五片切片拼接，9x9 编码槽 / 标志工作方块槽 / 产物槽 / 空白样板槽 / 样板输出槽 / 编写样板按钮全部落在贴图框内，`TerminalStyleDocumentTest` 逐点取像素锁定。
- ✅ **编写样板入口**（2026-11-12）：按钮走客户端动作 `encode_augmented_pattern`，把 81 格 + 产物写成 `增广合成样板`；产物由「9x9 配方匹配」一节的判定器链给出。
- ✅ **9x9 配方匹配**（2026-11-12）：产物预览与编码改走 `common/logic/AugmentedRecipeMatcher` 的判定器链。原版合成台按非空格包围盒裁剪后查配方（`ShapedRecipePattern` 要求输入宽高与配方宽高**完全相等**，9×9 全图直接喂必然一次也不命中）；合成拓展与无尽按配方自身 tier **等值**匹配（`TableCraftingInput#of` / `TierInput#of` 内部同样先裁包围盒），由 `integration/ModIntegrations` 挡在 `isLoaded` 之后注册。`ExtendedCraftingMatchingGameTest` 从真实配方表随机挑一条 >3×3 的有序配方原样还原，`RecipeMatchingGameTest` 锁偏移摆放与「>3×3 让位」。
- ✅ **无尽重生工作台编码集成**（2026-11-12）：标志物槽决定工作台档位——幽匿 5×5 / 下界 7×7 / 末地 9×9 / 终极 11×11 对应 tier 1..4，表格配方按 tier **等值**匹配；无尽锻造走十字五格（模板在左、基底居中、添加物在右/上/下）。内容先取包围盒再**居中**铺进目标台，编码进样板时同样居中，装不进这张台就不出产物。`AvaritiaMatchingGameTest` 在真实配方表上锁「同一网格换标志物就换结果」与十字布局。
- ✅ **JEI 配方转移按钮**（2026-11-12）：JEI 的转移处理器按「容器类 + 配方类别」**精确查表**（`ImmutableTable`，不是 `isAssignableFrom`），AE2 的 `EncodePatternTransferHandler` 绑在 `PatternEncodingTermMenu.class` 上，本终端继承 `MEStorageMenu`，两条查表路径都落空，所以按钮不画。新增 `client/integration/jei/` 三件套：按类别 UID 反查已注册类别后逐条注册，把输入按行优先居中铺进 81 格，按配方自身 tier 自动填标志工作方块，经 `InventoryActionPacket(SET_FILTER)` 落到 `ConfigInventory`（JEI 默认的客户端 `Slot#set` 对 `FakeSlot` 无效）。覆盖原版合成、合成拓展四档台、无尽重生四档台与无尽锻造十字五格；`GridPlacementTest` 锁具体落点，`RecipeGridCategoriesTest` 锁类别表形状。
- ⬜ 新增**装配室按样板执行**：编码出的样板目前只能编不能做——装配室还没有读 `AugmentedPattern`、按网格消耗原料与推进进度的逻辑。
  - 判据：样板供应器推入样板后装配室按网格消耗原料并产出，产物进输出槽；9×9 工作台配方与 ≤3×3 原版配方都能跑。
- ✅ **标志工作方块槽持久化**（2026-11-12）：这一格不在 `PatternEncodingLogic` 的读写范围内，改由宿主实现 `common/menu/WorkstationHost` 保管——线缆面板写部件 NBT 的 `workstation` 键（拆部件时连同工作台掉落、`clearContent` 一并清空），无线终端写进物品 `CUSTOM_DATA` 的同一根 compound。菜单只认接口，`host instanceof` 失败时退回临时库存。`WorkstationPersistenceGameTest` 两边各验一次「重读 NBT / 换一个宿主后仍在」，并断言部件 NBT 里确实出现了 `workstation` 键。
- ⬜ 新增**样板记录工作站**：`AugmentedPatternData` 目前只有 9×9 网格与产物，装配室分不清「这张样板要在无尽锻造台上打」还是「在 11×11 表格台上摆」。网格形状只是隐式约定，数据包里没有显式字段。
  - 判据：样板能带上工作站标识，装配室据此选择执行路径；旧样板（无字段）按表格合成处理。
- ⬜ 给**标志工作方块槽加提示**：现在放进任何物品都只是「不出产物」（`FakeSlot` 不过滤），玩家看不出这个槽该放工作台。
  - 判据：槽位有 tooltip 或空槽底图标注，说明「放工作台以选择配方档位」。
- ⬜ 复核**窗口高度**：界面 347 高（3 行物品列表时），1080p 下 GUI 缩放 3 只剩 360 虚拟像素、缩放 4 只剩 270，会超出屏幕；同时行数随窗口高度变化，实机要看重复行与 9x9 网格的衔接是否自然。
  - 判据：GUI 缩放 2/3/4 下都能完整显示，底部部件不错位。

### 工程 — P2

- ⬜ 迁移**发布工作流**：从参考项目复制 `release.yml` 并填新项目 id。
- ⬜ 补齐**客户端实机验证**：启动 `runClient` 确认三件套外观与创造页。
  - 判据：装配室界面能打开、9x9 槽位可放物品，工作台依赖无加载报错；背景与右侧浮层贴合贴图，进度条不偏移；线缆能连上装配室，漏斗可从产物槽抽取且塞不进 9x9 网格；右侧升级面板显示 4 个槽、只吃加速卡；左侧指南按钮能打开本模组页面；面板终端贴到线缆后右键能开样板编码界面，无线终端充能并与接入点链接后能开同一界面，编码台内容在关窗重开后还在。
  - 编码终端按 206×347 的新布局重排后**还没实机看过**：五片拼接、右侧挂带的四个槽、按钮落点、`upgrades`（`right: 2`，图片宽度取了 206 后左缘落在 x=204）都只由 `TerminalStyleDocumentTest` 与 `tools/layout/preview.py` 的像素比对保证，运行时效果待确认。
  - JEI 侧**注册已验、点击未验**：`runClient` 日志里有 `Registering recipes transfer handlers: ae2_crafting_integration:jei`，说明插件被 JEI 扫到且注册无异常；但「+」按钮是否出现在各页签、点下去 81 格是否按预期居中铺开、标志工作方块槽是否自动落到正确档位、上一轮的残留格是否被清干净，仍需进世界点一遍。

## 二、暂缓

### 9x9 配方数据生成 — P3

- ⏸ 等两台机器的配方类型定稿后再接 `runData`。

## 归档索引

- 已发布内容与实现细节：`CHANGELOG.md`、git 历史、`ARCHITECTURE.md`。
- 发版流程与踩坑：`RELEASE.md`。
- 更新日志体例：`CHANGELOG_STYLE.md`。
- 本表写法：`TODO_STYLE.md`。
