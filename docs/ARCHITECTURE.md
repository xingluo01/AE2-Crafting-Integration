# 架构

当前已落地基线三件套与 9x9 装配室的容器/界面骨架，方块实体已接进 AE2 的基类与能力面（网格节点可被发现、物品能力可被管道读写），并接上了 4 格升级槽与 AE2 标准的加速卡；9x9 配方匹配（含合成拓展与无尽的工作台）与样板编码终端已落地，装配室按样板执行与 ME 合成交互尚未实现。改代码前先读完本页。

## 基线

- Minecraft 1.21.1 / NeoForge 21.1.241 / AE2 19.2.19 / Java 21。
- 权威版本来源：`gradle.properties` 与 `gradle/libs.versions.toml`。
- 构建沿用 `AE2-Pattern-Disk` 的 NeoForge ModDevGradle 体系，但只保留通用部分。

## 源集

| 源集 | 目录 | 职责 |
|---|---|---|
| `main` | `src/main/java` | 通用与服务端代码；不能引用客户端类 |
| `client` | `src/client/java` | 仅客户端代码：装配室界面与屏幕注册 |
| `test` | `src/test/java` | JUnit 单测与 GameTest；不进发布 jar |

`build.gradle` 刻意把 `client` 与 `main` 分开，让公共代码无法误引客户端类——这是编译期防线，不是洁癖。

## 注册链路

```
AE2CraftingIntegration（@Mod 入口）
├── ModSlotSemantics.init()          → 先注册自定义槽位语义（AEjson 按名字查）
├── ModBlocks.BLOCKS                 → molecular_assembler 方块（MolecularAssemblerBlock）
├── ModItems.ITEMS                   → 基线物品 + 增广合成样板（EncodedPatternItem）
├── ModDataComponents.DATA_COMPONENTS → augmented_pattern 组件
├── ModCreativeTabs.TABS             → 创造页 main
├── ModBlockEntities.BLOCK_ENTITIES  → 装配室方块实体（注册时回填 setBlockEntity）
├── ModMenuTypes.MENUS               → 装配室菜单
├── modBus.addListener(ModCapabilities::register) → 网格节点宿主 + 物品能力
├── modBus.addListener(commonSetup) → FMLCommonSetupEvent 里调 ModUpgrades.register() / ModTerminals.register()
└─ client: AE2CraftingClient 把菜单绑定到 MolecularAssemblerScreen，并注册 GuideME 指南书
```

- 注册统一用 `DeferredRegister`；菜单实体由 AE2 `MenuTypeBuilder.buildUnregistered` 建好，`ModMenuTypes` 只负责挂到注册表。
- 编码终端有自有菜单类型 `AugmentedPatternEncodingTermMenu.TYPE`（直接继承 `MEStorageMenu`，见「编码终端」一节）；客户端按 `menu.getType()` 查 `MenuScreens`，所以类型必须自有。
- 基线 id 沿用 AE2 原名（`molecular_assembler` / `pattern_encoding_terminal` / `wireless_terminal`），处于本模组自己的 namespace；功能落地后如果要改名，连同模型/语言键一起动。
- 装配室方块是 `MolecularAssemblerBlock extends AEBaseEntityBlock<MolecularAssemblerBlockEntity>`，方块实体是 `MolecularAssemblerBlockEntity extends AENetworkedInvBlockEntity`；右键经 `MenuOpener` + `MenuLocators.forBlockEntity` 打开菜单。两个编码终端的接线见下节。

## 方块与方块实体的 AE2 适配

AE2 的机器不是「实现个接口就自动接上」：基类负责建方块实体与网格节点，能力面还得第三方自己再登记一遍。三层各有一条静默失效路径——都不报错，只在实机表现为机器不工作。

| 层 | 本模组实现 | 换成裸 MC 类型的后果 |
|---|---|---|
| 方块 | `MolecularAssemblerBlock extends AEBaseEntityBlock<T>` | `newBlockEntity` 拿到 null、破坏不掉内部库存、记忆卡失效 |
| 方块实体 | `MolecularAssemblerBlockEntity extends AENetworkedInvBlockEntity` | 无网格节点、无 91 格存读与掉落、无合并标脏 |
| 能力登记 | `ModCapabilities.register`（模组总线事件） | 线缆连不上、管道看不到库存 |

四条硬性前提：

1. **`setBlockEntity` 必须调用**（在 `ModBlockEntities` 的注册 lambda 内）。`AEBaseEntityBlock#newBlockEntity` 不做 `new`，而是 `blockEntityType.create(...)`；不回填类型就是空壳方块。
2. **`AEBaseBlockEntity.registerBlockEntityItem(type, item)`**：`getName()`、记忆卡导出与网格节点外观都从「类型 → 代表物品」表取图标，漏登记显示成空气。
3. **存读覆写 `loadTag`**：`AEBaseBlockEntity#loadAdditional` 是 `final`，只负责先解客户端视觉数据再转调 `loadTag`；覆写 `loadAdditional` 直接编译不过。
4. **能力事件挂在模组总线**：`RegisterCapabilitiesEvent` 是 `IModBusEvent`，`@EventBusSubscriber` 默认 `GAME` 总线收不到；本仓库一律在入口类 `modBus.addListener(ModCapabilities::register)`。

当前登记的能力：

- `AECapabilities.IN_WORLD_GRID_NODE_HOST` → 方块实体自身（`IGridConnectedBlockEntity extends IInWorldGridNodeHost`）。
- `Capabilities.ItemHandler.BLOCK` → `getExposedInventory()`：催化剂栏 `81..89` + 产物槽 `90`，9x9 网格不对自动化开放；产物槽由 `AssemblerInventory#isItemValid` 保证只出不进。
- **AE2 只会给 `AEBlockEntities` 自己的类型批量登记能力**（见 `InitCapabilityProviders`），新增方块实体时必须同步补 `ModCapabilities`，否则上面两条一条都不会生效。

