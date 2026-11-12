# AE2-Crafting-Integration 需求待办表

> 状态图例：⬜ 未完成 | ⏸ 暂缓；优先级：P1 最高 | P2 | P3。
> 本表只放**还没做完、能验收的事**。写法见 `TODO_STYLE.md`；工程约束见 `ARCHITECTURE.md`；发版见 `RELEASE.md`。

## 一、待实机

### 9x9 通用分子装配室 — P1

- ⬜ 验证**装配室与样板供应器的往返**：真供应器接频道与能量后能连续发配多轮。
  - 判据：连续三份样板都能铺格、完成、产物回到网络，中途不卡。
- ⬜ 验证**装配室呼吸灯**：接入网络的装配室灯亮，断网或断能后熄灭。
  - 判据：`block/molecular_assembler_lights` 不是紫黑块，亮灭与供电一致。
- ⬜ 验证**装配室浮空产物**：一次合成后产物在方块中心浮起旋转并播一次。
  - 判据：动画只播一次、不因邻近方块更新重放，粒子正常。
- ⬜ 确认**9x9 网格槽在界面上不可操作**：拖入无反应、Shift 点击不取出、空槽按 AE2 的 20% alpha 几乎隐形、铺料后显出完整槽框。
  - 判据：界面表现与服务端拒绝一致，不出现「看着能放、放进去没反应」；槽底不叠多余黑底。
- ⬜ 确认**装配室标题与进度条**：标志物栏 / 物品栏左上角两行标题不压槽位；进度条纹路固定、从左往右长出来而不是整条平移；悬停显示 `35%` 这类百分比。
  - 判据：进度 0/50/100 三档肉眼可辨，tooltip 无「/ 100」字样。
- ⬜ 确认**升级槽快捷移动**：背包里 Shift 点加速卡直接进右侧升级面板，Shift 点普通物品仍按原顺序落进标志物栏。
  - 判据：一次 Shift 就装好卡，卡片不出现在标志物栏；其它物品行为不变。
- ⬜ 确认**ME 编码终端标题**：窗口标题是通用的「终端」，编码区左上角是「增广样板编码」，两行文字不压住 9x9 网格。
  - 判据：窗口高度变化（物品行数 3/4/5）时标题始终贴在网格上方 11px。

### 无线增广样板编码终端 — P1

- ⬜ 确认**升级面板位置与卡片**：面板贴回标准落点不压挂带；2 张能源卡 + 1 张量子桥卡能放进去，槽位仍是 2 个。
  - 判据：能源卡可叠到 2 张，量子桥卡最多 1 张，任意时刻界面只显示 2 个升级槽。
- ⬜ 确认**WUT 融合线路**：把本终端与无线通用终端放进合成台能生成带本终端的 WUT；与无线合成终端 / 无线样板编码终端 / 无线样板访问终端两两组合也能成；在 WUT 里切回本终端能打开 9x9 编码界面，且编码台与标志物槽的内容还在。
  - 判据：4 条配方在 JEI 里都能查到、合成不报错；切换终端后 `CUSTOM_DATA` 的 `augmented_pattern_encoding` 键未被其它终端覆盖；左侧有终端切换按钮、热键能切走再切回。
- ⬜ 确认**升级槽滚动阈值固定 2**：融合 3 个以上终端后，切到本终端时升级面板最多显示 2 行、其余走滚动条；窗口拉到最高也不变多；装/拆量子桥卡时奇点槽正确显示/隐藏。
  - 判据：面板不越出界面右缘、滚动条位置贴着面板、只画一份升级面板；`TerminalStyleDocumentTest#scrollingUpgradePanelKeepsTheStandardAnchor` 已锁布局，运行时效果待实机。

### 增广合成样板 — P1

