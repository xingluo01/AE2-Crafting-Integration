package io.github.lounode.ae2crafting.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 锁定 AEjson 与贴图画布尺寸的对应关系。
 *
 * <p>{@code textures/guis/molecular_assembler.png} 是 512×512，而 AE2 的 {@code Blitter}
 * 默认按 256×256 归一化 UV。漏写 {@code textureWidth/Height} 时界面只截到贴图左上一块，
 * 这类错误编译期看不出来，只能用断言挡。</p>
 */
class StyleDocumentTest {

    private static final int CANVAS = 512;

    private static JsonObject style;

    @BeforeAll
    static void loadStyle() throws Exception {
        try (InputStream in = StyleDocumentTest.class.getResourceAsStream(
                "/assets/ae2/screens/ae2_crafting_integration/molecular_assembler.json")) {
            assertNotNull(in, "AEjson 必须随资源进 classpath");
            style = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    @Test
    void everyBlitterDeclaresTheRealCanvasSize() {
        assertCanvas(style.getAsJsonObject("background"), "background");

        JsonObject images = style.getAsJsonObject("images");
        for (var entry : images.entrySet()) {
            assertCanvas(entry.getValue().getAsJsonObject(), "images." + entry.getKey());
        }
    }

    @Test
    void backgroundCoversTheMainPanelOnly() {
        assertRect(style.getAsJsonObject("background").getAsJsonArray("srcRect"), 0, 0, 176, 314);
    }

    @Test
    void sidePanelIsDrawnOutsideTheBackground() {
        JsonObject image = style.getAsJsonObject("images").getAsJsonObject("sidePanel");
        assertRect(image.getAsJsonArray("srcRect"), 176, 134, 34, 50);

        JsonObject widget = style.getAsJsonObject("widgets").getAsJsonObject("sidePanel");
        assertEquals(176, widget.get("left").getAsInt());
        assertEquals(134, widget.get("top").getAsInt());
    }

    @Test
    void progressBarUsesTheDedicatedSprite() {
        JsonObject image = style.getAsJsonObject("images").getAsJsonObject("progressBar");
        assertRect(image.getAsJsonArray("srcRect"), 176, 0, 18, 6);

        JsonObject widget = style.getAsJsonObject("widgets").getAsJsonObject("progressBar");
        assertEquals(181, widget.get("left").getAsInt());
        assertEquals(169, widget.get("top").getAsInt());
    }

    /**
     * 贴图里背包栏第一格的槽底（16×16、顶 1px 深线）仍保留在 AEjson 里作像素对照。
     *
     * <p><b>运行时已不再引用</b>：9x9 网格槽改成 {@code IOptionalSlot} 后，槽底由
     * {@code AEBaseScreen#renderBg} 用 {@code Icon.SLOT_BACKGROUND} 按 1.0 / 0.2 alpha 画，
     * 与 AE2 原版分子装配室一致。这条断言锁的是「贴图那块槽底还在原来的坐标上」，
     * 免得改贴图时把可用的备用 sprite 挪没了。</p>
     */
    @Test
    void gridSlotBackgroundReusesThePlayerInventorySlot() {
        JsonObject image = style.getAsJsonObject("images").getAsJsonObject("slotBackground");
        assertRect(image.getAsJsonArray("srcRect"), 8, 230, 16, 16);
    }

    /**
     * 槽位坐标与贴图槽底必须同基准：贴图顶部留白从 26px 收到 15px 后整组上移 11px，
     * JSON 漏改就会让物品画在槽底之外。右侧浮层下移 54px（浮层 80→134）后，
     * 产物槽跟着 87→141 走，槽位本身取框内居中 141+5=146。
     */
    @Test
    void slotsFollowTheTrimmedTopMargin() {
        JsonObject slots = style.getAsJsonObject("slots");
        assertEquals(20, slots.getAsJsonObject("ae2_crafting_integration:grid").get("top").getAsInt());
        assertEquals(198, slots.getAsJsonObject("ae2_crafting_integration:catalyst").get("top").getAsInt());
        assertEquals(146, slots.getAsJsonObject("ae2_crafting_integration:output").get("top").getAsInt());
        assertEquals(230, slots.getAsJsonObject("PLAYER_INVENTORY").get("top").getAsInt());
        assertEquals(288, slots.getAsJsonObject("PLAYER_HOTBAR").get("top").getAsInt());
    }

    /**
     * 左侧指南按钮与右侧升级面板的落点来自 {@code common.json} 的
     * {@code widgets.verticalToolbar} / {@code widgets.upgrades}。删掉这条 include 后
     * 界面照常打开，只是两个面板永远停在 (0,0)——编译期与运行期都不会报错。
     */
    @Test
    void includesAe2CommonWidgetsForToolbarAndUpgrades() {
        JsonArray includes = style.getAsJsonArray("includes");
        assertNotNull(includes, "装配室 AEjson 必须 include AE2 的 common.json");
        boolean found = false;
        for (var element : includes) {
            if ("../common/common.json".equals(element.getAsString())) {
                found = true;
            }
        }
        assertEquals(true, found, "includes 缺少 ../common/common.json（verticalToolbar 与 upgrades 的位置来源）");
    }

    /**
     * 升级面板照 AE 原样的落法：{@code Position.resolve} 对 {@code right} 只算
     * {@code imageWidth - right}（不减面板宽度），所以 {@code right:2} = 左缘 174，
     * 面板左侧 2px 压在主面板右边框上、其余 26px 挂在外面。
     *
     * <p>别改写成 {@code left}：{@code left:212} 会让面板整块飘在主面板右侧，
     * 与 AE2 各机器「半嵌在侧边」的观感不一致；网格最右列到 167 为止，174 不会压到槽位。</p>
     */
    @Test
    void upgradePanelEmbedsIntoTheRightEdgeLikeAe() {
        JsonObject widget = style.getAsJsonObject("widgets").getAsJsonObject("upgrades");
        assertEquals(2, widget.get("right").getAsInt());
        assertEquals(0, widget.get("top").getAsInt());
        assertEquals(false, widget.has("left"), "写了 left 就绕开 AE 的 right 语义，面板会整块挂到外面");
    }

    private static void assertCanvas(JsonObject blitter, String id) {
        assertEquals(CANVAS, blitter.get("textureWidth").getAsInt(), id + ".textureWidth 必须显式写 512");
        assertEquals(CANVAS, blitter.get("textureHeight").getAsInt(), id + ".textureHeight 必须显式写 512");
    }

    private static void assertRect(JsonArray rect, int x, int y, int w, int h) {
        assertEquals(x, rect.get(0).getAsInt(), "srcRect.x");
        assertEquals(y, rect.get(1).getAsInt(), "srcRect.y");
        assertEquals(w, rect.get(2).getAsInt(), "srcRect.w");
        assertEquals(h, rect.get(3).getAsInt(), "srcRect.h");
    }
}