尚未适配，按落地顺序：`ICraftingMachine`（样板供应器推配方）、`IGridTickable`（进度推进与节点休眠）、`AECapabilities.GENERIC_INTERNAL_INV`（通用仓）。

方块掉落由 `data/ae2_crafting_integration/loot_table/blocks/molecular_assembler.json` 提供，内部 91 格由 `AEBaseEntityBlock#onRemove` → `addAdditionalDrops`（继承自 `AEBaseInvBlockEntity`）掉落。**缺战利品表时方块破坏后什么都不掉**；1.21 的目录名是单数 `loot_table`。

## 升级槽与加速卡

装配室实现 `IUpgradeableObject`，升级卡走 AE2 标准链路，不自己写槽位容器：

| 环节 | 实现 | 漏掉的后果 |
|---|---|---|
| 库存 | `UpgradeInventories.forMachine(ModBlocks.MOLECULAR_ASSEMBLER.get(), 4, this::saveChanges)` | 卡片塞不进去；变更不落盘 |
| 兼容登记 | `ModUpgrades.register()` → `Upgrades.add(AEItems.SPEED_CARD, 方块, 4)` | `getMaxInstallable` 返回 0，界面把空槽标成不兼容 |
| 菜单槽位 | `MolecularAssemblerMenu extends UpgradeableMenu` 的 `setupUpgrades()` | 界面有面板但没槽 |
| 界面面板 | `MolecularAssemblerScreen extends UpgradeableScreen` 的 `UpgradesPanel` | 槽位坐标停在 (0,0) |

**登记时机**：`Upgrades.add` 必须在 `FMLCommonSetupEvent` 的 `enqueueWork` 里调（AE2 的 `InitUpgrades.init()` 也在那里）。模组构造期物品还没绑定，`AEItems.SPEED_CARD.asItem()` 拿不到实例。

**加速档位**在 `AssemblerAcceleration`，两列数值逐项来自 AE2 本体分子装配室的 `speed × acceleratorTax`：

| 加速卡 | 每 tick 进度 | 完成一次合成 |
|---|---|---|
| 0 | 10 | 10 tick |
| 1 | 16.9 | 6 tick |
| 2 | 28.9 | 4 tick |
| 3 | 40 | 3 tick |
| 4 | 250 | **每 tick 一次** |

本体把「断档式跃升」放在第 5 张（50 × 5.0），本模组只开 4 槽，于是把它挪到第 4 张。改 `SPEED`/`TAX` 数组会直接打挂 `AssemblerAccelerationTest` 的四条断言——那是玩法承诺，不是内部细节。

升级库存**不参与** `getInternalInventory()`，必须在 `saveAdditional`/`loadTag` 里单独 `writeToNBT`/`readFromNBT("upgrades")`；漏了只是重启后卡片消失，不报错。

## 菜单与同步

- `MolecularAssemblerMenu extends UpgradeableMenu<MolecularAssemblerBlockEntity> implements IProgressProvider`；槽位只按 `SlotSemantic` 注册，坐标不进 Java 代码。
- 槽位用 AE2 的 `AppEngSlot`（吃 `InternalInventory`，写入走 `setItemDirect` → `saveChangedInventory` → `saveChanges` 的合并标脏链路）；换成原版 `Slot` + `Container` 适配器会丢掉这条通知，物品能放进去但不落盘。
- `AssemblerSlots` 只放数量与索引契约：81 网格（`0..80`）+ 9 催化剂（`81..89`）+ 1 产物（`90`），共 91 格。
- **`@GuiSync(10)` = `progress`，`@GuiSync(11)` = `maxProgress`**。编号必须避开 `UpgradeableMenu` 已占用的 0 / 1 / 5 / 6（`rsMode` / `fzMode` / `cMode` / `schedulingMode`）；编号是客户端/服务端协议，不可复用、不可改。
- `setupInventorySlots()` 只加机器自己的 91 格；玩家背包与升级槽由 `UpgradeableMenu` 的构造统一加，**子类再调 `createPlayerInventorySlots` 会直接抛「already created」**。
- 进度值由 `MolecularAssemblerBlockEntity` 持久化，目前恒 0；配方逻辑接入后按 `getProgressPerTick()` 推进，界面的进度条无需再改。
- `quickMoveStack` 与 `stillValid` 直接继承 `AEBaseMenu` 的实现。

## 编码终端

两个终端共用**自有菜单类型** `AugmentedPatternEncodingTermMenu.TYPE`、**自有界面** `AugmentedPatternEncodingTermScreen` 与**自有界面文档** `screens/ae2_crafting_integration/pattern_encoding_terminal.json`。菜单继承 AE2 的 `MEStorageMenu`（保住网络库存、搜索、排序、滚动列表），编码台换成自己的 9×9。

