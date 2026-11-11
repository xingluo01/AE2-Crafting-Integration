package io.github.lounode.ae2crafting.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 锁定编码终端界面文档与它引用的贴图。
 *
 * <p>这份文档最容易被「改了一半」：贴图重绘后坐标没跟、或者坐标改了贴图没重出。
 * 所以这里不只比对 JSON 里的数字，还<b>把数字代回贴图取像素</b>——槽位落点必须正好压在
 * 贴图画出来的槽框上，画歪一格就红。</p>
 *
 * <p>另一个必须锁的点是 {@code bottom}：终端行数随窗口高度变化，所有固定部件都靠
 * 「离底边的距离」定位。谁把某条改成 {@code top}，窗口一变高就会和 9x9 网格叠在一起。</p>
 */
class TerminalStyleDocumentTest {

    private static final String TEXTURE = "ae2_crafting_integration:textures/guis/pattern.png";
    private static final int CANVAS = 512;

    /** 三行物品列表时的界面高度：header 17 + 3 * 18 + bottom 276。{@code bottom} 都相对它换算。 */
    private static final int IMAGE_HEIGHT = 347;

    /** AE2 槽底：顶 1px 深线 + 16x15 底色。 */
    private static final int TOP_LINE = rgb(154, 159, 180);
    private static final int FILL = rgb(173, 176, 196);

    private static JsonObject style;
    private static BufferedImage texture;

