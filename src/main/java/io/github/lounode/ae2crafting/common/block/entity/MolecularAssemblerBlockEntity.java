package io.github.lounode.ae2crafting.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.inventories.InternalInventory;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkedInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.util.inv.CombinedInternalInventory;

import io.github.lounode.ae2crafting.common.menu.AssemblerSlots;
import io.github.lounode.ae2crafting.registry.ModBlockEntities;
import io.github.lounode.ae2crafting.registry.ModBlocks;

/**
 * 通用分子装配室方块实体：91 格物品、4 格升级卡与进度值的持久化，外加一个 ME 网络节点。
 *
 * <p>继承 {@link AENetworkedInvBlockEntity} 而不是裸 {@code BlockEntity}，一次拿到 AE2 方块实体的四条基线：
 * 网格节点生命周期（{@code onReady} 建节点、{@code setRemoved} 销毁）、
 * {@code getInternalInventory()} 的存读与掉落、{@code saveChanges()} 的合并标脏、
 * 以及 {@code IInWorldGridNodeHost} 的实现。</p>
 *
 * <p>实现 {@link IUpgradeableObject} 后，加速卡走 AE2 标准的 {@link IUpgradeInventory}：
 * 只接受 {@code Upgrades.add} 登记过的卡、上限由登记值决定、变更回调直接落到 {@code saveChanges()}。
 * 升级槽数量必须与 {@link AssemblerAcceleration#MAX_CARDS} 一致，否则界面上会空出多余的槽。</p>
 *
 * <p><b>存读入口是 {@code loadTag} 不是 {@code loadAdditional}</b>：AE2 把 {@code loadAdditional}
 * 定为 final，负责先解客户端视觉数据再转调 {@code loadTag}，子类只能覆盖后者。</p>
 *
 * <p>配方匹配与产物输出尚未接入，进度恒为 0；{@code @GuiSync} 与界面进度条已就位，
 * 后续按 {@link #getProgressPerTick()} 推进 {@code progress} 即可。</p>
 */
public class MolecularAssemblerBlockEntity extends AENetworkedInvBlockEntity implements IUpgradeableObject {

    private final AssemblerInventory inventory;
    /**
     * 对外自动化的视图：催化剂栏 + 产物槽。
     *
     * <p>9x9 网格是装配输入，不接受管道/漏斗/输出总线往里面塞东西，因此整段排除。
     * 产物槽在 {@link AssemblerInventory#isItemValid} 里已禁止放入，抽取不受影响。</p>
     */
    private final InternalInventory exposedInventory;
    /** 升级卡库存不参与 {@code getInternalInventory()}，需要单独存读。 */
    private final IUpgradeInventory upgrades;

    private int progress;
    private int maxProgress = AssemblerAcceleration.FULL_PROGRESS;

    public MolecularAssemblerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MOLECULAR_ASSEMBLER.get(), pos, state);
    }

    public MolecularAssemblerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);

        this.inventory = new AssemblerInventory(this);
        this.exposedInventory = new CombinedInternalInventory(
                inventory.getSubInventory(AssemblerSlots.catalyst(0), AssemblerSlots.output()),
                inventory.getSlotInv(AssemblerSlots.output()));
        this.upgrades = UpgradeInventories.forMachine(
                ModBlocks.MOLECULAR_ASSEMBLER.get(),
                AssemblerAcceleration.MAX_CARDS,
                this::saveChanges);

        // 装配室自身不耗待机功率：接上网络后靠样板供应器推进，频道与能耗由供应器那侧承担。
        // 注意必须在 onReady() 建节点之前设置，节点建立后再改能耗不会追溯生效。
        getMainNode().setIdlePowerUsage(0.0);
    }

    public AssemblerInventory getInventory() {
        return inventory;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return inventory;
    }

    /** 供能力登记使用；不要拿它代替 {@link #getInternalInventory()}，后者才是完整 91 格。 */
    public InternalInventory getExposedInventory() {
        return exposedInventory;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    /** 已插入的加速卡数量，0..{@link AssemblerAcceleration#MAX_CARDS}。 */
    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    /** 当前每 tick 推进的进度点数，插满 4 张时为 250。 */
    public double getProgressPerTick() {
        return AssemblerAcceleration.progressPerTick(getSpeedCardCount());
    }

    /** 当前完成一次合成需要的 tick 数，插满 4 张时为 1。 */
    public int getTicksPerCraft() {
        return AssemblerAcceleration.ticksPerCraft(getSpeedCardCount());
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        upgrades.writeToNBT(data, "upgrades", registries);
        data.putInt("progress", progress);
        data.putInt("maxProgress", maxProgress);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        upgrades.readFromNBT(data, "upgrades", registries);
        progress = data.getInt("progress");
        maxProgress = Math.max(1, data.getInt("maxProgress"));
    }
}
