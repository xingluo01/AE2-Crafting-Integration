# 资产授权清单

本文件是 `README.md` License 一节的明细：哪些资产来自 Applied Energistics 2（LGPL-3.0），哪些是本模组自写文件。表格路径相对 `src/main/resources/`，正文里的 `models/...`、`textures/...` 相对 `assets/ae2_crafting_integration/`。

## 授权分划

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.

当前没有 ARR 资产：其余基线资源来自 AE2（LGPL-3.0），自写的 GUI 底图与其余文件随项目一并按 LGPL-3.0 分发。

## Upstream Attribution

This mod is an addon for **[Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)** (AE2), also licensed under LGPL-3.0.

**The following textures are copied from AE2 (LGPL-3.0), byte-identical unless noted:**

| File | AE2 Source |
|------|-----------|
| `assets/ae2_crafting_integration/textures/block/molecular_assembler.png` | `assets/ae2/textures/block/molecular_assembler.png` |
| `assets/ae2_crafting_integration/textures/block/molecular_assembler_lights.png` | `assets/ae2/textures/block/molecular_assembler_lights.png` |
| `assets/ae2_crafting_integration/textures/block/molecular_assembler_lights.png.mcmeta` | 同路径动画元数据 |
| `assets/ae2_crafting_integration/textures/part/monitor_back.png` | `assets/ae2/textures/part/monitor_back.png` |
| `assets/ae2_crafting_integration/textures/part/monitor_colored.png` | `assets/ae2/textures/part/monitor_colored.png` |
| `assets/ae2_crafting_integration/textures/part/monitor_front.png` | `assets/ae2/textures/part/monitor_front.png` |
| `assets/ae2_crafting_integration/textures/part/monitor_sides.png` | `assets/ae2/textures/part/monitor_sides.png` |
| `assets/ae2_crafting_integration/textures/part/pattern_encoding_terminal_bright.png` | `assets/ae2/textures/part/pattern_encoding_terminal_bright.png` |
| `assets/ae2_crafting_integration/textures/part/pattern_encoding_terminal_medium.png` | `assets/ae2/textures/part/pattern_encoding_terminal_medium.png` |
| `assets/ae2_crafting_integration/textures/part/pattern_encoding_terminal_dark.png` | `assets/ae2/textures/part/pattern_encoding_terminal_dark.png` |
| `assets/ae2_crafting_integration/textures/part/terminal_bright.png` | `assets/ae2/textures/part/terminal_bright.png` |
| `assets/ae2_crafting_integration/textures/part/terminal_medium.png` | `assets/ae2/textures/part/terminal_medium.png` |
| `assets/ae2_crafting_integration/textures/part/terminal_dark.png` | `assets/ae2/textures/part/terminal_dark.png` |
| `assets/ae2_crafting_integration/textures/item/wireless_terminal.png` | `assets/ae2/textures/item/wireless_terminal.png` |
| `assets/ae2_crafting_integration/textures/item/wireless_crafting_terminal.png` | `assets/ae2/textures/item/wireless_crafting_terminal.png` |
| `assets/ae2_crafting_integration/textures/item/augmented_crafting_pattern.png` | `assets/ae2/textures/item/crafting_pattern.png`（**派生**：蓝色系转紫、右下角加 3×3 角标，非字节副本） |
| `assets/ae2_crafting_integration/textures/guis/pattern.png` | `assets/ae2/textures/guis/pattern.png`（**派生**：编码终端界面外框按 9×9 布局重绘，四个功能槽改成右侧竖排挂带、面板压到 206×347，512×512，五片纵向切片的构图沿用 AE2，非字节副本） |

**The following models are copied from AE2 and re-namespaced (LGPL-3.0):**

