# 发版手册

当前仓库只有 `.github/workflows/build.yml`（push / PR 构建），**没有发布工作流**。首次发版前先按第一节把参考项目 `AE2-Pattern-Disk` 的发布链路迁入。

## 一、迁移清单

1. 复制参考项目的 `.github/workflows/release.yml`，把 `AE2 Pattern Disk` 改成 `AE2 Crafting Integration`，`ae2_pattern_disk` 改成 `ae2_crafting_integration`。
2. `build.gradle` 加 `com.modrinth.minotaur` 插件与 `modrinth {}` 块，`dependencies` 里改成 `required.project 'ae2'`。
3. 建 Modrinth 与 CurseForge 项目，把项目 id 填进仓库 variables。
4. 更新日志写法见 `CHANGELOG_STYLE.md`；发布流程与下面第二到四节一致。

## 二、发版流程

1. 把 `CHANGELOG_UNRELEASED.md` 里 `## [未发布]` 整段搬到 `CHANGELOG.md` 顶部，标题改成 `## [<版本>] - <YYYY-MM-DD>`。
2. `gradle.properties` 的 `mod_version` 改成同一个版本号。
3. `git tag vX.Y.Z && git push origin vX.Y.Z`。
4. CI 依次做：解析 tag 版本 → JDK 21 → 构建 → Modrinth → CurseForge → GitHub Release。
5. 发布后按第五节核对。

中断后重跑同一 tag 会失败（Modrinth 版本号已存在、CurseForge 不去重），优先改版本号重发。

## 三、仓库变量与密钥

| 类型 | 名称 |
|---|---|
| vars | `MODRINTH_PROJECT_ID`、`CURSEFORGE_PROJECT_ID` |
| secrets | `MODRINTH_TOKEN`（PAT，勾 `VERSION_CREATE` + `VERSION_WRITE` + `PROJECT_WRITE`）、`CURSEFORGE_TOKEN` |

## 四、平台注意

### Modrinth

- 项目 id：待建，建好后填 `MODRINTH_PROJECT_ID`。
- 依赖由 `build.gradle` 的 `modrinth { dependencies { required.project 'ae2' } }` 声明。

### CurseForge

**双 API 混用，两者不通用。** 项目数字 id：待建。

- **上传**走传统接口 `POST https://minecraft.curseforge.com/api/projects/{数字id}/upload-file`，header `X-Api-Token`：
  - token 是**网站账户 token**（UUID 形式，`curseforge.com/account/api-tokens` 生成），不是 console 的 API key；
  - 路径只认**数字 project id**，用 slug 会 302 到错误页；
  - `gameVersions` 必须是**数字 id**：`1.21.1=11779`、`NeoForge=10150`、`Client=9638`、`Server=9639`。少环境组会报 `must select at least one version from the environment group of versions`。
- **元数据查询**走 Eternal API `https://api.curseforge.com/v1/...`，header `x-api-key`：key 是 **console 的 API key**（`$2a$10$…` bcrypt 串）。拿它去上传会报 `API token is malformed`。
- `itsmeow/curseforge-upload` 的 `game_versions` 接受名称/slug 但只查表转 id，**不自动补环境组**、**匹配不到会静默丢弃**，所以 `release.yml` 里直接写数字 id。
- `relations:` 让 CF 页标出前置，该 Action 只发 slug、按逗号切分后不 trim，不要加空格。
- 新项目首个文件、以及每个新文件都要**人工审核**：`isAvailable=False`、`latestFiles` 暂为空属正常。

## 五、发布后核对

- CI 日志里有 `changelog source: CHANGELOG.md (N chars)`，且发布正文首行是小节名（`### 新增` 一类）。若是 `commit log` 或首行是 `- 主题 (sha)`，说明没取到版本段。
- `fail_on_unmatched_files: true` 保护：产物路径写错会硬失败，而不是生成没有附件的 release。
- Modrinth 版本依赖里应有 `ae2` + `required`；CF 文件页 Relations 应出现 AE2 前置。
- 网页侧人工项（API 改不了）：Client/Server 环境标记、gallery 截图、项目正文授权表述。

## 六、CI action 版本

Node20 已于 2026-09-23 从 runner 移除，四个声明 node20 的 action 需升到 node24：`actions/checkout@v7`、`actions/setup-java@v6`、`gradle/actions/setup-gradle@v6`（加 `cache-provider: basic`）、`softprops/action-gh-release@v3`。

`itsmeow/curseforge-upload@v3.1.2` 仍是 node20，上游最后提交 2024-04、无更高版本可升。发版时会输出 `Node.js 20 is deprecated … forced to run on Node.js 24` 警告，上传功能正常。

## 七、已知踩坑

- **Gradle 配置缓存**：minotaur 的 `:modrinth` 任务在配置缓存下报 `invocation of 'Task.project' at execution time is unsupported`，但 BUILD SUCCESSFUL、发布正常。若日后 Gradle 收紧，可加 `--no-configuration-cache`。
- **`-Dorg.gradle.java.home`**：本地验证时偶发不生效（`tasks` 报 `invalid org.gradle.java.home`）；`release.yml` 现用的 `-D` 写法已被真实发版证明有效，不要主动改。
- **从默认分支触发的 workflow 与 tag 发版不同**：前者 `cache-read-only=false`（可写缓存），后者 `true`（只读）。