- **面板形态**：`pattern_encoding_terminal` 是 `PartItem<AugmentedPatternEncodingTerminalPart>`。`PartItem` 自带 `useOn` 放置与 `createPart`，部件类 `AugmentedPatternEncodingTerminalPart extends PatternEncodingTerminalPart` 继承编码逻辑与部件模型（`ae2:part/pattern_encoding_terminal_on/off`），只重写 `getMenuType(Player)` 返回自有类型。换成普通 `Item` 就只是块贴图，放不到线缆上。
- **无线形态**：`wireless_terminal` 是 `WirelessAugmentedPatternTerminalItem extends WirelessTerminalItem`，电量取 `AEConfig#getWirelessTerminalBattery`（与本体无线终端同配置，充能器/耗尽逻辑一致），`getMenuType()` 同样返回自有类型。
- **无线宿主**：AE2 只有 `WirelessCraftingTerminalMenuHost`，没有样板编码版本，所以 `WirelessAugmentedPatternTerminalHost` 把两半拼起来——`WirelessTerminalMenuHost` 提供网格连接/能量/终端设置，`PatternEncodingLogic` 提供编码台。它同时实现 `IPatternTerminalMenuHost`（菜单构造时按这个接口取 `getLogic()`）。
- **编码台存档**：物品没有天然的存档位，逻辑序列化进物品的 `CUSTOM_DATA` 组件，根键 `augmented_pattern_encoding`（改键等于让现存样板失效）。写入只发生在服务端：客户端那边格子变动由菜单包驱动，回写只会用本地状态覆盖同步值。读档期间 `PatternEncodingLogic` 会回调 `markForSave()`，此时 `logic` 字段还没赋值/正在装载，必须用 `loading` + null 判断挡住，否则会把半截数据写回物品。

### 为什么不再继承 AE2 的编码菜单

`PatternEncodingTermMenu` 绑死了 3×3 合成台、处理样板、锻造台与切石机四套模式（9 + 81 + 27 个 `FakeSlot` 加专用输入格），而本终端是一整块 9×9 编码区。继续继承会得到一堆用不上的格子，并且 `encode()` 永远读那 9 个隐藏的 3×3 格。现在直接继承 `MEStorageMenu`，只加自己的槽位：

- **9×9 编码格**（语义 `ae2_crafting_integration:augmented_grid`）：81 个 `FakeSlot`，库存直接用 `PatternEncodingLogic#getEncodedInputInv()`——它本身就是 81 格 `ConfigInventory`（AE2 处理样板的上限就是 9×9），并且已经挂在两种宿主的存档链上，关窗重开、跨维度都还在。自己 new 库存就得给部件和无线物品各写一套序列化，没必要。该库存的变更监听会把每格数量压回 1（`fixCraftingRecipes`），正好符合「一格一个原料」的合成语义。
- **标志工作方块格**（`ae2_crafting_integration:workstation`）：决定这套配方属于哪张工作台。无尽重生据此选 tier 与网格尺寸（幽匿 5×5 / 下界 7×7 / 末地 9×9 / 终极 11×11 / 无尽锻造台走十字五格），见「9x9 配方匹配」的「无尽重生」一节。这一格**不在** `PatternEncodingLogic` 的读写范围内（AE2 只存模式、输入输出与样板槽），所以由宿主实现 `common/menu/WorkstationHost` 自己保管：线缆面板写部件 NBT 的 `workstation` 键（拆部件时连同工作台一起掉落、`clearContent` 一并清空），无线终端写进物品 `CUSTOM_DATA` 的同一根 compound。菜单只认接口，两种形态共用一套槽位与同步逻辑；宿主不是本模组实现时退回临时库存，只影响存档。
- **产物槽**：`PreviewSlot`，只读。网格或标志工作方块变化时交给 `AugmentedRecipeMatcher`（原版合成台按包围盒匹配，合成拓展 / 无尽的表格台与无尽锻造由 `integration/*` 接手），有结果就画出来；判定细节见「9x9 配方匹配」。**不设 `Icon.BACKGROUND_PRIMARY_OUTPUT`**——那是 AE2 用来给自己槽位底图补一块内嵌凹槽的图标，本模组的贴图已经把产物槽画好了，叠上去会在正中多出一块灰底。
- **空白/已编码样板格**：`RestrictedInputSlot` 接 `PatternEncodingLogic` 的 `blankPatternInv` / `encodedPatternInv`，与 AE2 同一套存档与过滤。
- 编写样板按钮走客户端动作 `encode_augmented_pattern`：服务端把 81 格 + 预览产物写成 `增广合成样板`（`AugmentedPattern.encode`）。已编码槽非空时拒绝执行，不覆盖玩家的样板。

### 界面文档：切片与 bottom 定位

`StyleManager` 只从 `assets/ae2/screens/` 读文档（`AppEng.makeId` 固定 namespace `ae2`），include 的解析是「当前文件目录 + include」再对 `..` 做 URI 归一化。本终端只 include `../terminals/terminal.json`（AE2 的搜索框、滚动条、玩家背包、工具箱与公共色板），**不再 include `encoding/*.json`**——那四份会把 3×3 与模式面板的坐标重新灌回来。

背景不是整图，而是 `MEStorageScreen#drawBG` 逐片纵向拼出来的五片：`header(195×17)` + 三行物品列表 `firstRow/row/lastRow(195×18)` + `bottom(206×276)`，合计 347 高。行数由窗口高度决定（`rows = (height - headerH - bottomH) / rowH`），所以：

- 行切片必须同高且都是物品行贴图，多出来的行重复画出来仍是一条条相同的槽；
- 所有固定部件（9×9 网格、产物槽、样板槽、按钮）在 JSON 里一律用 `bottom`（离底边的距离）定位，`imageHeight` 变了也跟着走；写 `top` 的话窗口一变高就会被重复行压在网格中间；
- 贴图是 **512×512** 画布，`Blitter` 的 `srcRect` 默认按 256 归一化 UV，所以五片都必须显式写 `textureWidth/Height: 512`。

