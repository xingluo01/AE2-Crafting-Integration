package io.github.lounode.ae2crafting.common.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 特殊工作台的附加规则，全部用**物品 id 字符串**登记，不引用任何邻居的类型。
 *
 * <p>集成红线要求总是加载的类不能出现邻居类名，所以 {@code integration/<邻居>} 在初始化时只往这里
 * 写 {@code "extendedcrafting:ender_alternator"} 这样的字符串；装配室遍历自己的 9 格标志物栏，
 * 用 id 查出「这一格算不算发电机/基座」再计数。缺模组时注册表为空，所有规则自动退化为普通合成。</p>
 */
public final class CraftRules {

    /** 没有发电机时的耗时倍率。 */
    public static final int BASE_DURATION_MULTIPLIER = 50;
    /** 最多生效的发电机数量；满配时倍率降到 1，即正常速度。 */
    public static final int MAX_ALTERNATORS = 49;
    /** 组合合成器不插基座时提供的输入槽位。 */
    public static final int BASE_INPUT_SLOTS = 1;
    /** 最多生效的基座数量；1 + 80 正好铺满 9×9。 */
    public static final int MAX_PEDESTALS = 80;
    /** 通量合成器每生效一个通量发电机、每次合成额外消耗的 AE。 */
    public static final long FLUX_POWER_PER_ALTERNATOR = 8000L;

    /** 标志物栏里一件物品能贡献的规则类型。 */
    public enum Catalyst {
        ENDER_ALTERNATOR,
        FLUX_ALTERNATOR,
        PEDESTAL
    }

    // 键用 ResourceLocation 而不是 String：查询在装配室每 tick 与判定器每次匹配都会跑，
    // getKey(...).toString() 每次都会新建字符串；ResourceLocation 是注册表里现成的对象。
    private static final Map<ResourceLocation, Catalyst> CATALYSTS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> SPECIAL_WORKSTATIONS = ConcurrentHashMap.newKeySet();

    private CraftRules() {
    }

    /** 由 {@code integration/<邻居>} 在模组初始化时调用；重复登记同一 id 以最后一次为准。 */
    public static void registerCatalyst(String itemId, Catalyst type) {
        CATALYSTS.put(ResourceLocation.parse(itemId), type);
    }

    /**
     * 登记「只由专用判定器处理」的工作台。
     *
     * <p>原版合成台判定器排在链首，遇到这些工作台时必须让路，否则 3×3 的末影合成图案会先被
     * 原版配方截胡。判定依据是物品 id，不加载邻居类。</p>
     */
    public static void registerSpecialWorkstation(String itemId) {
        SPECIAL_WORKSTATIONS.add(ResourceLocation.parse(itemId));
    }

    /** 物品的注册键，例如 {@code extendedcrafting:ender_crafter}。空栈返回 {@code null}。 */
    @Nullable
    public static ResourceLocation itemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public static boolean isSpecialWorkstation(@Nullable ItemStack station) {
        var id = itemKey(station);
        return id != null && SPECIAL_WORKSTATIONS.contains(id);
    }

    @Nullable
    public static Catalyst catalystOf(ItemStack stack) {
        var id = itemKey(stack);
        return id == null ? null : CATALYSTS.get(id);
    }

    /**
     * 统计标志物栏里某一类催化剂的**总个数**（按堆叠数量累加，不按槽位数）。
     *
     * <p>9 格栏每格最多 64 件，足够覆盖 49 个发电机与 80 个基座；empty 栈按 0 计。</p>
     */
    public static int countCatalyst(List<ItemStack> catalysts, Catalyst wanted) {
        int total = 0;
        for (var stack : catalysts) {
            if (stack != null && !stack.isEmpty() && catalystOf(stack) == wanted) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 耗时倍率的纯算术版本：普通配方恒为 1；末影/通量合成器为
     * {@code 50 - min(49, 发电机数)}，即没有发电机时慢 50 倍、装满 49 个恢复正常。
     */
    public static int durationMultiplier(CraftKind kind, int alternators) {
        if (!kind.isAlternatorDriven()) {
            return 1;
        }
        return BASE_DURATION_MULTIPLIER - Math.min(MAX_ALTERNATORS, Math.max(0, alternators));
    }

    /** 按标志物栏里对应发电机的总数量折算耗时倍率。 */
    public static int durationMultiplier(CraftKind kind, List<ItemStack> catalysts) {
        if (!kind.isAlternatorDriven()) {
            return 1;
        }
        var wanted = kind == CraftKind.ENDER_CRAFTER
                ? Catalyst.ENDER_ALTERNATOR : Catalyst.FLUX_ALTERNATOR;
        return durationMultiplier(kind, countCatalyst(catalysts, wanted));
    }

    /** 组合合成器可用输入槽位的纯算术版本：基础 1 个，每个基座多 1 个，最多 81 个。 */
    public static int availableInputSlots(int pedestals) {
        return BASE_INPUT_SLOTS + Math.min(MAX_PEDESTALS, Math.max(0, pedestals));
    }

    /** 按标志物栏里基座的总数量算可用输入槽位。 */
    public static int availableInputSlots(List<ItemStack> catalysts) {
        return availableInputSlots(countCatalyst(catalysts, Catalyst.PEDESTAL));
    }

    /**
     * 通量合成器的额外耗能：每个生效的通量发电机 8000 AE，其他配方为 0。
     * 生效数量与耗时倍率同一口径（上限 49）。
     */
    public static long fluxPowerCost(CraftKind kind, int alternators) {
        if (kind != CraftKind.FLUX_CRAFTER) {
            return 0L;
        }
        return FLUX_POWER_PER_ALTERNATOR * Math.min(MAX_ALTERNATORS, Math.max(0, alternators));
    }

    /** 按标志物栏里通量发电机的总数量算额外耗能。 */
    public static long fluxPowerCost(CraftKind kind, List<ItemStack> catalysts) {
        if (kind != CraftKind.FLUX_CRAFTER) {
            return 0L;
        }
        return fluxPowerCost(kind, countCatalyst(catalysts, Catalyst.FLUX_ALTERNATOR));
    }
}
