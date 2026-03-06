package com.kooo.evcam;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * 自动适配宽高比的 TextureView
 * 根据设置的宽高比自动调整视图尺寸，避免画面拉伸
 */
public class AutoFitTextureView extends TextureView {

    private int ratioWidth = 0;
    private int ratioHeight = 0;
    private boolean fillContainer = false;
    private boolean useExactLayoutParams = false;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 设置此视图的宽高比
     *
     * @param width  相对宽度
     * @param height 相对高度
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    /**
     * 设置是否填满容器
     *
     * @param fill true=填满容器（可能裁切），false=适应容器（可能有黑边）
     */
    public void setFillContainer(boolean fill) {
        this.fillContainer = fill;
        requestLayout();
    }

    /**
     * 轮胎模式下使用精确 LayoutParams 尺寸，跳过宽高比自动调整。
     * 避免 onMeasure 覆盖我们设置的尺寸导致缩放计算错误。
     */
    public void setUseExactLayoutParams(boolean exact) {
        this.useExactLayoutParams = exact;
        requestLayout();
    }

    /**
     * 根据旋转角度设置宽高比
     * 当旋转角度为90°或270°时，会自动交换宽高
     *
     * @param width    原始宽度
     * @param height   原始高度
     * @param rotation 旋转角度（0, 90, 180, 270）
     */
    public void setAspectRatioWithRotation(int width, int height, int rotation) {
        if (rotation == 90 || rotation == 270) {
            // 旋转90°或270°时，宽高互换
            setAspectRatio(height, width);
        } else {
            setAspectRatio(width, height);
        }
    }

    /**
     * 获取当前设置的宽高比
     * @return 宽高比（宽/高），如果未设置返回0
     */
    public float getAspectRatio() {
        if (ratioWidth == 0 || ratioHeight == 0) {
            return 0f;
        }
        return (float) ratioWidth / ratioHeight;
    }

    /**
     * 获取当前设置的比例宽度
     */
    public int getRatioWidth() {
        return ratioWidth;
    }

    /**
     * 获取当前设置的比例高度
     */
    public int getRatioHeight() {
        return ratioHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (useExactLayoutParams) {
            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            if (lp != null && lp.width > 0 && lp.height > 0) {
                setMeasuredDimension(lp.width, lp.height);
                return;
            }
        }

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height);
        } else {
            // 根据宽高比调整尺寸
            int newWidth, newHeight;

            // 方案1：基于容器宽度计算高度
            newWidth = width;
            newHeight = width * ratioHeight / ratioWidth;

            if (fillContainer) {
                // 填满模式：如果高度不足，放大以填满容器
                if (newHeight < height) {
                    newHeight = height;
                    newWidth = height * ratioWidth / ratioHeight;
                }
            } else {
                // 适应模式：如果高度超出，缩小以适应容器
                if (newHeight > height) {
                    newHeight = height;
                    newWidth = height * ratioWidth / ratioHeight;
                }
            }

            setMeasuredDimension(newWidth, newHeight);
        }
    }
}
