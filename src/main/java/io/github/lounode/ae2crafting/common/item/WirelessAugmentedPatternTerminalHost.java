package io.github.lounode.ae2crafting.common.item;

import java.util.function.BiConsumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;

import io.github.lounode.ae2crafting.common.menu.WorkstationHost;

/**
 * 无线增广样板编码终端的菜单宿主。
 *
 * <p>AE2 本体只给了无线合成终端（{@code WirelessCraftingTerminalMenuHost}），样板编码这一侧
 * 没有现成实现，所以这里把两半拼起来：{@link WirelessTerminalMenuHost} 负责网格连接、能量与
 * 终端设置，{@link PatternEncodingLogic} 负责编码台（模式、输入输出、空白/已编码样板格）。</p>
 *
 * <p>编码台不是方块也不是部件，没有一个天然的存档位置，所以写进物品的
 * {@code CUSTOM_DATA} 组件：{@code markForSave} 由编码逻辑在格子变动时回调，序列化后随物品栈
 * 同步给客户端——客户端重开界面时 {@code PatternEncodingTermMenu} 构造里读到的就是同一份。</p>
 *
 * <p>标志工作方块（{@link WorkstationHost}）同理，只是它的键不在
 * {@link PatternEncodingLogic} 的读写范围内，由本类单独写进同一个根 compound。</p>
 */
public class WirelessAugmentedPatternTerminalHost
        extends WirelessTerminalMenuHost<WirelessAugmentedPatternTerminalItem>
        implements IPatternTerminalMenuHost, IPatternTerminalLogicHost, WorkstationHost {

    /** 物品 NBT 里装编码逻辑的根键，改它等于让所有已保存的样板失效。 */
    private static final String ROOT_TAG = "augmented_pattern_encoding";

    /** 根键下装标志工作方块的子键。与编码逻辑（{@code mode}/{@code encodedInputs} 等）平级。 */
    private static final String WORKSTATION_TAG = "workstation";

    private PatternEncodingLogic logic;

    /**
     * 标志工作方块。与编码台分开存：它的键不在 {@code PatternEncodingLogic} 的读写范围内，
     * 由本类自己写进同一个根 compound。
     */
    private final ConfigInventory workstationInventory = ConfigInventory.configStacks(1)
            .changeListener(this::markForSave)
            .build();

    /** 读档期间编码逻辑会回调 markForSave，此时不能反写，否则会把半截数据写回物品。 */
    private boolean loading;

    public WirelessAugmentedPatternTerminalHost(WirelessAugmentedPatternTerminalItem item, Player player,
            ItemMenuHostLocator locator, BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
        // 构造期间就可能回调 markForSave，所以 logic 先赋给字段再读档。
        this.logic = new PatternEncodingLogic(this);
        loadLogicFromStack();
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return logic;
    }

    @Override
    public ConfigInventory getWorkstationInventory() {
        return workstationInventory;
    }

    @Override
    public Level getLevel() {
        return getPlayer().level();
    }

    @Override
    public void markForSave() {
        // 编码逻辑构造时会调到，那时字段还没赋值。
        if (logic == null) {
            return;
        }
        saveLogicToStack();
    }

    private void loadLogicFromStack() {
        var data = getItemStack().get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        var tag = data.copyTag().getCompound(ROOT_TAG);
        if (tag.isEmpty()) {
            return;
        }
        loading = true;
        try {
            logic.readFromNBT(tag, getLevel().registryAccess());
            workstationInventory.readFromChildTag(tag, WORKSTATION_TAG, getLevel().registryAccess());
        } finally {
            loading = false;
        }
    }

    /**
     * 只让服务端写：客户端那边格子变动是菜单包驱动的，写回去只会拿本地状态覆盖同步值。
     */
    private void saveLogicToStack() {
        if (loading || isClientSide()) {
            return;
        }
        var stack = getItemStack();
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var tag = new CompoundTag();
        logic.writeToNBT(tag, getLevel().registryAccess());
        workstationInventory.writeToChildTag(tag, WORKSTATION_TAG, getLevel().registryAccess());
        root.put(ROOT_TAG, tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
