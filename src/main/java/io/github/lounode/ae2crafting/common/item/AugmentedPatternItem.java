package io.github.lounode.ae2crafting.common.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import appeng.core.AppEng;
import appeng.core.localization.GuiText;
import appeng.crafting.pattern.EncodedPatternItem;

import io.github.lounode.ae2crafting.common.pattern.AugmentedPattern;

/**
 * 增广合成样板物品：沿用 AE2 的 {@code EncodedPatternItem} 类型（解码器只认这个类型），
 * 但把 tooltip 换成自己的排版。
 *
 * <p>AE2 的 {@code appendHoverText} 固定按「产物行 → 逐条输入行 → properties 行」画，
 * 而 9×9 样板的 {@code getInputs()} 是每格一个 {@code IInput}，结果就是同一个物品在
 * 「使用 / 与 …」里重复好几行。这里改成：</p>
 * <ol>
 *   <li><b>标志物行</b>（在「制作」之上）：样板要求的工作台，装配室必须按物品键全等匹配；
 *       没有要求时显示「不要求」。</li>
 *   <li><b>制作行</b>：产物。</li>
 *   <li><b>使用行</b>：按物品键合并后的输入；开启高级信息（F3+H）时每行再附它在
 *       9×9 网格里的 1-based (列,行) 坐标——那是内部排布信息，普通悬停不显示。</li>
 * </ol>
 *
 * <p>不调用 {@code super.appendHoverText}：那一份逐格罗列的行由本类完整重排，调用 super 只会
 * 重复画两遍。「按下[P]查看样板」之类的提示由外部 tooltip 事件添加，不在这个方法里，不受影响。</p>
 */
public class AugmentedPatternItem extends EncodedPatternItem<AugmentedPattern> {

    private static final String WORKSTATION = "gui.ae2_crafting_integration.pattern.workstation";
    private static final String WORKSTATION_NONE = "gui.ae2_crafting_integration.pattern.workstation.none";

    public AugmentedPatternItem(Item.Properties properties) {
        super(properties, AugmentedPattern::new, null);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines,
            TooltipFlag flags) {
        var level = AppEng.instance().getClientLevel();
        if (level == null) {
            return;
        }

        AugmentedPattern details;
        try {
            if (!(decode(stack, level) instanceof AugmentedPattern decoded)) {
                return;
            }
            details = decoded;
        } catch (RuntimeException e) {
            lines.add(GuiText.InvalidPattern.text().copy().withStyle(ChatFormatting.RED));
            return;
        }

        var and = Component.literal(" ").append(GuiText.And.text()).append(" ")
                .withStyle(ChatFormatting.GRAY);

        // 1) 标志物：位置固定在「制作」之上。
        var station = details.getWorkstation();
        lines.add(Component.empty()
                .append(Component.translatable(WORKSTATION).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(station != null
                        ? station.getDisplayName()
                        : Component.translatable(WORKSTATION_NONE)
                                .withStyle(ChatFormatting.DARK_GRAY)));

        // 2) 制作：产物。
        var label = Component.empty().append(GuiText.Produces.text()).append(": ")
                .withStyle(ChatFormatting.GRAY);
        boolean first = true;
        for (var output : details.getOutputs()) {
            lines.add(Component.empty().append(first ? label : and).append(getTooltipEntryLine(output)));
            first = false;
        }

        // 3) 使用：相同物品合并成一行。坐标是内部排布信息，只在开启高级信息（F3+H）时显示。
        var with = GuiText.With.text().copy().append(": ").withStyle(ChatFormatting.GRAY);
        first = true;
        for (var group : details.groupedInputs()) {
            var line = Component.empty().append(first ? with : and)
                    .append(getTooltipEntryLine(group.stack()));
            if (flags.isAdvanced()) {
                line.append(coordinates(group.positions()));
            }
            lines.add(line);
            first = false;
        }
    }

    /** 坐标串用灰色小字跟在物品后面：(列,行) (列,行) … */
    private static Component coordinates(List<AugmentedPattern.GridPos> positions) {
        var text = new StringBuilder();
        for (var pos : positions) {
            text.append(" (").append(pos.column()).append(',').append(pos.row()).append(')');
        }
        return Component.literal(text.toString()).withStyle(ChatFormatting.DARK_GRAY);
    }
}
