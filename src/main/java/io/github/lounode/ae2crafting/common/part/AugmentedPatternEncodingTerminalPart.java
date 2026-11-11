package io.github.lounode.ae2crafting.common.part;

import java.util.List;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import appeng.api.parts.IPartItem;
import appeng.api.stacks.GenericStack;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import appeng.util.ConfigInventory;

import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;
import io.github.lounode.ae2crafting.common.menu.WorkstationHost;

/**
 * ME 增广样板编码终端（线缆面板形态）。
 *
 * <p>编码逻辑（{@code PatternEncodingLogic}）、部件模型与放置行为全部继承 AE2 的
 * {@link PatternEncodingTerminalPart}，本类把<b>菜单类型</b>换成
 * {@link AugmentedPatternEncodingTermMenu#TYPE}：服务端下发开屏包时取
 * {@code menu.getType()}，客户端再按它查界面工厂，没有这一步本模组复制过来的
 * AEjson 界面永远不会被打开。</p>
 *
 * <p><b>标志工作方块为什么由本类保管</b>：AE2 的编码逻辑只存模式、输入输出与样板槽，
 * 没有第 82 格的位置。这一格是集成层新增的语义，所以跟部件一起落盘（同一个 NBT 根下的
 * {@value #WORKSTATION_TAG} 键），拆掉部件时连同里面的工作台一起掉落——
 * 否则玩家放进去的一张终极工作台会因为拆线缆而凭空消失。</p>
 */
public class AugmentedPatternEncodingTerminalPart extends PatternEncodingTerminalPart implements WorkstationHost {

    /** 部件 NBT 里装标志工作方块的键。改它等于让所有已放置终端里的工作台失效。 */
    private static final String WORKSTATION_TAG = "workstation";

    private final ConfigInventory workstationInventory = ConfigInventory.configStacks(1)
            .changeListener(this::onWorkstationChanged)
            .build();

    /** 读档期间空格也算「变动」，此时反写只会把半截数据写回自己的 NBT。 */
    private boolean loading;

    public AugmentedPatternEncodingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public MenuType<?> getMenuType(Player player) {
        return AugmentedPatternEncodingTermMenu.TYPE;
    }

    @Override
    public ConfigInventory getWorkstationInventory() {
        return workstationInventory;
    }

    private void onWorkstationChanged() {
        // 读档回填会逐格触发；客户端这一格是槽位包写进来的副本，反写只会在本地标记脏。
        if (loading || isClientSide()) {
            return;
        }
        // 部件本体还没挂到宿主上时（构造、测试里直接 new）不能存档。
        var host = getHost();
        if (host != null) {
            host.markForSave();
        }
    }

    @Override
    public void readFromNBT(CompoundTag data, Provider registries) {
        super.readFromNBT(data, registries);
        loading = true;
        try {
            workstationInventory.readFromChildTag(data, WORKSTATION_TAG, registries);
        } finally {
            loading = false;
        }
    }

    @Override
    public void writeToNBT(CompoundTag data, Provider registries) {
        super.writeToNBT(data, registries);
        workstationInventory.writeToChildTag(data, WORKSTATION_TAG, registries);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);

        var stack = workstationInventory.getStack(0);
        if (stack != null) {
            drops.add(GenericStack.wrapInItemStack(stack));
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        workstationInventory.clear();
    }
}