这一版面板是 206 宽：主体仍是 195（x 0..194），右侧挂带凸出到 x=205，只存在于 `bottom` 那一段（y 84..250）。因此 `bottom` 必须写 206——`MEStorageScreen#getScreenWidth()` 取五片 `srcWidth` 的**最大值**当界面宽度，少一像素就把挂带右边那条描边切掉，多一像素则界面无谓变宽、`upgrades`（`right: 2`）跟着外移。挂带左边框（x=168）同时是 9×9 网格块的右边框：9 格按 18 步进要 162，而贴图只画 160（`9×16 + 8×2`），多出的 2px 是末格的可点区域，不画东西——`TerminalStyleDocumentTest` 直接断言「末格右缘 = 168」。

槽位原点一律取 **16×16 内容区的左上角**，不是外框原点：18×18 的框取 `+1`，24×24 的框取 `+4`（16×16 居中）。这不是估的——AE2 自己的编码终端把 `BLANK_PATTERN` 写在 `(147,86)`，而它 `guis/pattern.png` 里那个框画在 `(146,85)`，正好差 1；`CRAFTING_GRID` 的 `(15,93)` 同样落在格内首行。9×9 编码格因为格内自带 1px 顶线、又没有左边框，原点是 `(8,87)`（内容区就是 8..23 × 87..102）。

槽位落点与贴图槽框的对应关系由 `TerminalStyleDocumentTest` 逐点取像素锁定（槽位坐标代回贴图必须是 `(154,159,180)` / `(173,176,196)` 且 16×16 全不透明），改贴图不改坐标会直接红。换贴图时先用 `tools/layout/analyze.py` 量一遍——它从像素里读出有效内容尺寸、列表行数、网格原点与间距、挂带里每个方框，再按同一条规则换算成 `left/bottom` 与五片 `srcRect`，最后逐点校验 16×16 不透明；`tools/layout/preview.py` 按 JSON 把切片重拼回去并与原图做逐像素比对（当前 0/71482），同时把槽位框画上去供肉眼核对。

编写样板按钮在新贴图里没有画框（AE2 的编码终端也一样，按钮是用 `ActionItems.ENCODE` 的图标直接盖在底色上），落点取空白样板槽与样板输出槽之间的空档，间距照 AE2 自己的节奏（按钮顶离空白槽 5px、离输出槽 11px）。

## 9x9 配方匹配

编码终端的产物预览与（以后的）装配室共用 `common/logic/AugmentedRecipeMatcher`：判定器排成一条链，**第一个非 `null` 的结果获胜**。原版合成台是内置的第一条，邻居的工作台由 `integration/*` 追加。

| 类 | 位置 | 职责 |
|---|---|---|
| `GridBounds` | `common/logic/` | record：非空格的最小外接矩形 + 裁出来 + **居中铺进目标网格** |
| `GridMatcher` | `common/logic/` | 接口：整张网格 + 尺寸（+ 可选标志工作方块）→ `Match(output, recipeId)` 或 `null` |
| `AugmentedRecipeMatcher` | `common/logic/` | 判定器链、`registerIntegration`、`GenericStack` 快照 → 物品表 |
| `*TableMatcher` | `integration/<邻居>/` | 合成拓展 / 无尽的表格配方 |
| `AvaritiaWorkstations` | `integration/avaritia/` | 标志工作方块 → 网格边长与 tier（`ModBlocks` 比较） |
| `AvaritiaSmithingMatcher` | `integration/avaritia/` | 无尽锻造的十字五格 |
| `ModIntegrations` | `integration/` | 唯一一处 `ModList.isLoaded` 分流 |

四条硬性约定：

1. **必须先裁包围盒**。`ShapedRecipePattern#matches` 要求 `input.width() == pattern.width()` 且高度同样**完全相等**，不是「放得下就匹配」。所以把整张 9×9 直接喂给 `getRecipeFor` 一次也命中不了；原版工作台走的是同一条路（`CraftingInput.ofPositioned`）。
2. **tier 由标志物决定，不是从网格猜**。合成拓展的 `ShapedTableRecipe#matches` 与无尽的 `ShapedTableCraftingRecipe#matches` 都写成 `recipe.tier != 0 && recipe.tier != input.tier() → false`，是**等值**不是「不低于」。所以 `GridMatcher` 有一个带 `ItemStack station`（标志工作方块）的五参重载，默认实现忽略它并转给四参版本——原版与合成拓展零改动。没放标志物时，需要 tier 的判定器返回 `null`，不猜档位。
3. **`GridBounds.of` 的空格判定是参数，不是写死的 `ItemStack::isEmpty`**。纯 JUnit 里 `ItemStack` 连静态初始化都会抛 `ExceptionInInitializerError`（`ItemStack.EMPTY` 依赖物品注册表），写死一个单测都跑不起来。`ofItems` 只是套了一层；测试用字符串列表验裁法。`null` 元素一律按空格处理（`ConfigInventory#getStack` 对空格返回的就是 `null`）。
4. **预览与编码各算一次**。`encode()` 重跑匹配而不复用每 tick 的预览缓存：按钮点击与缓存之间可能隔着一次未广播的槽位改动。多算一次匹配远比编出错样板便宜。

### 无尽重生（Re-Avaritia）

`integration/avaritia` 有两类配方，靠标志物槽里的物品分流（`AvaritiaWorkstations`）：

| 标志物 | 网格 | tier | 判定器 |
|---|---|---|---|
| 幽匿合成台 | 5×5 | 1 | `AvaritiaTableMatcher` |
| 下界合成台 | 7×7 | 2 | 同上 |
| 末地合成台 | 9×9 | 3 | 同上 |
| 终极合成台 | 11×11 | 4 | 同上 |
| 无尽锻造台 | 十字五格 | — | `AvaritiaSmithingMatcher` |

