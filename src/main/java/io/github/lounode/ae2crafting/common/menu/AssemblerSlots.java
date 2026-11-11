package io.github.lounode.ae2crafting.common.menu;

/**
 * 装配室容器的槽位编号契约。
 *
 * <p>这里只放数量与索引；槽位在界面上的位置一律由 AEjson
 * （{@code assets/ae2/screens/ae2_crafting_integration/molecular_assembler.json}）决定，
 * 改布局只动那份 JSON。</p>
 */
public final class AssemblerSlots {

    public static final int GRID_SIZE = 9;
    public static final int GRID_SLOTS = GRID_SIZE * GRID_SIZE;
    public static final int CATALYST_SLOTS = GRID_SIZE;
    public static final int OUTPUT_SLOTS = 1;
    public static final int MACHINE_SLOTS = GRID_SLOTS + CATALYST_SLOTS + OUTPUT_SLOTS;

    /** 9x9 合成网格：行优先，索引 0..80。 */
    public static int grid(int row, int col) {
        return row * GRID_SIZE + col;
    }

    /** 网格下方一排催化剂槽，索引 81..89。 */
    public static int catalyst(int col) {
        return GRID_SLOTS + col;
    }

    /** 产物输出槽，索引 90。 */
    public static int output() {
        return GRID_SLOTS + CATALYST_SLOTS;
    }

    public static boolean isMachineSlot(int index) {
        return index >= 0 && index < MACHINE_SLOTS;
    }

    private AssemblerSlots() {
    }
}
