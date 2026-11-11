# AGENTS.md

本仓库的**文档地图 + 动手前必读的硬约定**。先读本页再动手。

## 这是什么

AE2 附属 `ae2_crafting_integration`。命名按「后续更多集成」定，不绑死在某一台机器上。

- **通用分子装配室**：类 AE2 分子装配室，9x9 合成格，下方一排催化剂槽；至少支持合成拓展（Extended Crafting）与无尽重生（Avaritia 系）工作台。
- **ME 大型样板编码终端**：类工作台编码，最大支持 9x9 格子。

**基线：Minecraft 1.21.1 · NeoForge 21.1.241 · AE2 19.2.19 · Java 21**。权威来源是 `gradle/libs.versions.toml` 与 `gradle.properties`。

**框架沿用** `E:\GitHub\AE2-Pattern-Disk`：Gradle wrapper、NeoForge ModDev 构建体系、main/client/test 三源集、模板生成 `neoforge.mods.toml`、LGPL-3.0、CI 构建工作流。骨架只保留通用部分，旧模组的功能源码、集成依赖与本地 libs jar 没有带过来。

## 当前文档

| 文档 | 内容 |
|---|---|
| `README.md` | 玩法自述（英文，GitHub 首页与商店页） |
| `AGENTS.md` | 本页：约定与文档地图 |

`docs/`、`CHANGELOG.md`、更新日志体例、release 发布工作流随功能落地再从框架仓库迁入。当前 CI 只有 `.github/workflows/build.yml`（push / PR 跑构建）。

## 硬约定

- **语言**：注释、`docs/` 与更新日志一律中文；**对外英文文案（`README.md`）**、代码标识符与游戏内英文文案除外。
- **集成红线**：`integration/<邻居>` 只写那个邻居的适配；邻居的类型名不得漏进总是加载的类，缺那个模组时会在加载阶段炸。后续接入工作台模组时按此分包。
- **名字即协议**：客户端/服务端靠字符串动作名与 `@GuiSync(n)` 注解值配对，不能改、不能被第二个字段复用。
- **文件规模**：一个类只装一件能一句话讲清的事，超过 800 行必须按职责拆（目标 ≤ 650 行）。
- **资源自包含**：`assets/ae2_crafting_integration/` 下的模型与纹理是 AE2 本体资源的物理副本，JSON 里不得出现 `ae2:` 引用；parent 模型链（`item/display_base` → `item/part_base`、`part/display_off`）与纹理已一并复制。换外观直接覆盖同名 PNG，不要改成远程引用。
- **基线三件套**：`molecular_assembler`（方块）、`pattern_encoding_terminal`（面板物品）、`wireless_terminal`（无线物品）目前只做注册与外观，AE2 part / 无线终端交互逻辑待接入；注册 id 沿用 AE2 原名，处于本模组自己的 namespace。
- **依赖版本**：`gradle/libs.versions.toml` 的 AE2 编译基线与 `build.gradle` 里 CurseForge 运行期构件必须同版，`resolutionStrategy.force` 已钉住；两个坐标要一起动。
- **本地件**：`libs/*.jar` 与 `docs/LOCAL_*.md` 不进版本库；缺了运行期集成无法验证。
- **验证**：改动后按类型跑 `./gradlew build` / `test`；界面交互与时序仍需 `runClient` 实机验证。

## 目录

```
src/main/java/io/github/lounode/ae2crafting/            模组入口
src/main/java/io/github/lounode/ae2crafting/registry/   方块 / 物品 / 创造页注册
src/main/templates/META-INF/neoforge.mods.toml          模组元数据模板（构建时展开）
src/main/resources/assets/ae2_crafting_integration/     模型、纹理、lang（物理复制自 AE2 本体）
src/client/java/                                        仅客户端代码
src/test/java/                                          单元测试
src/generated/resources/                                数据生成输出
```

## 常用命令

```bash
./gradlew build              # 全量构建
./gradlew compileJava        # 只编 main
./gradlew compileClientJava  # 只编 client
./gradlew runClient          # 实机验证
./gradlew runServer
./gradlew runData            # 数据生成
```