    @BeforeAll
    static void loadStyle() throws Exception {
        try (InputStream in = TerminalStyleDocumentTest.class.getResourceAsStream(
                "/assets/ae2/screens/ae2_crafting_integration/pattern_encoding_terminal.json")) {
            assertNotNull(in, "终端 AEjson 必须随资源进 classpath");
            style = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
        try (InputStream in = TerminalStyleDocumentTest.class
                .getResourceAsStream("/assets/ae2_crafting_integration/textures/guis/pattern.png")) {
            assertNotNull(in, "textures/guis/pattern.png 缺失（外框会渲染成紫黑格）");
            texture = ImageIO.read(in);
        }
    }

    /**
     * 只 include AE2 的终端基类。槽位与按钮坐标全部写在本文件里，多 include 一层
     * {@code encoding/*.json} 会把 AE2 的 3x3 合成台、处理样板、锻造台位置重新灌进来，
     * 与本界面的 9x9 布局打架。
     */
    @Test
    void includesOnlyPullTheAe2TerminalBase() {
        JsonArray includes = style.getAsJsonArray("includes");
        assertNotNull(includes, "终端 AEjson 必须 include AE2 的终端基类");
        assertEquals(1, includes.size(), "只应有 terminals/terminal.json 一条");

        String resolved = URI.create("/screens/ae2_crafting_integration/" + includes.get(0).getAsString())
                .normalize().toString();
        assertEquals("/screens/terminals/terminal.json", resolved,
                "include 归化后必须指回 AE2 的 terminals 目录（多写一层 .. 会跑到 screens 根下）");
    }

    /**
     * 五片纵向切片必须和重绘后的贴图严格对齐。
     *
     * <p>{@code StyleManager}/{@code MEStorageScreen} 是逐片纵向拼背景的，切片高度决定行高：
     * header 17、三行物品列表各 18、bottom 从 y=71 起 276 高。改贴图时如果这几段错位，
     * 表现出来是「物品槽和背景差一行」。</p>
     */
    @Test
    void frameSlicesMatchTheReexportedTexture() {
        JsonObject terminalStyle = style.getAsJsonObject("terminalStyle");
        assertRect(terminalStyle, "header", 0, 0, 195, 17);
        assertRect(terminalStyle, "firstRow", 0, 17, 195, 18);
        assertRect(terminalStyle, "row", 0, 35, 195, 18);
        assertRect(terminalStyle, "lastRow", 0, 53, 195, 18);
        assertRect(terminalStyle, "bottom", 0, 71, 206, 276);

        assertEquals(3 * 18 + 17 + 276, IMAGE_HEIGHT, "切片高度之和必须等于换算用的界面高度");
    }

    /**
     * bottom 切片必须盖住贴图最右边那列不透明像素。
     *
     * <p>{@code MEStorageScreen#getScreenWidth} 取五片 {@code srcWidth} 的最大值，而右侧挂带
     * （x 168..205）只画在 bottom 里。宽度少写一像素，挂带右边那条描边就被切掉；多写一像素
     * 只会多出一列透明，但界面会无谓变宽、升级面板跟着往外挪——所以按贴图实测值锁死。</p>
     */
    @Test
    void bottomSliceCoversTheRightmostPaintedColumn() {
        int rightmost = -1;
        for (int y = 0; y < texture.getHeight(); y++) {
            for (int x = 0; x < texture.getWidth(); x++) {
                if ((texture.getRGB(x, y) >>> 24) != 0 && x > rightmost) {
                    rightmost = x;
                }
            }
        }
        assertTrue(rightmost > 0, "贴图整张透明？");

        JsonArray bottom = style.getAsJsonObject("terminalStyle")
                .getAsJsonObject("bottom").getAsJsonArray("srcRect");
        assertEquals(rightmost + 1, bottom.get(2).getAsInt(),
                "bottom 宽度必须盖到最右的不透明像素（x=" + rightmost + "）");
    }

    /** 贴图本身也得真在包里，且尺寸与声明一致；512 的画布必须显式声明，否则 UV 会按 256 归一化。 */
    @Test
    void textureIsPresentAtTheDeclaredCanvasSize() {
        assertEquals(CANVAS, texture.getWidth(), "pattern.png 宽度");
        assertEquals(CANVAS, texture.getHeight(), "pattern.png 高度");

        for (var entry : style.getAsJsonObject("terminalStyle").entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject slice = entry.getValue().getAsJsonObject();
            if (!slice.has("texture")) {
                continue;
            }
            assertEquals(TEXTURE, slice.get("texture").getAsString(), entry.getKey() + ".texture");
            assertEquals(CANVAS, slice.get("textureWidth").getAsInt(), entry.getKey() + ".textureWidth");
            assertEquals(CANVAS, slice.get("textureHeight").getAsInt(), entry.getKey() + ".textureHeight");
        }
    }

    /**
     * 固定部件一律用 {@code bottom} 定位。
     *
     * <p>终端的 {@code imageHeight} 会随窗口高度（物品行数）变化，写 {@code top} 的部件
     * 在窗口变高后会留在上面，被重复行切片压在 9x9 网格中间。AE2 自己的编码终端
     * （{@code encoding/crafting.json}）也是这个写法。</p>
     */
    @Test
    void fixedPartsAnchorToTheBottomEdge() {
        for (var entry : style.getAsJsonObject("slots").entrySet()) {
            JsonObject slot = entry.getValue().getAsJsonObject();
            assertTrue(slot.has("bottom"), entry.getKey() + " 必须用 bottom 定位");
            assertFalse(slot.has("top"), entry.getKey() + " 不应同时写 top");
        }
    }

    /**
     * 每个槽位的落点必须正好压在贴图画出来的槽框上，且 16x16 全部不透明。
     *
     * <p>右侧挂带那四个槽的口径是「槽位原点 = 16x16 内容区左上角」：18x18 的框取 +1，
     * 24x24 的框取 +4（居中）。这条不是猜的——AE2 自己的编码终端把 {@code BLANK_PATTERN}
     * 写在 (147,86)，而它贴图里那个框画在 (146,85)，正好差 1。</p>
     */
    @Test
    void slotsLandOnTheFramesPaintedInTheTexture() {
        JsonObject slots = style.getAsJsonObject("slots");
        assertSlotOnArt(slots, "ae2_crafting_integration:augmented_grid", 8, 260);
        assertSlotOnArt(slots, "ae2_crafting_integration:workstation", 179, 253);
        assertSlotOnArt(slots, "CRAFTING_RESULT", 179, 206);
        assertSlotOnArt(slots, "BLANK_PATTERN", 179, 172);
        assertSlotOnArt(slots, "ENCODED_PATTERN", 179, 125);
    }

    /**
     * 9x9 网格按 18px 步进铺开，末格的右缘必须正好落在右侧挂带的左边框上。
     *
     * <p>贴图里网格块是 {@code 9 * 16 + 8 * 2 = 160} 宽，而 {@code SlotGridLayout} 按 18
     * 步进需要 162：多出来的 2px 是槽的可点区域，不画东西。这条把「末格有没有画到挂带上」
     * 和「槽位原点有没有整体偏一格」都兜住了。</p>
     */
    @Test
    void gridFillsItsFrameAndStopsAtTheBand() {
        JsonObject grid = style.getAsJsonObject("slots")
                .getAsJsonObject("ae2_crafting_integration:augmented_grid");
        assertEquals("BREAK_AFTER_9COLS", grid.get("grid").getAsString());

        int left = grid.get("left").getAsInt();
        int top = IMAGE_HEIGHT - grid.get("bottom").getAsInt();
        int last = 8 * 18;
        assertSlotPixel(left + last, top + last, "网格末格");

        assertEquals(160, 9 * 16 + 8 * 2, "网格块 = 9 格 * 16px + 8 条 2px 缝");
        assertEquals(168, left + last + 16, "9x9 末格右缘应与右侧挂带的左边框重合");
    }

    /**
     * 编写样板按钮落在两个样板槽之间的空档里，且不与任何一方重叠。
     *
     * <p>新贴图没给按钮留框（AE2 的编码终端也一样，它是用 {@code ActionItems.ENCODE}
     * 的图标直接盖在底色上）。所以这里不校验「在框内」，改校验更硬的一条：按钮的
     * 16x16 判定区不能压到空白样板槽或样板输出槽的 18x18 判定区——压上了，点按钮和
     * 点槽就会打架。</p>
     */
    @Test
    void encodeButtonSitsBetweenThePatternSlots() {
        JsonObject button = style.getAsJsonObject("widgets").getAsJsonObject("encodePattern");
        int left = button.get("left").getAsInt();
        int top = IMAGE_HEIGHT - button.get("bottom").getAsInt();
        int width = button.get("width").getAsInt();
        int height = button.get("height").getAsInt();

        assertEquals(16, width);
        assertEquals(16, height);

        JsonObject slots = style.getAsJsonObject("slots");
        int blankTop = IMAGE_HEIGHT - slots.getAsJsonObject("BLANK_PATTERN").get("bottom").getAsInt();
        int encodedTop = IMAGE_HEIGHT - slots.getAsJsonObject("ENCODED_PATTERN").get("bottom").getAsInt();

        assertTrue(left >= 170 && left + width <= 206, "按钮超出右侧挂带");
        assertTrue(top >= blankTop + 18, "按钮压住了空白样板槽的判定区");
        assertTrue(top + height <= encodedTop, "按钮压住了样板输出槽的判定区");
    }

    /**
     * 标题走本模组自己的语言键。留着 {@code gui.ae2.Terminal} 的话，界面上会直接显示
     * AE2 的「终端」，和物品名对不上。
     */
    @Test
    void dialogTitleUsesOurOwnLangKey() throws Exception {
        String key = style.getAsJsonObject("text").getAsJsonObject("dialog_title")
                .getAsJsonObject("text").get("translate").getAsString();
        assertEquals("gui.ae2_crafting_integration.augmented_pattern_terminal", key);

        for (String lang : new String[] { "zh_cn", "en_us" }) {
            assertTrue(readLang(lang).has(key), lang + " 缺少 " + key);
        }
    }

    private static void assertSlotOnArt(JsonObject slots, String name, int expectedLeft, int expectedBottom) {
        JsonObject slot = slots.getAsJsonObject(name);
        assertNotNull(slot, "缺少槽位 " + name);
        assertEquals(expectedLeft, slot.get("left").getAsInt(), name + ".left");
        assertEquals(expectedBottom, slot.get("bottom").getAsInt(), name + ".bottom");
        assertSlotPixel(expectedLeft, IMAGE_HEIGHT - expectedBottom, name);
    }

    private static void assertSlotPixel(int x, int y, String label) {
        int pixel = texture.getRGB(x, y);
        assertTrue(pixel == TOP_LINE || pixel == FILL,
                label + " 落在 (" + x + "," + y + ") 的颜色是 " + Integer.toHexString(pixel)
                        + "，不是槽底（154,159,180 / 173,176,196）");
        for (int dy = 0; dy < 16; dy++) {
            for (int dx = 0; dx < 16; dx++) {
                int alpha = (texture.getRGB(x + dx, y + dy) >>> 24) & 0xFF;
                assertEquals(255, alpha, label + " 的 16x16 内 (" + dx + "," + dy + ") 是透明的");
            }
        }
    }

    private static void assertRect(JsonObject terminalStyle, String key, int x, int y, int width, int height) {
        JsonObject slice = terminalStyle.getAsJsonObject(key);
        assertNotNull(slice, "缺少切片 " + key);
        JsonArray rect = slice.getAsJsonArray("srcRect");
        assertEquals(List.of(x, y, width, height),
                List.of(rect.get(0).getAsInt(), rect.get(1).getAsInt(),
                        rect.get(2).getAsInt(), rect.get(3).getAsInt()),
                key + ".srcRect");
    }

    private static int rgb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static JsonObject readLang(String lang) throws Exception {
        String path = "/assets/ae2_crafting_integration/lang/" + lang + ".json";
        try (InputStream in = TerminalStyleDocumentTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " 不在 classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
