# AGENTS.md

本仓库的**文档地图 + 动手前必读的硬约定**。先读本页再动手。

## 这是什么

AE2 附属 `ae2_crafting_integration`。命名按「后续更多集成」定，不绑死在某一台机器上。

- **通用分子装配室**：类 AE2 分子装配室，9x9 合成格，下方一排催化剂槽；至少支持合成拓展（Extended Crafting）与无尽重生（Avaritia 系）工作台。
- **ME 增广样板编码终端 / 无线增广样板编码终端**：类工作台编码，最大支持 9x9 格子。两形态都接自有菜单类型 `AugmentedPatternEncodingTermMenu.TYPE` 与自有界面（9x9 编码台已接，产物预览走判定器链，含无尽重生的 tier 分流与无尽锻造十字；标志工作方块由宿主按 `WorkstationHost` 落盘，关窗重开仍在）。

**基线：Minecraft 1.21.1 · NeoForge 21.1.241 · AE2 19.2.19 · Java 21**。权威来源是 `gradle/libs.versions.toml` 与 `gradle.properties`。

**框架沿用** `E:\GitHub\AE2-Pattern-Disk`：Gradle wrapper、NeoForge ModDev 构建体系、main/client/test 三源集、模板生成 `neoforge.mods.toml`、LGPL-3.0、CI 构建工作流。骨架只保留通用部分，旧模组的功能源码、集成依赖与本地 libs jar 没有带过来。

## 当前文档

| 文档 | 什么时候读 |
|---|---|
| `README.md` | 玩法自述（英文，GitHub 首页与商店页） |
| `AGENTS.md` | 本页：约定与文档地图 |
| `docs/ARCHITECTURE.md` | 改任何代码前：源集、注册链路、资源约定、红线 |
| `docs/ASSET_LICENSES.md` | 换纹理或新增资源前：AE2 来源映射与授权 |
| `docs/CHANGELOG_STYLE.md` | 改任一份更新日志前 |
| `docs/TODO_STYLE.md` | 改 `docs/TODO.md` 前 |
| `docs/TODO.md` | 想知道还剩什么事没做时（全仓唯一待办表） |
| `docs/RELEASE.md` | 发版前：发布工作流迁移清单与平台踩坑 |

发布工作流（`.github/workflows/release.yml`）仍待从参考项目迁入；当前 CI 只有 `build.yml`（push / PR 跑构建）。客户端启动用 `run_client.bat`，游戏内自动测试用 `./gradlew runGameTestServer`。

## 开发运行环境

`build.gradle` 的 `localRuntime` 是开发运行期模组清单，前 3 轮从 `AE2-Pattern-Disk` 多选复制、第 4 轮按作用域补齐，只影响 `runClient` / `runServer`，不进发布依赖：

- 第 1 轮：AE2WTLib 基座、JEI + AE2 JEI Integration、Jade / Modern UI / JECH / Embeddium、FTB Lib + FTB Ultimine。
- 第 2 轮：ExtendedAE + AdvancedAE + Glodium + GeckoLib、ExtendedAE Plus 族、Applied Flux + Polymorph、MEGA Cells + AE2 Omni Cells。
- 第 3 轮：闪电科技族（AE2LT / Thunderbolt Core / 封包供应器）、NEO ECO 族（ECO / 青春版 / LDLib2 / Bigger AE2）。
- 第 4 轮：Extended Crafting 7.0.9（`extendedcrafting`）、Re-Avaritia 1.4.2（`avaritia`），以及前者的强制前置 Cucumber Library 8.0.16（`cucumber`，需 ≥ 8.0.13）。参考项目没有这三项，坐标由作用域补齐；分包按 modId 命名，见集成红线。仍未选：Mekanism、神秘农业、AE2 Crystal Science。
- 刻意未复制：Rechiseled 族（与 JEI 19.56 有已知 mixin 冲突）、批处理 / 吞吐族（多方改同一条派发链路）。
- 仓库：FTB 系走 `maven.ftb.dev` 的 `-neoforge` 专用件（Modrinth 上没有 FTB 项目）；Architectury 走 `maven.architectury.dev`；JEI 走 BlameJared；其余沿用 Modrinth / CurseMaven / ModMaven。
- 版本纪律：FTB 那组按 `ftb-ultimine-neoforge` 2101.1.13 的 POM 依赖表锁定，NEO ECO 族四者地板咬合，升任一版本都要同批复核。

## 硬约定

