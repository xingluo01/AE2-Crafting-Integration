# AE2 Crafting Integration

An addon for [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) that brings large-grid crafting into the ME network. The repository is named for its role as a home for crafting integrations — new workbench and mod integrations are expected to land here over time.

- **Loader / MC**: NeoForge 1.21.1
- **NeoForge**: 21.1.241 up to 21.2.0
- **AE2**: 19 or newer (built against 19.2.19)
- **Java**: 21
- **License**: LGPL-3.0

## Scope

### Universal Molecular Assembler Chamber

An AE2-style molecular assembler built around a **9x9 crafting grid**, with a **row of catalyst slots beneath the grid**. It is designed to run recipes from large crafting workbenches rather than only vanilla 3x3 recipes. At minimum it must support the workbenches from Extended Crafting (合成拓展) and the Avaritia-family workbenches (无尽重生), with room for further integrations.

### ME Large Pattern Encoding Terminal

A pattern encoding terminal whose crafting grid supports **up to 9x9** recipes, so large workbench recipes can be encoded into patterns the ME network can autocraft.

## Status

Repository scaffold with three baseline objects registered: the molecular assembler block, the panel-form pattern encoding terminal item, and the wireless terminal item. They are plain registered objects carrying AE2's stock appearance; the 9x9 crafting, catalyst slots, pattern encoding, and ME-network behaviour are not implemented yet.

## Assets

Models and textures are **physically copied** from the Applied Energistics 2 1.21.1 sources into `assets/ae2_crafting_integration/`. The whole parent-model and texture chain is copied as well, and no `ae2:` reference remains in the JSON models — nothing is fetched from upstream at runtime. Replace any PNG in place to change the look.

Copied baseline names are intentionally kept: `molecular_assembler`, `pattern_encoding_terminal` (panel), `wireless_terminal`. They are registration ids in this mod's own namespace, not AE2's.

## Build

```bash
./gradlew build              # 全量构建
./gradlew compileJava        # 只编 main
./gradlew compileClientJava  # 只编 client
./gradlew test               # 单元测试
./gradlew runClient          # 实机验证
./gradlew runData            # 数据生成
```

The build baseline is pinned in `gradle.properties` and `gradle/libs.versions.toml`. Project conventions for contributors are in `AGENTS.md`.