| File | AE2 Source | Relation |
|------|-----------|----------|
| `assets/ae2_crafting_integration/blockstates/molecular_assembler.json` | `assets/ae2/blockstates/molecular_assembler.json` | 仅把 `ae2:` 改为 `ae2_crafting_integration:` |
| `assets/ae2_crafting_integration/models/block/molecular_assembler.json` | 同路径 | 仅命名空间改写 |
| `assets/ae2_crafting_integration/models/block/molecular_assembler_lights.json` | 同路径 | 仅命名空间改写 |
| `assets/ae2_crafting_integration/models/item/molecular_assembler.json` | 同路径 | parent 指向本模组方块模型 |
| `assets/ae2_crafting_integration/models/item/display_base.json` | 同路径 | 仅命名空间改写 |
| `assets/ae2_crafting_integration/models/item/part_base.json` | 同路径 | 无命名空间引用，原样复制 |
| `assets/ae2_crafting_integration/models/item/pattern_encoding_terminal.json` | 同路径 | parent 与纹理改指本模组副本 |
| `assets/ae2_crafting_integration/models/part/display_off.json` | 同路径 | 仅命名空间改写 |
| `assets/ae2_crafting_integration/models/part/pattern_encoding_terminal_on.json` | 同路径 | 仅命名空间改写 |
| `assets/ae2_crafting_integration/models/part/pattern_encoding_terminal_off.json` | 同路径 | parent 与纹理改指本模组副本 |

来源基线：AE2 1.21.1 源码（本机 `E:\GitHub\AE2-19.2.8`），对应 AE2 19.2.x。复制只改命名空间，没有像素级修改；替换外观时直接覆盖同名 PNG 即可。**例外**：`textures/guis/molecular_assembler.png` 已按 9x9 布局重绘，不再与 AE2 字节相同，见下一节。

## 自写文件

以下文件不是 AE2 资产，随项目按 LGPL-3.0 分发：

- `assets/ae2_crafting_integration/models/item/wireless_terminal.json`：`minecraft:item/generated` + 本地纹理引用。
- `assets/ae2_crafting_integration/textures/guis/molecular_assembler.png`：9x9 装配室界面底图，本模组重绘（2026-11-11 替换原 AE2 副本），含 9x9 网格底板、催化剂栏、进度条与输出槽。画布 **512×512**，实际内容为主面板 `176×314` 加右侧浮层 `34×50`（2026-11-11 顶部留白由 26px 收到 15px，整体上移 11px）；AE2 `Blitter` 默认按 256×256 归一化 UV，引用这张图的每处 blitter 都要显式写 `textureWidth`/`textureHeight` 为 `512`。
- `assets/ae2/screens/ae2_crafting_integration/molecular_assembler.json`：装配室界面布局文档（AE2 ScreenStyle），本模组自写；放在 `ae2` namespace 是 AE2 `StyleManager` 的读取约定，文档内 `texture` 指向本模组贴图。
- `assets/ae2/screens/ae2_crafting_integration/pattern_encoding_terminal.json`：编码终端界面布局文档（AE2 ScreenStyle），**派生自 AE2 `assets/ae2/screens/terminals/pattern_encoding_terminal.json`**（LGPL-3.0）：只 include `../terminals/terminal.json` 复用 AE2 的终端基类（搜索框、滚动条、玩家背包、工具箱与公共色板），本模组自行声明 9×9 编码槽、标志工作方块槽、产物/样板槽与编写样板按钮，并覆盖五片外框纹理（改指 `textures/guis/pattern.png`）、`textureWidth/Height` 与标题语言键。同样放在 `ae2` namespace。
- `assets/ae2_crafting_integration/lang/en_us.json`、`lang/zh_cn.json`：本模组的名称与提示文案。
- `assets/ae2_crafting_integration/models/item/augmented_crafting_pattern.json`：`minecraft:item/generated` + 本地纹理引用。
- `assets/ae2_crafting_integration/ae2guide/*.md`：GuideME 指南页面（`index.md`、`molecular_assembler.md`、`augmented_pattern.md`），本模组自写文案。
