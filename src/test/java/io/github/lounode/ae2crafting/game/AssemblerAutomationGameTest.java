package io.github.lounode.ae2crafting.game;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.block.entity.MolecularAssemblerBlockEntity;
import io.github.lounode.ae2crafting.common.menu.AssemblerSlots;
import io.github.lounode.ae2crafting.common.menu.MolecularAssemblerMenu;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPattern;
import io.github.lounode.ae2crafting.registry.ModBlocks;
import io.github.lounode.ae2crafting.registry.ModItems;
import io.github.lounode.ae2crafting.registry.ModSlotSemantics;

/**
 * 装配室的自动化链路：样板供应器发配 → 铺格 → 推进度 → 产物弹出返回。
 *
 * <p>这一整段在纯 JUnit 里验不出来：{@code fillGrid} 要真的解码样板组件、{@code finishCraft}
 * 要真的查配方表、{@code pushToSide} 要真的走 {@code InternalInventory.wrapExternal} 的能力查询。
 * 所以这里跑在真实服务器世界上，用真的箱子当「供应器那一侧」——供应器把回收库存以
 * {@code ItemHandler.BLOCK} 暴露出来，箱子提供的是同一种能力，这条通路等价。</p>
 *
 * <p><b>为什么直接调 {@code pushPattern}/{@code tick} 而不是摆一台样板供应器</b>：供应商那侧
 * 需要频道、能量、图案槽、退回库存一整套，摆齐了验的还是 AE2 自己的代码；真正属于本模组的是
 * 「拿到 {@code KeyCounter[]} 之后把原料摆对、把产物推回去」。{@code tick(int)} 被特意做成公开方法
 * 就是为了这件事——构造一个真实 {@code IGridNode} 要整套网格服务，测试里不值得。</p>
 *
 * <p>启动方式：{@code ./gradlew runGameTestServer}。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class AssemblerAutomationGameTest {

    private static final int GRID = 9;
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    /** 供应器那一侧：产物要往这个方向推回去。 */
    private static final BlockPos PROVIDER = new BlockPos(1, 1, 2);
    private static final Direction PUSH_SIDE = Direction.SOUTH;

    /** 2×2 木板配方在样板网格里的摆放：第 2..3 行、第 3..4 列，故意不贴左上角。 */
    private static final int PATTERN_ORIGIN_X = 3;
    private static final int PATTERN_ORIGIN_Y = 2;
    /** 一次合成要的木板总数，也是 {@code KeyCounter[]} 的长度。 */
    private static final int INPUT_COUNT = 4;

    /**
     * 一次完整合成：铺格对不对、进度推得动、产物回到供应器那一侧、网格清干净。
     *
     * <p>产物必须落在箱子里而不只是进产物槽——进产物槽说明「推回网络」这条主路没通，
     * 只是靠缓冲兜住了，ME 网络里永远看不到这次产物。</p>
     */
    @GameTest(template = "empty")
    public void providerFillRunsCraftAndReturnsOutput(GameTestHelper helper) {
        var machine = placeMachine(helper);
        helper.setBlock(PROVIDER, Blocks.CHEST);
        var pattern = planksPattern(helper, null);

        helper.assertTrue(machine.acceptsPlans(), "刚放下的机器却说不能接活");
        helper.assertTrue(machine.pushPattern(pattern, tableOf(pattern), PUSH_SIDE),
                "样板推不进来：网格是空的、样板也不需要标志物，没有任何理由拒绝");

        // 数据坐标 (x,y) 必须落到 grid(y,x)：行列不能对调，否则产物会按镜像配方去判。
        for (int y = PATTERN_ORIGIN_Y; y < PATTERN_ORIGIN_Y + 2; y++) {
            for (int x = PATTERN_ORIGIN_X; x < PATTERN_ORIGIN_X + 2; x++) {
                var placed = machine.getInventory().getStackInSlot(AssemblerSlots.grid(y, x));
                helper.assertTrue(placed.is(Items.OAK_PLANKS),
                        "网格 (" + x + "," + y + ") 没铺上木板：" + placed);
            }
        }
        helper.assertTrue(machine.getInventory().getStackInSlot(AssemblerSlots.grid(0, 0)).isEmpty(),
                "样板里是空的格子被填上了东西");
        helper.assertTrue(machine.getCurrentPattern() != null, "样板推进来了，机器却说自己没有任务");

        runUntilIdle(machine, 100);

        helper.assertTrue(machine.getCurrentPattern() == null, "推进度之后样板还在，这台机器卡住了");
        helper.assertTrue(machine.getInventory().getStackInSlot(AssemblerSlots.output()).isEmpty(),
                "产物留在了产物槽里：说明没能推回供应器那一侧");

        var chest = providerContainer(helper);
        helper.assertTrue(chest.getItem(0).is(Items.CRAFTING_TABLE),
                "供应器那一侧没收到产物：" + chest.getItem(0));
        helper.assertTrue(chest.getItem(0).getCount() == 1, "产物数量不对：" + chest.getItem(0).getCount());

        for (int i = 0; i < AssemblerSlots.GRID_SLOTS; i++) {
            helper.assertTrue(machine.getInventory().getStackInSlot(i).isEmpty(),
                    "合成完了第 " + i + " 格还留着原料：" + machine.getInventory().getStackInSlot(i));
        }
        helper.succeed();
    }

    /**
     * 标志物栏空着时，带工作台的样板必须连推都推不进来——而且不能留下半份原料。
     *
     * <p>这条是「仅在标志物槽具有样板可用的标志物时才自动化」的执行侧兑现：
     * 拒绝发生在 {@code pushPattern}，不是先答应再去 tick 里反悔。</p>
     */
    @GameTest(template = "empty")
    public void patternWithoutMatchingWorkstationIsRefused(GameTestHelper helper) {
        var machine = placeMachine(helper);
        var pattern = planksPattern(helper, AEItemKey.of(Blocks.CRAFTING_TABLE));

        helper.assertTrue(machine.acceptsPlans(), "空格子的机器应当表示可以接活：拒绝要发生在 pushPattern");
        helper.assertFalse(machine.pushPattern(pattern, tableOf(pattern), PUSH_SIDE),
                "标志物栏里没有工作台，样板却被接下了");

        for (int i = 0; i < AssemblerSlots.GRID_SLOTS; i++) {
            helper.assertTrue(machine.getInventory().getStackInSlot(i).isEmpty(),
                    "被拒的样板在第 " + i + " 格留下了原料，下次发配会带着脏网格开机");
        }
        helper.assertTrue(machine.getCurrentPattern() == null, "被拒之后机器却认为自己有任务");

        // 放上工作台，同一张样板就该推得进来——准入门槛是「有没有」，不是「能不能」。
        machine.getInventory().setItemDirect(AssemblerSlots.catalyst(0), new ItemStack(Blocks.CRAFTING_TABLE));
        helper.assertTrue(machine.pushPattern(pattern, tableOf(pattern), PUSH_SIDE),
                "标志物栏里已经放了样板要的合成台，样板还是推不进来");
        helper.succeed();
    }

    /**
     * 合成途中把标志物抽走：机器停下，网格里一格原料都不能少。
     *
     * <p>这里刻意选「暂停」而不是「作废」：原料是 ME 网络垫的，静默吞掉等于凭空丢东西；
     * 玩家把标志物放回来就能接着做。</p>
     */
    @GameTest(template = "empty")
    public void craftPausesWhenWorkstationIsPulledOut(GameTestHelper helper) {
        var machine = placeMachine(helper);
        helper.setBlock(PROVIDER, Blocks.CHEST);
        var pattern = planksPattern(helper, AEItemKey.of(Blocks.CRAFTING_TABLE));

        machine.getInventory().setItemDirect(AssemblerSlots.catalyst(0), new ItemStack(Blocks.CRAFTING_TABLE));
        helper.assertTrue(machine.pushPattern(pattern, tableOf(pattern), PUSH_SIDE), "样板推不进来");

        machine.getInventory().setItemDirect(AssemblerSlots.catalyst(0), ItemStack.EMPTY);
        for (int i = 0; i < 30; i++) {
            machine.tick(1);
        }

        helper.assertTrue(machine.getCurrentPattern() != null, "标志物没了，机器却把这一炉做完了");
        helper.assertTrue(machine.getInventory().getStackInSlot(AssemblerSlots.grid(PATTERN_ORIGIN_Y, PATTERN_ORIGIN_X))
                .is(Items.OAK_PLANKS), "标志物没了，网格里的原料还是被吃掉了");
        helper.assertTrue(providerContainer(helper).isEmpty(), "标志物没了，产物却已经推出来了");

        // 放回去接着做，结果与一次没被打断时一致。
        machine.getInventory().setItemDirect(AssemblerSlots.catalyst(0), new ItemStack(Blocks.CRAFTING_TABLE));
        runUntilIdle(machine, 100);
        helper.assertTrue(providerContainer(helper).getItem(0).is(Items.CRAFTING_TABLE),
                "标志物放回去之后没能续上：" + providerContainer(helper).getItem(0));
        helper.succeed();
    }

    /**
     * 界面侧：网格槽默认关着、既放不进也拿不出，铺料之后才亮起来。
     *
     * <p>「亮」这件事只有 {@code isSlotEnabled()} 一个真值来源，界面压暗与服务端拒绝都读它；
     * 三者一旦分叉，就会出现「界面看着能放，拖进去没反应」这种最难查的 bug。</p>
     */
    @GameTest(template = "empty")
    public void gridSlotsStayLockedUntilThePatternFillsThem(GameTestHelper helper) {
        var machine = placeMachine(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MolecularAssemblerMenu(0, player.getInventory(), machine);

        var grid = menu.getSlots(ModSlotSemantics.GRID);
        helper.assertTrue(grid.size() == AssemblerSlots.GRID_SLOTS, "网格槽数量不对：" + grid.size());

        for (var slot : grid) {
            var appEngSlot = (AppEngSlot) slot;
            helper.assertTrue(!appEngSlot.isSlotEnabled(), "空格子却是启用状态，界面上会亮着骗人");
            helper.assertFalse(appEngSlot.mayPlace(new ItemStack(Items.OAK_PLANKS)), "网格槽允许手动放入");
            helper.assertFalse(appEngSlot.mayPickup(player), "网格槽允许手动取出");
        }

        var pattern = planksPattern(helper, null);
        helper.assertTrue(machine.pushPattern(pattern, tableOf(pattern), PUSH_SIDE), "样板推不进来");

        var filled = (AppEngSlot) grid.get(AssemblerSlots.grid(PATTERN_ORIGIN_Y, PATTERN_ORIGIN_X));
        helper.assertTrue(filled.isSlotEnabled(), "铺了料的格子仍然是暗的，玩家看不到机器里在做什么");
        helper.assertFalse(filled.mayPickup(player), "网格里已经有料了，仍然不能手动取");

        // 标志物栏是配置槽：进门见山地要能手动放，否则「本机扮演哪张工作台」就配不了。
        var catalyst = (AppEngSlot) menu.getSlots(ModSlotSemantics.CATALYST).getFirst();
        helper.assertTrue(catalyst.mayPlace(new ItemStack(Blocks.CRAFTING_TABLE)), "标志物栏不允许手动放入");

        // 产物槽只出不进：手动放不进，机器自己的产出不受影响（上面几条已经走过）。
        var output = (AppEngSlot) menu.getSlots(ModSlotSemantics.OUTPUT).getFirst();
        helper.assertFalse(output.mayPlace(new ItemStack(Items.CRAFTING_TABLE)), "产物槽允许手动放入");
        helper.succeed();
    }

    /**
     * 背包里 Shift 点加速卡：卡片必须进右侧升级槽，不能被「什么都收」的标志物栏截胡。
     *
     * <p>升级槽的 {@code SlotSemantic} 默认 quickMovePriority 是 0，和机器自己的槽同分；
     * 不给它抬优先级的话，卡片会先落到标志物栏，玩家得再手动拖一次。</p>
     */
    @GameTest(template = "empty")
    public void speedCardsQuickMoveIntoUpgradeSlotsFirst(GameTestHelper helper) {
        var machine = placeMachine(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MolecularAssemblerMenu(0, player.getInventory(), machine);

        var upgrades = menu.getSlots(SlotSemantics.UPGRADE);
        helper.assertFalse(upgrades.isEmpty(), "装配室没有升级槽");

        // 菜单槽下标与背包内部下标不是一回事，按 container 认，别硬编码偏移。
        int menuSlotIndex = -1;
        int inventoryIndex = -1;
        for (var slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                menuSlotIndex = slot.index;
                inventoryIndex = slot.getContainerSlot();
                break;
            }
        }
        helper.assertTrue(menuSlotIndex >= 0, "菜单里找不到玩家背包槽");

        var card = AEItems.SPEED_CARD.stack();
        player.getInventory().setItem(inventoryIndex, card.copy());
        menu.quickMoveStack(player, menuSlotIndex);

        helper.assertTrue(upgrades.getFirst().getItem().getItem() == card.getItem(),
                "加速卡没进升级槽：" + upgrades.getFirst().getItem());
        for (var catalyst : menu.getSlots(ModSlotSemantics.CATALYST)) {
            helper.assertTrue(catalyst.getItem().isEmpty(), "加速卡被塞进标志物栏了");
        }
        helper.succeed();
    }

    /**
     * 样板 tooltip 的输入分组：相同物品合并成一行并累加数量，坐标按 1-based (列,行) 跟着走。
     *
     * <p>顺带确认「标志物行」有数据可用：带工作台的样板 {@code getWorkstation()} 非空，
     * 且不混进输入分组里。</p>
     */
    @GameTest(template = "empty")
    public void patternTooltipMergesEqualInputsWithCoordinates(GameTestHelper helper) {
        var decoded = planksPattern(helper, AEItemKey.of(Blocks.CRAFTING_TABLE));
        helper.assertTrue(decoded instanceof AugmentedPattern, "样板解码后不是 AugmentedPattern");
        var pattern = (AugmentedPattern) decoded;
        helper.assertTrue(pattern.getWorkstation() != null, "严格标志物行需要样板带上工作台");

        var groups = pattern.groupedInputs();
        helper.assertTrue(groups.size() == 1, "4 格木板应合并成 1 行，实际 " + groups.size());
        var planks = groups.getFirst();
        helper.assertTrue(planks.stack().amount() == INPUT_COUNT, "合并数量不对：" + planks.stack().amount());
        helper.assertTrue(planks.positions().size() == INPUT_COUNT, "坐标数量不对");
        helper.assertTrue(planks.positions().getFirst().column() == PATTERN_ORIGIN_X + 1
                && planks.positions().getFirst().row() == PATTERN_ORIGIN_Y + 1,
                "首个坐标应是样板左上角：" + planks.positions().getFirst());
        for (var pos : planks.positions()) {
            helper.assertTrue(pos.column() >= PATTERN_ORIGIN_X + 1 && pos.column() <= PATTERN_ORIGIN_X + 2
                    && pos.row() >= PATTERN_ORIGIN_Y + 1 && pos.row() <= PATTERN_ORIGIN_Y + 2,
                    "坐标落在样板之外：" + pos);
        }

        // 不同物品必须分成不同行，各组坐标互不串位。
        var grid = new ArrayList<GenericStack>(GRID * GRID);
        for (int i = 0; i < GRID * GRID; i++) {
            grid.add(null);
        }
        grid.set(0, GenericStack.fromItemStack(new ItemStack(Items.OAK_PLANKS)));
        grid.set(1, GenericStack.fromItemStack(new ItemStack(Items.DIAMOND)));
        grid.set(2, GenericStack.fromItemStack(new ItemStack(Items.OAK_PLANKS)));

        var stack = new ItemStack(ModItems.AUGMENTED_CRAFTING_PATTERN.get());
        AugmentedPattern.encode(stack, GRID, GRID, grid,
                List.of(GenericStack.fromItemStack(new ItemStack(Items.CRAFTING_TABLE))), null);
        var mixedDetails = PatternDetailsHelper.decodePattern(stack, helper.getLevel());
        helper.assertTrue(mixedDetails instanceof AugmentedPattern, "混合样板解码失败");
        var mixedGroups = ((AugmentedPattern) mixedDetails).groupedInputs();

        helper.assertTrue(mixedGroups.size() == 2, "木板+钻石应分成 2 行，实际 " + mixedGroups.size());
        helper.assertTrue(mixedGroups.get(0).stack().amount() == 2, "木板总量应为 2");
        helper.assertTrue(mixedGroups.get(0).positions().size() == 2, "木板应有 2 个坐标");
        helper.assertTrue(mixedGroups.get(1).stack().amount() == 1, "钻石总量应为 1");
        var diamondPos = mixedGroups.get(1).positions().getFirst();
        helper.assertTrue(diamondPos.column() == 2 && diamondPos.row() == 1,
                "钻石坐标应是 (2,1)：" + diamondPos);
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 铺垫
    // ------------------------------------------------------------------

    private static MolecularAssemblerBlockEntity placeMachine(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.MOLECULAR_ASSEMBLER.get());
        var blockEntity = helper.getBlockEntity(MACHINE);
        helper.assertTrue(blockEntity instanceof MolecularAssemblerBlockEntity,
                "装配室方块放下了却拿到了别的方块实体：" + blockEntity);
        return (MolecularAssemblerBlockEntity) blockEntity;
    }

    private static Container providerContainer(GameTestHelper helper) {
        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(PROVIDER));
        helper.assertTrue(blockEntity instanceof Container, "供应器那一侧不是容器：" + blockEntity);
        return (Container) blockEntity;
    }

    /**
     * 编一张 9×9 增广样板：网格上摆 2×2 木板，产物一个工作台。
     *
     * <p>走 {@code encode} + {@code decodePattern} 而不直接 {@code new AugmentedPattern}：
     * 供应器发配前也要解码一遍，这条注册没接好的话实机上是「供应器看不见这张样板」，
     * 测试里就该在这里断掉。</p>
     */
    private static IPatternDetails planksPattern(GameTestHelper helper, @Nullable AEItemKey workstation) {
        var grid = new ArrayList<GenericStack>(GRID * GRID);
        for (int i = 0; i < GRID * GRID; i++) {
            grid.add(null);
        }
        for (int y = PATTERN_ORIGIN_Y; y < PATTERN_ORIGIN_Y + 2; y++) {
            for (int x = PATTERN_ORIGIN_X; x < PATTERN_ORIGIN_X + 2; x++) {
                grid.set(y * GRID + x, GenericStack.fromItemStack(new ItemStack(Items.OAK_PLANKS)));
            }
        }

        var stack = new ItemStack(ModItems.AUGMENTED_CRAFTING_PATTERN.get());
        AugmentedPattern.encode(stack, GRID, GRID, grid,
                List.of(GenericStack.fromItemStack(new ItemStack(Items.CRAFTING_TABLE))), workstation);

        var decoded = PatternDetailsHelper.decodePattern(stack, helper.getLevel());
        helper.assertTrue(decoded instanceof AugmentedPattern,
                "样板解码没回到 AugmentedPattern，供应器在实机上根本看不见它：" + decoded);
        return decoded;
    }

    /**
     * 供应器交给机器的输入表。
     *
     * <p>与 {@code PatternProviderLogic} 同构：一格一个计数器，长度等于输入数，顺序就是
     * 样板 {@code getInputs()} 的顺序（非空格的行优先），<b>不是</b> 81 格的下标。</p>
     */
    private static KeyCounter[] tableOf(IPatternDetails pattern) {
        var inputs = pattern.getInputs();
        var table = new KeyCounter[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            table[i] = new KeyCounter();
            var possible = inputs[i].getPossibleInputs()[0];
            table[i].add(possible.what(), inputs[i].getMultiplier() * possible.amount());
        }
        return table;
    }

    /** 一直推到机器没活为止；超过上限就把机器留给断言去报，别在这儿死循环。 */
    private static void runUntilIdle(MolecularAssemblerBlockEntity machine, int maxTicks) {
        for (int i = 0; i < maxTicks && machine.getCurrentPattern() != null; i++) {
            machine.tick(1);
        }
    }
}