- **语言**：注释、`docs/` 与更新日志一律中文；**对外英文文案（`README.md`）**、代码标识符与游戏内英文文案除外。
- **集成红线**：`integration/<邻居>` 只写那个邻居的适配；邻居的类型名不得漏进总是加载的类，缺那个模组时会在加载阶段炸。已落地的样例：`integration/ModIntegrations` 是唯一一处 `ModList.isLoaded` 分流，`integration/extendedcrafting` 与 `integration/avaritia` 各自实现 `GridMatcher`，由 `AugmentedRecipeMatcher#registerIntegration` 追加到匹配链上——它们的类名可以出现在 `ModIntegrations` 的常量池里，但只有 `isLoaded` 为真时才被 `new`。
- **AE2 方块与方块实体基类**：方块继承 `AEBaseEntityBlock<T>`，方块实体继承 `AENetworkedInvBlockEntity`（无库存则 `AENetworkedBlockEntity`）。注册时两件事都不能漏：`setBlockEntity(...)` 回填类型（否则 `newBlockEntity` 返回 null）、`AEBaseBlockEntity.registerBlockEntityItem(type, item)`（否则机器显示成空气）。存读只覆写 `loadTag`，`loadAdditional` 是 AE2 的 final。
- **能力自登记**：AE2 只给 `AEBlockEntities` 自己的类型批量登记能力，第三方类型必须写进 `registry/ModCapabilities.java`；`RegisterCapabilitiesEvent` 是模组总线事件，只能 `modBus.addListener` 或显式 `bus = MOD`，用注解默认的 `GAME` 总线会静默失效。新增方块实体时同步补这一处。
- **战利品表**：方块掉落放 `src/main/resources/data/<ns>/loot_table/blocks/`（1.21 目录名是单数）；缺表时方块破坏后什么都不掉，且不报错。
- **名字即协议**：客户端/服务端靠字符串动作名与 `@GuiSync(n)` 注解值配对，不能改、不能被第二个字段复用；`ModSlotSemantics` 的注册名必须与 AEjson 的 `slots` 键逐字一致。**继承 `UpgradeableMenu` 时编号必须避开 0 / 1 / 5 / 6**（`rsMode` / `fzMode` / `cMode` / `schedulingMode` 已占用），本仓库用 10 / 11。
- **升级卡登记**：可升级对象实现 `IUpgradeableObject`，库存用 `UpgradeInventories.forMachine(...)`；兼容性与上限必须另调 `Upgrades.add(卡, 对象, 上限)`，且**只能在 `FMLCommonSetupEvent` 的 `enqueueWork` 里调**——模组构造期物品未绑定，`AEItems.*.asItem()` 拿不到实例。升级库存不参与 `getInternalInventory()`，要在 `saveAdditional` / `loadTag` 里单独 `writeToNBT` / `readFromNBT`。
- **指南书**：页面放 `assets/<ns>/ae2guide/*.md`，`Guide.builder(...).folder("ae2guide")` 的目录名必须逐字一致；指南书要在客户端入口**构造函数里**建（不能等 `FMLClientSetupEvent`，否则资源重载时页面为空）。`AEBaseScreen` 已自带左侧指南按钮，点击落点由覆盖 `openHelp()` 决定——不覆盖会跳到 AE2 的指南。
- **自定义样板**：必须用 `PatternDetailsHelper.encodedPatternItemBuilder(...)` 生成 `EncodedPatternItem`，自己写 `Item` 子类不会被 `AEPatternDecoder` 识别；数据组件要同时给 `persistent` 与 `networkSynchronized`，只给前者会让客户端读到空样板。网格里的空格用 `null`，收尾不能用 `List.copyOf`（拒绝 null）。
- **界面布局**：装配室界面走 AE2 `ScreenStyle`，坐标只写在 `assets/ae2/screens/ae2_crafting_integration/molecular_assembler.json`；Java 侧只保留槽位数量与索引契约，不硬编码坐标。
- **文件规模**：一个类只装一件能一句话讲清的事，超过 800 行必须按职责拆（目标 ≤ 650 行）。
- **资源自包含**：`assets/ae2_crafting_integration/` 下的模型与纹理是 AE2 本体资源的物理副本，JSON 里不得出现 `ae2:` 引用；parent 模型链（`item/display_base` → `item/part_base`、`part/display_off`）与纹理已一并复制。换外观直接覆盖同名 PNG，不要改成远程引用。两个例外：`textures/guis/molecular_assembler.png` 已按 9x9 布局重绘，`textures/item/augmented_crafting_pattern.png` 由 AE2 `crafting_pattern.png` 改色加角标生成。
- **基线三件套**：`molecular_assembler` 已有方块实体、91 格容器、4 格升级槽 + 加速卡、9x9 界面与 AE2 能力面（配方匹配、样板供应器与 tick 未接）；`pattern_encoding_terminal` 已是可贴电缆的 `PartItem`（`AugmentedPatternEncodingTerminalPart extends PatternEncodingTerminalPart`），`wireless_terminal` 已是 `WirelessTerminalItem` + 自建宿主，两者都开自有 `AugmentedPatternEncodingTermMenu.TYPE`（`MEStorageMenu` + 81 格 `FakeSlot`），界面走 `screens/ae2_crafting_integration/pattern_encoding_terminal.json` + 本模组 `textures/guis/pattern.png`（512×512、五片纵向切片），编写样板按钮已能产出增广合成样板；产物预览走 `common/logic/AugmentedRecipeMatcher` 的判定器链（原版合成台 + 合成拓展 + 无尽表格 + 无尽锻造），标志工作方块决定无尽的工作台档位与网格尺寸（该格由宿主按 `WorkstationHost` 落盘），图案居中铺进目标台，装配室按样板执行仍待接。注册 id 沿用 AE2 原名，处于本模组自己的 namespace。
- **新增格子的存档归属**：不在 AE2 逻辑（`PatternEncodingLogic` 之类）读写范围内的格子，由**宿主**自己落盘——部件覆写 `readFromNBT` / `writeToNBT` 把库存写进自己的键（拆部件要随 `addAdditionalDrops` 掉落、`clearContent` 清空），无线宿主写物品 `CUSTOM_DATA` 的同一根 compound。菜单只认 `common/menu/WorkstationHost` 这样的接口，`host instanceof` 失败时才退回临时库存；别把「菜单里 new 的库存」当正本，那样关窗即丢。
- **JEI 配方转移**：JEI 按「容器类 + 配方类别」**精确查表**（`ImmutableTable`，不是 `isAssignableFrom`），复用不了 AE2 绑在 `PatternEncodingTermMenu.class` 上的处理器，所以 `client/integration/jei/` 自建 `@JeiPlugin` 逐类别注册；类别不直接引用邻居的常量，而是用 `IJeiHelpers#getRecipeType(uid)` 反查已注册类别。写入必须发 `InventoryActionPacket(SET_FILTER)`（走 `AEBaseMenu#setFilter` → `ConfigInventory`），不要用 JEI 默认的客户端 `Slot#set`——`FakeSlot` 在客户端写不进去。新增可编码的类别时改 `common/logic/RecipeGridCategories`，别在注册端散写 UID。
- **依赖版本**：`gradle/libs.versions.toml` 的 AE2 编译基线与 `build.gradle` 里 CurseForge 运行期构件必须同版，`resolutionStrategy.force` 已钉住；两个坐标要一起动。
- **本地件**：`libs/*.jar` 与 `docs/LOCAL_*.md` 不进版本库；缺了运行期集成无法验证。
- **验证**：改动后按类型跑 `./gradlew build` / `test`；界面交互与时序仍需 `runClient` 实机验证。

