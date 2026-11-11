---
navigation:
  title: 增广合成样板
  icon: ae2_crafting_integration:augmented_crafting_pattern
---

# 增广合成样板

普通合成样板只能表达 3×3。要喂给 9×9 装配室的配方，需要**增广合成样板**：它保存整张网格的每格坐标与物品，而不是一个原版配方 id。

## 数据格式

样板里存四样东西：

| 字段 | 含义 |
|---|---|
| `width` / `height` | 网格尺寸，1..9 |
| `grid` | 行优先的定长列表，长度 = 宽 × 高，空格为 `null` |
| `outputs` | 产物列表，第一个是主产物 |

与处理样板的区别：处理样板只记「需要什么、各多少个」，增广合成样板还记「在哪一格」。装配室因此能还原摆放形状，而不是把原料按无序配比塞进去。

## 编码与解码

- 解码由 `AugmentedPattern` 完成：每格生成一个输入，倍率取该格数量，空格跳过。
- 编码入口是 `AugmentedPattern.encode(...)`，接收网格与产物。
- 样板本身由 AE2 的 `PatternDetailsHelper.encodedPatternItemBuilder(...)` 生成，因此 AE2 自带的样板识别、提示与清洗逻辑都会生效。

> 编码终端的 9×9 输入面板尚未接入；在那之前，可以通过命令、数据包或其它模组的样板操作调用编码接口来生成样板。