**居中填充**：编码台固定 9×9，工作台是 5/7/9/11。匹配前先取非空格包围盒，再用 `GridBounds#centerIn` 以目标台的中心对齐铺进去；包围盒比台大就直接不匹配（`centerIn` 带尺寸检查，不会悄悄截掉原料）。玩家把图案摆在编码台哪个角落都不影响结果。编码进样板时同样居中，装配室以后按目标台尺寸再居中铺开。

**无尽锻造不是网格**：`ExtremeSmithingRecipeInput` 是五格（模板、基底、三个添加物），`matches` 要求五格全部非空。模组自己把五格摆成十字（`ExtremeSmithingMenu#createInputSlotDefinitions`）：

```
      [添加]
[模板] [基底] [添加]
      [添加]
```

编码端沿用这个布局：先把包围盒居中成 3×3 概念区域，再按十字读取；四角必须为空，否则拒绝——宁可不出产物，也不要把一张 3×3 表格配方误编成锻造样板。

**tier 为什么是 `(size - 3) / 2` 而不是模组自己的 `size / 2`**：数据包里的 `tier` 取值是 1..4（内置 77 条表格配方里 1/2/3/4 分别 5/10/9/53 条），而 `ShapedTableCraftingRecipe#getTier()` 在没写 tier 时按 pattern 尺寸推断成 1..4（宽高都 `<4` → 1、`<6` → 2、`<8` → 3、否则 4）。两处口径都指向「第 n 档台做第 n 档配方」，每档台比该档配方的最大尺寸大 2，正好留一圈边。模组 `ModCraftContainer#asCraftInput()` 用的是 `floorDiv(width, 2)`，会让四张台整体偏移一位（终极台算出 tier 5，而数据包里没有 tier 5 的配方），这里刻意不照抄。

匹配链的验证分两层：`GridBoundsTest` / `AugmentedRecipeMatcherTest` 在纯 JUnit 里锁裁法、居中填充与链的顺序、短路；`RecipeMatchingGameTest` 在真实服务器世界上锁「偏移摆放仍命中原版配方」「包围盒 >3×3 时原版让位」；`ExtendedCraftingMatchingGameTest` 从真实配方表里随机挑一条 >3×3 的有序表格配方，按 pattern 铺回 9×9 网格并要求还原出同一个产物；`AvaritiaMatchingGameTest` 验「同一网格换标志物就换结果」（tier=4 的配方只被终极台认出来、幽匿台拒绝）、「没放标志物不出产物」与无尽锻造的十字布局（四角有杂物要拒绝）。

## JEI 配方转移

JEI 配方界面右下角那个「+」按钮（把配方原料搬进打开着的容器）在本终端上原本不出现。原因不在权限、不在界面，而在**容器类没登记**。

JEI 的 `RecipeTransferManager` 把处理器存成：

```java
ImmutableTable<Class<? extends AbstractContainerMenu>, RecipeType<?>, IRecipeTransferHandler<?, ?>>
```

查表用的是 `container.getClass()`，**精确相等**（`get(containerClass, recipeType)`），不是 `isAssignableFrom`；查到之后还要 `handler.getMenuType().equals(container.getType())`。AE2 的 JEI 集成（独立模组 `ae2-jei-integration`）把 `EncodePatternTransferHandler` 注册在 `PatternEncodingTermMenu.class` 上——本终端继承的是 `MEStorageMenu`，两条查表路径（该类 + universal）都落空，JEI 于是根本不画按钮。

所以不能复用 AE2 的处理器，也不能简单地改成 universal：universal 只是把这个处理器挂到「万能配方类型」这一格上，容器类仍然是 `PatternEncodingTermMenu`，而且 universal 会对**所有**类别显示按钮（熔炉、酿造台也在内），本终端没有处理样板模式，那些按钮点了也没有意义。

实现放在 client 源集的 `client/integration/jei/`：

| 类 | 职责 |
|---|---|
| `AugmentedPatternTerminalJeiPlugin` | `@JeiPlugin` 入口，遍历类别表逐条注册 |
| `AugmentedEncodeTransferHandler` | 一个类别一个实例：铺格、挑变体、发包 |
| `JeiTransferStations` | 配方 → 该放哪张工作台（模组引用藏在 `isLoaded` 守卫后面） |
| `RecipeGridCategories`（main） | 白名单：类别 UID、边长、是否十字、标志物策略 |
| `GridPlacement`（main） | 纯几何：输入槽序号 → 81 格 |

四条关键决定：

