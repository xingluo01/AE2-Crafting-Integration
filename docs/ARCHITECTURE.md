# 架构

当前是基线骨架：三类对象已注册、外观自包含，9x9 合成与编码逻辑尚未实现。改代码前先读完本页。

## 基线

- Minecraft 1.21.1 / NeoForge 21.1.241 / AE2 19.2.19 / Java 21。
- 权威版本来源：`gradle.properties` 与 `gradle/libs.versions.toml`。
- 构建沿用 `AE2-Pattern-Disk` 的 NeoForge ModDevGradle 体系，但只保留通用部分。

## 源集

| 源集 | 目录 | 职责 |
|---|---|---|
| `main` | `src/main/java` | 通用与服务端代码；不能引用客户端类 |
| `client` | `src/client/java` | 仅客户端代码；当前为空 |
| `test` | `src/test/java` | JUnit 单测与 GameTest；不进发布 jar |

`build.gradle` 刻意把 `client` 与 `main` 分开，让公共代码无法误引客户端类——这是编译期防线，不是洁癖。

## 注册链路

```
AE2CraftingIntegration（@Mod 入口，注入 IEventBus）
├── ModBlocks.BLOCKS        → molecular_assembler 方块
├── ModItems.ITEMS          → molecular_assembler 方块物品
│                             pattern_encoding_terminal 面板物品
│                             wireless_terminal 无线物品
└── ModCreativeTabs.TABS    → 创造页 main
```

- 注册统一用 `DeferredRegister`，入口只负责把三个注册表挂到 mod bus。
- 基线 id 沿用 AE2 原名（`molecular_assembler` / `pattern_encoding_terminal` / `wireless_terminal`），处于本模组自己的 namespace；功能落地后如果要改名，连同模型/语言键一起动。
- 当前三件套都是普通 `Block` / `Item`。面板终端要接 AE2 `IPart`，无线终端要接 AE2WTLib，两者都需要新增依赖与注册通道。

## 资源

- 全部模型、纹理在 `src/main/resources/assets/ae2_crafting_integration/`，物理复制自 AE2 1.21.1 源码。
- **JSON 里不得出现 `ae2:` 引用**：parent 模型链（`item/display_base` → `item/part_base`、`part/display_off`）与纹理已一并复制并改成本 namespace。替换外观直接覆盖同名 PNG。
- 授权明细见 `ASSET_LICENSES.md`。

## 测试

- JUnit：`src/test/java`，跑 `./gradlew test`。
- GameTest：`src/test/java/io/github/lounode/ae2crafting/game/`，跑 `./gradlew runGameTestServer`；模板 `empty` 来自 `src/test/resources/data/ae2_crafting_integration/structure/empty.nbt`。
- 客户端启动：`run_client.bat`（等价 `./gradlew runClient --no-configuration-cache`）。界面交互与时序仍需实机看，GameTest 只覆盖注册与逻辑断言。

## 开发运行环境

`build.gradle` 的 `localRuntime` 是从参考项目分四轮复制的开发期模组，只影响 `runClient` / `runServer`，不进发布依赖。分组与版本纪律见 `AGENTS.md`「开发运行环境」。

## 后续结构（规划）

功能落地时按职责分包，不要把 9x9 逻辑塞进入口类：

```
common/block/            通用分子装配室方块
common/block/entity/     方块实体、9x9 网格与催化剂槽状态
common/menu/             装配室与编码终端的菜单
common/part/             面板终端（AE2 IPart）
common/logic/            配方匹配、编码、输出解析
integration/<邻居>/      工作台/模组适配，按邻居隔离
```

## 红线

- **集成隔离**：`integration/<邻居>` 只写那个邻居的适配；邻居的类型名不得漏进总是加载的类，缺那个模组时会在加载阶段炸。
- **名字即协议**：客户端/服务端靠字符串动作名与 `@GuiSync(n)` 配对，不能改、不能被第二个字段复用。
- **只用 AE2 公共 API**：不反射内部类当 API，不 mixin AE2 核心；新增例外必须有明确理由并记进注释。
- **文件规模**：一个类只装一件能一句话讲清的事，超过 800 行必须按职责拆（目标 ≤ 650 行）。
