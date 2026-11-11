package io.github.lounode.ae2crafting.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.locator.MenuLocators;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.item.WirelessAugmentedPatternTerminalHost;
import io.github.lounode.ae2crafting.registry.ModItems;

/**
 * 标志工作方块的持久化：两种终端形态各存各的位置，关窗重开不能丢。
 *
 * <p>这一格不在 AE2 的 {@code PatternEncodingLogic} 里，是集成层新加的语义，所以必须自己验
 * 落盘——只靠「界面上看着还在」看不出来：菜单每次打开都会新建库存，丢档时表现为
 * 「重开就空了，产物预览也跟着没」，而不是报错。</p>
 *
 * <p>启动方式：{@code ./gradlew runGameTestServer}。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class WorkstationPersistenceGameTest {

    /**
     * 面板形态：写进部件 NBT 的 {@code workstation} 键，拆装一次还能读回来。
     *
     * <p>必须用 {@code PartHelper.setPart} 挂到真实线缆上，不能直接 {@code new}：
     * AE2 的 {@code AbstractTerminalPart#readFromNBT} 会回填配置管理器，回调里
     * {@code saveChanges()} 直接取 {@code getHost()}——未放置的部件在那里就是 NPE。</p>
     */
    @GameTest(template = "empty")
    public void panelPartKeepsWorkstationInPartNbt(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var partItem = ModItems.PATTERN_ENCODING_TERMINAL.get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var workstation = AEItemKey.of(Blocks.CRAFTING_TABLE);

        var source = PartHelper.setPart(level, helper.absolutePos(new BlockPos(1, 1, 1)),
                Direction.UP, player, partItem);
        helper.assertTrue(source != null, "线缆上没放下部件，后面几条都无从谈起");
        source.getWorkstationInventory().setStack(0, new GenericStack(workstation, 1));

        var tag = new CompoundTag();
        source.writeToNBT(tag, registries);
        helper.assertTrue(tag.contains("workstation"), "部件 NBT 里没有 workstation 键，拆装后必然丢档");

        var reloaded = PartHelper.setPart(level, helper.absolutePos(new BlockPos(3, 1, 1)),
                Direction.UP, player, partItem);
        helper.assertTrue(reloaded != null, "第二个位置没放下部件");
        reloaded.readFromNBT(tag, registries);

        var restored = reloaded.getWorkstationInventory().getStack(0);
        helper.assertTrue(restored != null && workstation.equals(restored.what()),
                "重读部件 NBT 后标志物变了：" + restored + "，期望 " + workstation);
        helper.succeed();
    }

    /** 无线形态：写进物品的 {@code CUSTOM_DATA}，换一个宿主（等同于关窗重开）仍能读到。 */
    @GameTest(template = "empty")
    public void wirelessHostKeepsWorkstationInItemData(GameTestHelper helper) {
        var item = ModItems.WIRELESS_TERMINAL.get();
        var stack = new ItemStack(item);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var locator = MenuLocators.forStack(stack);
        var workstation = AEItemKey.of(Blocks.CRAFTING_TABLE);

        var first = new WirelessAugmentedPatternTerminalHost(item, player, locator, (p, menu) -> {
        });
        first.getWorkstationInventory().setStack(0, new GenericStack(workstation, 1));
        helper.assertTrue(stack.get(DataComponents.CUSTOM_DATA) != null,
                "物品上没有写 CUSTOM_DATA，标志物只留在了内存里");

        var second = new WirelessAugmentedPatternTerminalHost(item, player, locator, (p, menu) -> {
        });
        var restored = second.getWorkstationInventory().getStack(0);
        helper.assertTrue(restored != null && workstation.equals(restored.what()),
                "重开宿主后标志物变了：" + restored + "，期望 " + workstation);
        helper.succeed();
    }
}