- ⬜ 确认**样板 tooltip 排版**：「标志物」行在「制作」之上，带工作台的样板显示工作台名、不带时显示「不要求」；输入按物品合并；坐标只在开启高级信息（F3+H）时出现，普通悬停不显示。
  - 判据：拿截图里那张样板对照，应只出现 2 行输入；F3+H 关闭时行尾无坐标、开启时有且与编码台里摆的位置一致；「按下[P]查看样板」等既有提示仍在。`AugmentedPatternTooltipTest` 锁坐标换算、`AssemblerAutomationGameTest#patternTooltipMergesEqualInputsWithCoordinates` 锁分组与坐标，运行时排版待实机。
- ⬜ 确认**样板输出槽只收增广合成样板**：AE2 原版合成/处理/锻造/切石样板放不进编码终端的样板输出槽（放入无反应），增广样板与取出正常；旧存档里已在该槽的 AE2 样板仍能取出。
  - 判据：试放 4 种 AE2 原版样板均被拒；编写样板流程不受影响。

## 二、未开始

### 增广样板编码终端 — P1

- ✅ **自有菜单类型与界面**（2026-11-12）：`AugmentedPatternEncodingTermMenu.TYPE` + `AugmentedPatternEncodingTermScreen`，界面文档 `screens/ae2_crafting_integration/pattern_encoding_terminal.json` 只 include AE2 的 `terminals/terminal.json`。
- ✅ **9x9 编码台与新外框**（2026-11-12）：菜单改为直接继承 `MEStorageMenu`，81 个 `FakeSlot` 复用 `PatternEncodingLogic#getEncodedInputInv()`（自带存档）；外框按用户重绘稿改成 512×512、五片切片拼接，9x9 编码槽 / 标志工作方块槽 / 产物槽 / 空白样板槽 / 样板输出槽 / 编写样板按钮全部落在贴图框内，`TerminalStyleDocumentTest` 逐点取像素锁定。
- ✅ **编写样板入口**（2026-11-12）：按钮走客户端动作 `encode_augmented_pattern`，把 81 格 + 产物写成 `增广合成样板`；产物由「9x9 配方匹配」一节的判定器链给出。
- ✅ **9x9 配方匹配**（2026-11-12）：产物预览与编码改走 `common/logic/AugmentedRecipeMatcher` 的判定器链。原版合成台按非空格包围盒裁剪后查配方（`ShapedRecipePattern` 要求输入宽高与配方宽高**完全相等**，9×9 全图直接喂必然一次也不命中）；合成拓展与无尽按配方自身 tier **等值**匹配（`TableCraftingInput#of` / `TierInput#of` 内部同样先裁包围盒），由 `integration/ModIntegrations` 挡在 `isLoaded` 之后注册。`ExtendedCraftingMatchingGameTest` 从真实配方表随机挑一条 >3×3 的有序配方原样还原，`RecipeMatchingGameTest` 锁偏移摆放与「>3×3 让位」。
- ✅ **无尽重生工作台编码集成**（2026-11-12）：标志物槽决定工作台档位——幽匿 5×5 / 下界 7×7 / 末地 9×9 / 终极 11×11 对应 tier 1..4，表格配方按 tier **等值**匹配；无尽锻造走十字五格（模板在左、基底居中、添加物在右/上/下）。内容先取包围盒再**居中**铺进目标台，编码进样板时同样居中，装不进这张台就不出产物。`AvaritiaMatchingGameTest` 在真实配方表上锁「同一网格换标志物就换结果」与十字布局。
- ✅ **JEI 配方转移按钮**（2026-11-12）：JEI 的转移处理器按「容器类 + 配方类别」**精确查表**（`ImmutableTable`，不是 `isAssignableFrom`），AE2 的 `EncodePatternTransferHandler` 绑在 `PatternEncodingTermMenu.class` 上，本终端继承 `MEStorageMenu`，两条查表路径都落空，所以按钮不画。新增 `client/integration/jei/` 三件套：按类别 UID 反查已注册类别后逐条注册，把输入按行优先居中铺进 81 格，按配方自身 tier 自动填标志工作方块，经 `InventoryActionPacket(SET_FILTER)` 落到 `ConfigInventory`（JEI 默认的客户端 `Slot#set` 对 `FakeSlot` 无效）。覆盖原版合成、合成拓展四档台、无尽重生四档台与无尽锻造十字五格；`GridPlacementTest` 锁具体落点，`RecipeGridCategoriesTest` 锁类别表形状。
- ✅ **标志工作方块槽持久化**（2026-11-12）：这一格不在 `PatternEncodingLogic` 的读写范围内，改由宿主实现 `common/menu/WorkstationHost` 保管——线缆面板写部件 NBT 的 `workstation` 键（拆部件时连同工作台掉落、`clearContent` 一并清空），无线终端写进物品 `CUSTOM_DATA` 的同一根 compound。菜单只认接口，`host instanceof` 失败时退回临时库存。`WorkstationPersistenceGameTest` 两边各验一次「重读 NBT / 换一个宿主后仍在」，并断言部件 NBT 里确实出现了 `workstation` 键。
- ⬜ 给**标志工作方块槽加提示**：现在放进任何物品都只是「不出产物」（`FakeSlot` 不过滤），玩家看不出这个槽该放工作台。
  - 判据：槽位有 tooltip 或空槽底图标注，说明「放工作台以选择配方档位」。