1. **按类别注册，而不是一个 universal**。注册用的类别对象不是从模组类里 `static` 取的，而是拿 `IJeiHelpers#getRecipeType(uid)` 反查「这一次真的注册了哪些类别」：邻居在配置里关掉某类配方、或者改了 UID，查不到就安静跳过，不会因为一个常量引用抛 `NoClassDefFoundError`，也不会把按钮挂到未注册的类别上。类别表在 `registerCategories` 阶段就已经填进 `JeiHelpers`（`RecipeCategoryRegistration#addRecipeCategories` 每次都调 `setRecipeCategories`），`registerRecipeTransferHandlers` 在其后，反查一定拿得到。
2. **转移走 AE2 自己的通道，不走 JEI 默认实现**。JEI 的默认转移是对客户端 `Slot#set`，而本终端的 81 格是 `FakeSlot`——客户端写只改本地副本，服务端毫不知情。这里按 AE2 `EncodingHelper` 的做法发 `InventoryActionPacket(InventoryAction.SET_FILTER, slotIndex, stack)`，服务端由 `AEBaseMenu#setFilter` 落到 `ConfigInventory` 上；`slotIndex` 必须是**菜单槽下标**（`AEBaseMenu` 拿它 `getSlot`），不是库存内部序号，所以菜单加了 `getGridSlotIndices()` / `getWorkstationSlotIndex()`。空格也要发：转移是覆盖，不发就留着上一次的残留（`ConfigMenuInventory#isItemValid` 对空栈返回 true，清得掉）。
3. **边长写死，不从槽数开方**。两家的无形状配方只给实际用到的格子建槽（`if (index < inputs.size())`），7 个材料就是 7 个槽，开方开不出边长。边长属于类别布局，与具体配方无关。
4. **标志工作方块按配方自己的 tier 填，不按玩家点的页签**。两家的配方查看器都把「可升级配方」同时列进更高档的类别（过滤条件是 `tier >= recipe.getTier()`），但运行时 `matches` 判的是**等值**档位；按页签推档会在终极台页签里点一个幽匿级配方时编不出产物。配方返回 0（不限档）时才退回按边长推断（`(side - 1) / 2`，两家的 3/5/7/9 都对应 1..4）。原版合成不动这个槽——玩家原来放什么就还是什么。

类别与布局（**边长是 JEI 画出来的格子数，不是台面尺寸**）：

| 类别 UID | 边长 | 标志物 |
|---|---|---|
| `minecraft:crafting` | 3 | 不动 |
| `extendedcrafting:basic/advanced/elite/ultimate_crafting` | 3 / 5 / 7 / 9 | 按配方 tier 放对应档台 |
| `avaritia:sculk/nether/end/extreme_craft` | 3 / 5 / 7 / 9 | 同上 |
| `avaritia:extreme_smithing` | 十字五格 | 无尽锻造台 |

无尽重生的类别比台面小一圈（类别 3/5/7/9 对应台面 5/7/9/11，每档台留一圈边），所以 9×9 编码台能完整摆下它所有配方——包括 11×11 终极台那一档。合成拓展的类别与台面同尺寸。两家的有形状类别都已经在 JEI 里自己居中（`(网格 - 图案) / 2`），这里再居中一次，落点和玩家在 JEI 里看到的一致。

十字五格的输入顺序（模板、基底、上、右、下）与 `ExtremeSmithingCategory#setRecipe` 的 `addSlot` 顺序逐格对应，也与 `AvaritiaSmithingMatcher` 的读取顺序一致——三处必须一起改。

挑哪一格的哪个变体：先挑 ME 网络里真有库存的（编码出的样板才指向玩家拿得到的东西），其次挑 JEI 里可见的，最后退回第一个候选。不看数量，只看有没有。

验证：`GridPlacementTest` 锁 3×3/7×7/9×9 与十字的**具体落点**（错一位就是整体平移），`RecipeGridCategoriesTest` 锁「每类边长 ≤ 9」「十字只有无尽锻造一类」与 UID 反查。插件是否被扫到看客户端日志里的 `Registering recipes transfer handlers: ae2_crafting_integration:jei` 那一行——它在就说明注册链路通了，与它同段出现的异常才是要查的；按钮点下去的实际效果见 `TODO.md` 的「客户端实机验证」。

## 资源

- 全部模型、纹理在 `src/main/resources/assets/ae2_crafting_integration/`，除 `textures/guis/molecular_assembler.png` 已按 9x9 布局重绘、`textures/item/augmented_crafting_pattern.png` 由 AE2 `crafting_pattern.png` 改色加角标生成外，其余物理复制自 AE2 1.21.1 源码；`textures/guis/pattern.png`（编码终端外框）已按 9×9 布局重绘成 512×512，属于派生资源，仍记在 `ASSET_LICENSES.md`。
- 指南页面在 `assets/ae2_crafting_integration/ae2guide/`（Markdown，GuideME 格式），不参与模型/纹理的自包含约定。
- **JSON 里不得出现 `ae2:` 引用**：parent 模型链（`item/display_base` → `item/part_base`、`part/display_off`）与纹理已一并复制并改成本 namespace。替换外观直接覆盖同名 PNG。
- **例外**：AE2 的 `StyleManager` 只从 `assets/ae2/screens/` 读布局文档（`AppEng.makeId` 固定 namespace `ae2`），所以本模组的 AEjson 都放在 `src/main/resources/assets/ae2/screens/ae2_crafting_integration/`：`molecular_assembler.json`（装配室，自写）与 `pattern_encoding_terminal.json`（编码终端，只 include AE2 `terminals/terminal.json`）。两份文档里的 `texture` 都指向本模组贴图，界面外观只由本模组的 PNG 决定。
- 授权明细见 `ASSET_LICENSES.md`。

## 装配室 GUI 布局（AEjson）

坐标全部在 `molecular_assembler.json`，改布局只动那份 JSON。`textures/guis/molecular_assembler.png` 的画布是 **512×512**，而 AE2 `Blitter` 的 `srcRect` 默认按 256×256 归一化 UV，所以 `background` 与 `images.*` 每一处都必须显式写 `"textureWidth": 512, "textureHeight": 512`；漏写会让 UV 变成 `x/256` 而非 `x/512`，界面只能截到贴图左上一块并整体错位。

贴图内容分两块：主面板 `0..175 × 0..313`（`176×314`），以及右挂浮层 `176..209 × 134..183`（`34×50`，含输出槽与进度条底槽）。`background.srcRect` 只截主面板 `[0,0,176,314]`，浮层由 `images.sidePanel` + `widgets.sidePanel` 单独画，避免把中间的透明区一起裁进界面。浮层上方的挂带 `176..209 × 0..133` 空着，升级面板按 AE 的 `right:2` 从 `x=174` 起压在主面板边框上（见下）。

