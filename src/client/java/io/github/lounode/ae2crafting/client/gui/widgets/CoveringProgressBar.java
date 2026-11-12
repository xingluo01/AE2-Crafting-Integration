package io.github.lounode.ae2crafting.client.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ProgressBar;
import appeng.menu.interfaces.IProgressProvider;

/**
 * 「逐渐覆盖」式进度条。
 *
 * <p>AE2 的 {@link ProgressBar} 对水平方向算的是
 * {@code srcX += width - filled; srcW = filled}：取贴图的<b>右侧</b>一段画到固定的左端。
 * 贴图的进度条是一排有方向的竖纹，进度变化时露出的纹路会跟着往左滑，看上去像整条在平移，
 * 而不是从左往右长出来。这里保持 {@code srcX} 不动、只把 {@code srcWidth} 裁成 {@code filled}，
 * 让纹路钉在原处、按进度逐格露出。</p>
 *
 * <p>tooltip 不在这里改：{@link ProgressBar#setFullMsg} 由界面每帧写入百分比，
 * 与 AE2 原版分子装配室的做法一致。</p>
 */
public class CoveringProgressBar extends ProgressBar {

    private final IProgressProvider source;
    private final Blitter blitter;
    private final int srcX;
    private final int srcY;
    private final int srcWidth;
    private final int srcHeight;

    public CoveringProgressBar(IProgressProvider source, Blitter blitter, Direction dir) {
        super(source, blitter, dir);
        this.source = source;
        this.blitter = blitter.copy();
        this.srcX = blitter.getSrcX();
        this.srcY = blitter.getSrcY();
        this.srcWidth = blitter.getSrcWidth();
        this.srcHeight = blitter.getSrcHeight();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int max = source.getMaxProgress();
        int current = Math.min(source.getCurrentProgress(), max);
        int filled = max > 0 ? srcWidth * current / max : 0;
        if (filled <= 0) {
            return;
        }
        blitter.src(srcX, srcY, filled, srcHeight)
                .dest(getX(), getY())
                .blit(graphics);
    }
}