## 目录

```
src/main/java/io/github/lounode/ae2crafting/             模组入口
src/main/java/io/github/lounode/ae2crafting/registry/    方块 / 物品 / 组件 / 创造页 / 方块实体 / 菜单 / 槽位语义 / 能力 / 升级登记
src/main/java/io/github/lounode/ae2crafting/common/      方块、方块实体、容器、菜单、样板
src/main/templates/META-INF/neoforge.mods.toml           模组元数据模板（构建时展开）
src/main/resources/assets/ae2_crafting_integration/      模型、纹理、lang、ae2guide 指南页面
src/main/resources/assets/ae2/screens/                   AE2 ScreenStyle 布局文档（AEjson）
src/main/resources/data/ae2_crafting_integration/        战利品表等数据包内容
src/client/java/                                        仅客户端代码：界面、屏幕注册、指南书注册与 JEI 插件
src/test/java/                                          单元测试
src/generated/resources/                                数据生成输出
tools/layout/                                           终端贴图布局分析脚本（Pillow，换贴图后重算坐标用）
```

## 常用命令

```bash
./gradlew build              # 全量构建
./gradlew compileJava        # 只编 main
./gradlew compileClientJava  # 只编 client
./gradlew runClient          # 实机验证
./gradlew runServer
./gradlew runData            # 数据生成
python tools/layout/analyze.py   # 从贴图量出 AEjson 的切片与槽位坐标
python tools/layout/preview.py   # 按 AEjson 重拼背景 + 标注槽位，出核对图
```
