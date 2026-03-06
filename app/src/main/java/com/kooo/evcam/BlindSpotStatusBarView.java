package com.kooo.evcam;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * 补盲悬浮窗状态栏 — 纯动效方向指示视图（无文字）。
 * <p>
 * 支持 5 种动效样式 + 关闭模式，通过 {@link #setAnimationStyle(int)} 切换。
 * <ul>
 *   <li>STYLE_SEQUENTIAL — 序贯灯段（类奥迪动态转向灯）</li>
 *   <li>STYLE_COMET — 流光彗尾（流星拖尾效果）</li>
 *   <li>STYLE_RIPPLE — 波纹扩散（水波纹水平传播）</li>
 *   <li>STYLE_GRADIENT_FILL — 呼吸渐变填充（类 iPhone 充电动画）</li>
 *   <li>STYLE_ARROW_RIPPLE — 箭头涟漪（箭头从中心向外飞出）</li>
 * </ul>
 */
public class BlindSpotStatusBarView extends View {

    public static final int STYLE_OFF = 0;
    public static final int STYLE_SEQUENTIAL = 1;
    public static final int STYLE_COMET = 2;
    public static final int STYLE_RIPPLE = 3;
    public static final int STYLE_GRADIENT_FILL = 4;
    public static final int STYLE_ARROW_RIPPLE = 5;

    private int animationStyle = STYLE_SEQUENTIAL;
    private String direction = "";

    private ValueAnimator flowAnimator;
    private ValueAnimator pulseAnimator;
    private ValueAnimator fadeAnimator;

    private float flowPhase = 0f;
    private float pulseValue = 1f;
    private float dirAlpha = 0f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chevronPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint idlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path chevronPath = new Path();
    private final RectF rectF = new RectF();

    private float dp;

    public BlindSpotStatusBarView(Context context) {
        super(context);
        init();
    }

    public BlindSpotStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlindSpotStatusBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        dp = getResources().getDisplayMetrics().density;

        chevronPaint.setStyle(Paint.Style.STROKE);
        chevronPaint.setStrokeWidth(2.5f * dp);
        chevronPaint.setStrokeCap(Paint.Cap.ROUND);
        chevronPaint.setStrokeJoin(Paint.Join.ROUND);

        glowPaint.setStyle(Paint.Style.FILL);
        idlePaint.setStyle(Paint.Style.FILL);

        flowAnimator = ValueAnimator.ofFloat(0f, 1f);
        flowAnimator.setDuration(1500);
        flowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flowAnimator.setInterpolator(new LinearInterpolator());
        flowAnimator.addUpdateListener(a -> {
            flowPhase = (float) a.getAnimatedValue();
            invalidate();
        });

        pulseAnimator = ValueAnimator.ofFloat(0.7f, 1f);
        pulseAnimator.setDuration(900);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(a -> pulseValue = (float) a.getAnimatedValue());
    }

    public void setAnimationStyle(int style) {
        if (style == animationStyle) return;
        animationStyle = style;
        updateFlowDuration();
        invalidate();
    }

    public int getAnimationStyle() {
        return animationStyle;
    }

    private void updateFlowDuration() {
        long duration;
        switch (animationStyle) {
            case STYLE_SEQUENTIAL:    duration = 1200; break;
            case STYLE_COMET:         duration = 2000; break;
            case STYLE_RIPPLE:        duration = 1600; break;
            case STYLE_GRADIENT_FILL: duration = 1800; break;
            case STYLE_ARROW_RIPPLE:  duration = 1400; break;
            default:                  duration = 1500; break;
        }
        flowAnimator.setDuration(duration);
    }

    public void setDirection(String dir) {
        if (dir == null) dir = "";
        if (dir.equals(direction)) return;
        direction = dir;

        float target = dir.isEmpty() ? 0f : 1f;

        if (fadeAnimator != null) fadeAnimator.cancel();
        fadeAnimator = ValueAnimator.ofFloat(dirAlpha, target);
        fadeAnimator.setDuration(350);
        fadeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeAnimator.addUpdateListener(a -> {
            dirAlpha = (float) a.getAnimatedValue();
            invalidate();
        });
        if (target == 0f) {
            fadeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator a) {
                    if (direction.isEmpty()) {
                        flowAnimator.cancel();
                        pulseAnimator.cancel();
                    }
                }
            });
        }
        fadeAnimator.start();

        if (!dir.isEmpty()) {
            if (!flowAnimator.isRunning()) flowAnimator.start();
            if (!pulseAnimator.isRunning()) pulseAnimator.start();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        if (dirAlpha < 0.01f) {
            drawIdleState(canvas, w, h);
            return;
        }

        switch (animationStyle) {
            case STYLE_SEQUENTIAL:
                drawSequential(canvas, w, h);
                break;
            case STYLE_COMET:
                drawComet(canvas, w, h);
                break;
            case STYLE_RIPPLE:
                drawRipple(canvas, w, h);
                break;
            case STYLE_GRADIENT_FILL:
                drawGradientFill(canvas, w, h);
                break;
            case STYLE_ARROW_RIPPLE:
                drawArrowRipple(canvas, w, h);
                break;
            default:
                drawIdleState(canvas, w, h);
                break;
        }
    }

    // ==================== A: Sequential Segments ====================

    private void drawSequential(Canvas canvas, int w, int h) {
        boolean left = "left".equals(direction);
        int segCount = 7;
        float segWidth = w / (float) segCount;
        float gap = 1.5f * dp;
        float cornerR = 3 * dp;
        float pad = 3 * dp;

        float litCount;
        float fadeMul = 1f;
        if (flowPhase < 0.55f) {
            litCount = (flowPhase / 0.55f) * segCount;
        } else if (flowPhase < 0.65f) {
            litCount = segCount;
        } else {
            litCount = segCount;
            fadeMul = 1f - smoothStep((flowPhase - 0.65f) / 0.35f);
        }

        for (int i = 0; i < segCount; i++) {
            int drawIdx = left ? (segCount - 1 - i) : i;
            float x = drawIdx * segWidth + gap / 2;
            float x2 = x + segWidth - gap;

            float segAlpha;
            if (i < (int) litCount) {
                segAlpha = 1f;
            } else if (i == (int) litCount) {
                segAlpha = litCount - (int) litCount;
            } else {
                segAlpha = 0f;
            }
            segAlpha *= fadeMul * dirAlpha;
            if (segAlpha < 0.01f) continue;

            int bodyAlpha = (int) (180 * segAlpha * pulseValue);
            fillPaint.setColor(Color.argb(bodyAlpha, 255, 183, 77));
            rectF.set(x, pad, x2, h - pad);
            canvas.drawRoundRect(rectF, cornerR, cornerR, fillPaint);

            int hlAlpha = (int) (80 * segAlpha * pulseValue);
            fillPaint.setColor(Color.argb(hlAlpha, 255, 225, 150));
            rectF.set(x + dp, pad, x2 - dp, pad + 2.5f * dp);
            canvas.drawRoundRect(rectF, 1.5f * dp, 1.5f * dp, fillPaint);
        }
    }

    // ==================== B: Comet Trails ====================

    private void drawComet(Canvas canvas, int w, int h) {
        boolean left = "left".equals(direction);
        int cometCount = 3;

        for (int i = 0; i < cometCount; i++) {
            float phase = (flowPhase + i / (float) cometCount) % 1f;

            float easedPhase = 1f - (1f - phase) * (1f - phase);
            float headX = left ? w * (1f - easedPhase) : w * easedPhase;
            float tailLen = w * 0.35f;

            float alpha = (float) Math.sin(phase * Math.PI);
            alpha *= alpha;
            alpha *= dirAlpha * pulseValue;
            if (alpha < 0.01f) continue;

            float tailX = left ? headX + tailLen : headX - tailLen;
            int headColor = Color.argb((int) (160 * alpha), 255, 210, 100);
            fillPaint.setShader(new LinearGradient(
                    headX, 0, tailX, 0,
                    headColor, Color.TRANSPARENT,
                    Shader.TileMode.CLAMP));
            canvas.drawRect(Math.min(headX, tailX), 0, Math.max(headX, tailX), h, fillPaint);
            fillPaint.setShader(null);

            float glowR = 10 * dp;
            glowPaint.setShader(new RadialGradient(headX, h / 2f, glowR,
                    Color.argb((int) (200 * alpha), 255, 235, 160),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(headX, h / 2f, glowR, glowPaint);
            glowPaint.setShader(null);
        }
    }

    // ==================== C: Ripple Waves ====================

    private void drawRipple(Canvas canvas, int w, int h) {
        boolean left = "left".equals(direction);
        int waveCount = 4;

        int baseAlpha = (int) (30 * dirAlpha * pulseValue);
        float gradEnd = w * 0.3f;
        fillPaint.setShader(new LinearGradient(
                left ? 0 : w, 0, left ? gradEnd : w - gradEnd, 0,
                Color.argb(baseAlpha, 255, 183, 77), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, fillPaint);
        fillPaint.setShader(null);

        for (int i = 0; i < waveCount; i++) {
            float phase = (flowPhase + i / (float) waveCount) % 1f;

            float travelDist = w * phase;
            float waveX = left ? travelDist : w - travelDist;

            float distFade = 1f - phase;
            distFade *= distFade;
            float alpha = distFade * dirAlpha * pulseValue;
            if (alpha < 0.02f) continue;

            float bandWidth = 22 * dp;
            int centerAlpha = (int) (150 * alpha);

            fillPaint.setShader(new LinearGradient(
                    waveX - bandWidth / 2, 0, waveX + bandWidth / 2, 0,
                    new int[]{Color.TRANSPARENT,
                            Color.argb(centerAlpha, 255, 183, 77),
                            Color.TRANSPARENT},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(waveX - bandWidth / 2, 0, waveX + bandWidth / 2, h, fillPaint);
            fillPaint.setShader(null);
        }
    }

    // ==================== D: Gradient Fill ====================

    private void drawGradientFill(Canvas canvas, int w, int h) {
        boolean left = "left".equals(direction);

        float fillProgress;
        float fadeOut = 1f;
        if (flowPhase < 0.65f) {
            float t = flowPhase / 0.65f;
            fillProgress = 1f - (1f - t) * (1f - t) * (1f - t);
        } else {
            fillProgress = 1f;
            float t = (flowPhase - 0.65f) / 0.35f;
            fadeOut = 1f - t * t;
        }

        float edgeX = left ? w * (1f - fillProgress) : w * fillProgress;
        float startX = left ? w : 0;

        int fillAlpha = (int) (120 * dirAlpha * pulseValue * fadeOut);
        float l = Math.min(startX, edgeX);
        float r = Math.max(startX, edgeX);
        if (r - l > 1) {
            fillPaint.setShader(new LinearGradient(
                    startX, 0, edgeX, 0,
                    Color.argb((int) (fillAlpha * 0.4f), 255, 183, 77),
                    Color.argb(fillAlpha, 255, 183, 77),
                    Shader.TileMode.CLAMP));
            canvas.drawRect(l, 0, r, h, fillPaint);
            fillPaint.setShader(null);
        }

        if (fillProgress > 0.01f && fillProgress < 1f && fadeOut > 0.5f) {
            float edgeGlowR = 15 * dp;
            float glowAlpha = (float) Math.sin(fillProgress * Math.PI) * dirAlpha * fadeOut;

            glowPaint.setShader(new RadialGradient(edgeX, h / 2f, edgeGlowR,
                    Color.argb((int) (180 * glowAlpha), 255, 225, 140),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(edgeX, h / 2f, edgeGlowR, glowPaint);
            glowPaint.setShader(null);

            float lineW = 3 * dp;
            int lineAlpha = (int) (200 * glowAlpha);
            fillPaint.setShader(new LinearGradient(
                    edgeX - lineW, 0, edgeX + lineW, 0,
                    new int[]{Color.TRANSPARENT,
                            Color.argb(lineAlpha, 255, 220, 130),
                            Color.TRANSPARENT},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(edgeX - lineW, 0, edgeX + lineW, h, fillPaint);
            fillPaint.setShader(null);
        }
    }

    // ==================== E: Arrow Ripples ====================

    private void drawArrowRipple(Canvas canvas, int w, int h) {
        boolean left = "left".equals(direction);
        float cy = h / 2f;
        int arrowCount = 4;
        float baseChevronH = 9 * dp;
        float baseChevronW = 6 * dp;

        int baseAlpha = (int) (20 * dirAlpha * pulseValue);
        fillPaint.setShader(new LinearGradient(
                left ? 0 : w, 0, left ? w : 0, 0,
                Color.argb(baseAlpha, 255, 183, 77), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, fillPaint);
        fillPaint.setShader(null);

        float centerX = w / 2f;
        float travelDist = w * 0.55f;

        for (int i = 0; i < arrowCount; i++) {
            float phase = (flowPhase + i / (float) arrowCount) % 1f;

            float offset = travelDist * phase;
            float cx = left ? centerX - offset : centerX + offset;

            float scale = 0.6f + 0.5f * phase;
            float curH = baseChevronH * scale;
            float curW = baseChevronW * scale;

            float alpha = (float) Math.sin(phase * Math.PI);
            alpha *= dirAlpha * pulseValue;
            if (alpha < 0.02f) continue;

            if (alpha > 0.2f) {
                float glowR = curH * 2f;
                glowPaint.setShader(new RadialGradient(cx, cy, glowR,
                        Color.argb((int) (45 * alpha), 255, 183, 77),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP));
                canvas.drawCircle(cx, cy, glowR, glowPaint);
                glowPaint.setShader(null);
            }

            int a = clamp((int) (220 * alpha));
            chevronPaint.setColor(Color.argb(a, 255, 183, 77));
            chevronPaint.setStrokeWidth(2.5f * dp * Math.min(scale, 1f));

            chevronPath.reset();
            if (left) {
                chevronPath.moveTo(cx + curW, cy - curH);
                chevronPath.lineTo(cx - curW, cy);
                chevronPath.lineTo(cx + curW, cy + curH);
            } else {
                chevronPath.moveTo(cx - curW, cy - curH);
                chevronPath.lineTo(cx + curW, cy);
                chevronPath.lineTo(cx - curW, cy + curH);
            }
            canvas.drawPath(chevronPath, chevronPaint);
        }
    }

    // ==================== Idle state ====================

    private void drawIdleState(Canvas canvas, int w, int h) {
        idlePaint.setColor(Color.argb(50, 176, 176, 176));
        canvas.drawCircle(w / 2f, h / 2f, 2.5f * dp, idlePaint);
    }

    // ==================== Utilities ====================

    private static float smoothStep(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (flowAnimator != null) flowAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (fadeAnimator != null) fadeAnimator.cancel();
    }
}