- ⬜ 复核**窗口高度**：界面 347 高（3 行物品列表时），1080p 下 GUI 缩放 3 只剩 360 虚拟像素、缩放 4 只剩 270，会超出屏幕；同时行数随窗口高度变化，实机要看重复行与 9x9 网格的衔接是否自然。
  - 判据：GUI 缩放 2/3/4 下都能完整显示，底部部件不错位。

### 工程 — P2

- ⬜ 迁移**发布工作流**：从参考项目复制 `release.yml` 并填新项目 id。
- ⬜ 补齐**客户端实机验证**：启动 `runClient` 确认三件套外观与创造页。
  - 判据：装配室界面能打开、9x9 槽位可放物品，工作台依赖无加载报错；背景与右侧浮层贴合贴图，进度条不偏移；线缆能连上装配室，漏斗可从产物槽抽取且塞不进 9x9 网格；右侧升级面板显示 4 个槽、只吃加速卡；左侧指南按钮能打开本模组页面；面板终端贴到线缆后右键能开样板编码界面，无线终端充能并与接入点链接后能开同一界面，编码台内容在关窗重开后还在。
  - 编码终端按 206×347 的新布局重排后**还没实机看过**：五片拼接、右侧挂带的四个槽、按钮落点都只由 `TerminalStyleDocumentTest` 与 `tools/layout/preview.py` 的像素比对保证，运行时效果待确认。AE2 的 `widgets.upgrades` 已挪出屏幕，改由 `scrollingUpgrades: right 13`（x=193）与 `upgradeScrollbar: right -6`（x=212）接管，这两处同样待实机确认。
  - JEI 侧**注册已验、点击未验**：`runClient` 日志里有 `Registering recipes transfer handlers: ae2_crafting_integration:jei`，说明插件被 JEI 扫到且注册无异常；但「+」按钮是否出现在各页签、点下去 81 格是否按预期居中铺开、标志工作方块槽是否自动落到正确档位、上一轮的残留格是否被清干净，仍需进世界点一遍。

## 三、暂缓

### 9x9 配方数据生成 — P3

- ⏸ 等两台机器的配方类型定稿后再接 `runData`。

## 归档索引

- 已发布内容与实现细节：`CHANGELOG.md`、git 历史、`ARCHITECTURE.md`。
- 发版流程与踩坑：`RELEASE.md`。
- 更新日志体例：`CHANGELOG_STYLE.md`。
- 本表写法：`TODO_STYLE.md`。
