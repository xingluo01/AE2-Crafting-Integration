# 资产授权清单

本文件是 `README.md` License 一节的明细：哪些资产来自 Applied Energistics 2（LGPL-3.0），哪些是本模组自写文件。表格路径相对 `src/main/resources/`，正文里的 `models/...`、`textures/...` 相对 `assets/ae2_crafting_integration/`。

## 授权分划

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.

当前没有 ARR 资产：基线资源全部来自 AE2（LGPL-3.0），自写文件随项目一并按 LGPL-3.0 分发。

## Upstream Attribution

This mod is an addon for **[Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)** (AE2), also licensed under LGPL-3.0.

**The following textures are copied from AE2 (LGPL-3.0), byte-identical unless noted:**

| File | AE2 Source |
|------|-----------|
| `assets/ae2_crafting_integration/textures/block/molecular_assembler.png` | `assets/ae2/textures/block/molecular_assembler.png` |
| `assets/ae2_crafting_integration/textures/block/molecular_assembler_lights.png` | `assets/ae2/textures/block/molecular_assembler_lights.png` |
| `assets/ae2_crafting_integration/textures/block/molecular_assembler_lights.png.mcmeta` | 同路径动画元数据 |
| `assets/ae2_crafting_integration/textures/guis/molecular_assembler.png` | `assets/ae2/textures/guis/molecular_assembler.png` |
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

来源基线：AE2 1.21.1 源码（本机 `E:\GitHub\AE2-19.2.8`），对应 AE2 19.2.x。复制只改命名空间，没有像素级修改；替换外观时直接覆盖同名 PNG 即可。

## 自写文件

以下文件不是 AE2 资产，随项目按 LGPL-3.0 分发：

- `assets/ae2_crafting_integration/models/item/wireless_terminal.json`：`minecraft:item/generated` + 本地纹理引用。
- `assets/ae2_crafting_integration/lang/en_us.json`、`lang/zh_cn.json`：本模组的名称与提示文案。