整体纵坐标以贴图实测为准：2026-11-11 顶部留白从 26px 收到 15px 后，除标题与进度条 sprite 外，所有内容统一上移 11px，界面总高由 325 变 314。改贴图或改留白时，下面整张表要同批平移。

| 区域 | 贴图槽框 x, y, w, h | AEjson `slots` 键 | JSON 中的 left/top |
|---|---|---|---|
| 9x9 合成网格 | `7, 19, 162, 162` | `ae2_crafting_integration:grid` | `8, 20`，`BREAK_AFTER_9COLS` |
| 催化剂栏 | `7, 197, 162, 18` | `ae2_crafting_integration:catalyst` | `8, 198`，`HORIZONTAL` |
| 产物输出槽 | `177, 141, 26, 26` | `ae2_crafting_integration:output` | `182, 146`（框内居中 `+4`，再 `+1` 边框） |
| 玩家背包 | `7, 229, 162, 54` | `PLAYER_INVENTORY` | `8, 230`，`BREAK_AFTER_9COLS` |
| 玩家快捷栏 | `7, 287, 162, 18` | `PLAYER_HOTBAR` | `8, 288`，`HORIZONTAL` |
| 右挂浮层 | `176, 134, 34, 50` | `images.sidePanel` / `widgets.sidePanel` | `176, 134`（由 `drawBG` 补画） |
| 槽底 sprite | `8, 230, 16, 16` | `images.slotBackground` | 按各 `Slot.x/y` 平铺 |

- 槽框 `18×18` 含 `1px` 边框，真实可放物品区 `16×16`；Java 里 `Slot.x/y` 是图标左上角，所以比槽框左上角各 `+1`。
- 进度条：`images.progressBar` 取贴图 UV `[176, 0, 18, 6]`（贴图右上角那条 18×6 的 sprite，位置不随浮层移动），`widgets.progressBar` 落在 `181, 169`，方向 `HORIZONTAL`。底槽 `181, 169, 18, 6` 已画在浮层贴图里，不需要单独渲染。
- 升级面板：4 张加速卡竖排一列，`UpgradesPanel#getBounds` 算出的面板是 `28×82`（`10 + 4*18`）。位置**照 AE 原样写 `right: 2 / top: 0`**：`Position.resolve` 对 `right` 只算 `imageWidth - right`（**不减面板宽度**），所以左缘 = `176 - 2 = 174`，面板左侧 2px 压在主面板右边框上、其余 26px 挂在外面——AE2 各机器就是这个「半嵌在侧边」的落法。别改写成 `left`（早先的 `left:212` 会让面板整块飘在右边）；网格最右列到 `167` 为止，`174` 不会压槽位。面板占 `174..201 × 0..82`，底边到浮层 `y=134` 还有 52px。
- 9x9 网格在贴图里是空白底板，81 个槽底由 `MolecularAssemblerScreen#drawBG` 平铺 `images.slotBackground` 补画：16×16、顶 1px `(154,159,180)`、填充 `(173,176,196)`，与 AE2 `Icon.SLOT_BACKGROUND` 同规格。**不要**用 `graphics.fill` 画 18×18 四边框——那会盖掉底板的 `(203,204,212)`，相邻边框还会叠成 2px 深线。`AEBaseScreen#renderBg` 是 final，子类只能用 `drawBG`。
- 浮层与背景分开画，`imageWidth` 仍是 `176`（由 `background.srcRect` 决定），所以输出槽 `x=182` 会落在主面板右缘之外，这是贴图设计，不是坐标越界。
- 标题 `dialog_title` 固定 `left 8 / top 6`，与 AE2 全系一致；新留白下标题区 `y=1..16` 仍容得下 9px 字高。
- 布局口径：美术交付说明里的「玩家快捷栏 `7, 198`」与催化剂栏 `208` 重叠，按贴图实测边框应为 **`7, 298`**。

## 指南书与左侧按钮

左侧竖直工具栏由 `AEBaseScreen` 在构造时自动创建，并在 `addToLeftToolbar(new OpenGuideButton(...))` 里塞入指南按钮——**不需要自己加控件，也不需要改 AEjson 的 `widgets.verticalToolbar`**（`common.json` 已给出 `left: 3, top: 1`）。要做的只有两件事：

1. **注册指南书**：`AE2CraftingClient` 构造里 `Guide.builder(GUIDE_ID).folder("ae2guide")...build()`。
   必须在构造函数里建，不能等 `FMLClientSetupEvent`：GuideME 的静态指南表只在资源重载时读一次，注册晚了页面为空。AE2 自己也在 `AppEngClient` 构造里调 `createGuide()`。
2. **覆盖 `openHelp()`**：`AEBaseScreen` 默认打开的是 AE2 的 `ae2:guide`（`GuideItem.GUIDE_ID` 写死），不覆盖就会跳到 AE2 指南。

资源约定：页面放 `assets/ae2_crafting_integration/ae2guide/*.md`，页面 id 为 `ae2_crafting_integration:<相对路径去掉 .md>`。`folder("ae2guide")` 必须与目录名逐字一致。

## 增广合成样板

AE2 的合成样板只能表达 3×3：它把**配方 id** 存进组件，解码时回查 `CraftingRecipe`。9×9 没有对应的原版配方类型，所以 `AugmentedPattern` 改为**直接存网格**：

| 组成 | 位置 | 职责 |
|---|---|---|
| `AugmentedPatternData` | `common/pattern/` | record：`width` / `height` / 行优先 `grid`（空格为 `null`）/ `outputs` + Codec 与 StreamCodec |
| `AugmentedPattern` | `common/pattern/` | `IPatternDetails` 实现，每格展开成一个 `IInput`，倍率取该格数量 |
| `ModDataComponents` | `registry/` | `augmented_pattern` 组件，`persistent` + `networkSynchronized` 两条都要 |
| `ModItems.AUGMENTED_CRAFTING_PATTERN` | `registry/` | 由 `PatternDetailsHelper.encodedPatternItemBuilder` 生成的 `EncodedPatternItem` |

三条硬性约定：

1. **样板物品必须用 `PatternDetailsHelper.encodedPatternItemBuilder(...)` 生成**。AE2 的 `AEPatternDecoder` 只认 `EncodedPatternItem` 这个类型；自己写 `Item` 子类不会被 `isEncodedPattern` / `decodePattern` 看见，表现是「放进供应器不算样板」。
2. **组件必须带 `networkSynchronized`**。只写 `persistent` 时服务端能存，客户端读到 `null`，样板看起来是空的。
3. **`grid` 不能用 `List.copyOf` 收尾**：`List.copyOf` 拒绝 `null` 元素，而空格的语义就是 `null`。用 `Collections.unmodifiableList(new ArrayList<>(grid))`。

编码入口是 `AugmentedPattern.encode(...)`，编码终端的编写样板按钮会调它（见「编码终端」）；产物来源见「9x9 配方匹配」。装配室按样板执行尚未接入。

## 测试

- JUnit：`src/test/java`，跑 `./gradlew test`。槽位编号契约由 `AssemblerSlotsTest` 覆盖；AEjson 的画布尺寸与 `srcRect` 由 `StyleDocumentTest` 锁定，漏写 `textureWidth` 会直接失败；`BlockRegistrationTypeTest` 锁定方块/方块实体的 AE2 基类，`AssemblerInventoryTest` 锁定产物槽只出不进；`AssemblerAccelerationTest` 锁定五个档位的每 tick 进度与「4 卡每 tick 一次」，`AugmentedPatternDataTest` 锁定网格尺寸校验，`TerminalRegistrationTypeTest` 锁两个终端的继承链；`GridBoundsTest` / `AugmentedRecipeMatcherTest` 锁包围盒裁法、居中填充与判定器链的顺序、短路。
- 单测跑在纯 JUnit 环境，没有原版注册表 bootstrap：**不要实例化方块实体、库存或 Part**（会碰 `ItemStack.EMPTY` / 空 level），也不要 `new ItemStack(Items.X)`、不要碰 `ItemStack.EMPTY`——`ItemStack` 的类初始化直接抛 `ExceptionInInitializerError`。把纯规则抽成静态方法再测（`AssemblerInventory#isInsertable`、`GridBounds#of` 的空格判定参数就是这么来的）。`Bootstrap.bootStrap()` 在 NeoForge 下会 NPE，别试。
- GameTest：`src/test/java/io/github/lounode/ae2crafting/game/`，跑 `./gradlew runGameTestServer`；模板 `empty` 来自 `src/test/resources/data/ae2_crafting_integration/structure/empty.nbt`。`ModLoadGameTest#terminalsAreWiredToAe2` 在真实注册表上验两个终端的类型、菜单类型与无线链接登记；`RecipeMatchingGameTest` 验 9×9 网格的偏移摆放与「>3×3 让位」；`ExtendedCraftingMatchingGameTest` 用真实配方表验合成拓展的表格配方能原样还原；`WorkstationPersistenceGameTest` 两边各验一次标志工作方块落盘——面板用 `PartHelper.setPart` 挂到真实线缆上（**不能直接 `new`**：`AbstractTerminalPart#readFromNBT` 回填配置管理器时会回调 `saveChanges()`，未放置的部件在那里是 NPE），无线用换一个宿主模拟关窗重开。
- 客户端启动：`run_client.bat`（等价 `./gradlew runClient --no-configuration-cache`）。AEjson 能否被 AE2 正确解析、槽位是否贴合贴图，仍需实机看。

## 开发运行环境

`build.gradle` 的 `localRuntime` 是从参考项目分四轮整理的开发期模组，只影响 `runClient` / `runServer`，不进发布依赖。分组与版本纪律见 `AGENTS.md`「开发运行环境」。

## 后续结构（规划）

```
common/block/            通用分子装配室方块（已落地）
common/block/entity/     方块实体、91 格容器与加速档位表（已落地）
common/menu/             装配室菜单与槽位编号契约（已落地）
common/pattern/          增广合成样板的数据与解码（已落地）
client/gui/              装配室界面（已落地）
common/part/             面板终端（已落地）
common/logic/            9x9 配方匹配、包围盒与居中填充（已落地）
integration/<邻居>/      工作台/模组适配，按邻居隔离（合成拓展、无尽表格与无尽锻造已接）
```

## 红线

- **集成隔离**：`integration/<邻居>` 只写那个邻居的适配；邻居的类型名不得漏进总是加载的类，缺那个模组时会在加载阶段炸。
- **名字即协议**：客户端/服务端靠字符串动作名与 `@GuiSync(n)` 配对，不能改、不能被第二个字段复用；`ModSlotSemantics` 的注册名必须与 AEjson 的 `slots` 键逐字一致。
- **只用 AE2 公共 API**：不反射内部类当 API，不 mixin AE2 核心；新增例外必须有明确理由并记进注释。
- **文件规模**：一个类只装一件能一句话讲清的事，超过 800 行必须按职责拆（目标 ≤ 650 行）。
