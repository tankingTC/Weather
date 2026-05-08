package com.example.weather.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class HolographicImageView extends AppCompatImageView {

    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private @ColorInt int glowStartColor = Color.argb(168, 255, 196, 98);
    private @ColorInt int glowEndColor = Color.argb(148, 110, 225, 255);

    public HolographicImageView(Context context) {
        super(context);
        init();
    }

    public HolographicImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HolographicImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.FIT_CENTER);
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(2f);
        sheenPaint.setStyle(Paint.Style.FILL);
    }

    public void setGlowPalette(@ColorInt int startColor, @ColorInt int endColor) {
        glowStartColor = startColor;
        glowEndColor = endColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        if (width > 0f && height > 0f) {
            float centerX = width * 0.5f;
            float centerY = height * 0.52f;
            float glowRadius = Math.max(width, height) * 0.48f;

            glowPaint.setShader(new RadialGradient(
                    centerX,
                    centerY,
                    glowRadius,
                    new int[]{glowStartColor, glowEndColor, Color.TRANSPARENT},
                    new float[]{0f, 0.58f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(centerX, centerY, glowRadius, glowPaint);

            accentPaint.setShader(new LinearGradient(
                    width * 0.18f,
                    height * 0.18f,
                    width * 0.82f,
                    height * 0.82f,
                    new int[]{Color.argb(182, 255, 255, 255), Color.argb(94, 255, 255, 255)},
                    null,
                    Shader.TileMode.CLAMP
            ));
            RectF border = new RectF(width * 0.1f, height * 0.1f, width * 0.9f, height * 0.9f);
            canvas.drawRoundRect(border, width * 0.28f, width * 0.28f, accentPaint);

            sheenPaint.setShader(new LinearGradient(
                    0f,
                    0f,
                    width,
                    height,
                    new int[]{
                            Color.argb(42, 255, 255, 255),
                            Color.argb(12, 255, 255, 255),
                            Color.argb(0, 255, 255, 255)
                    },
                    new float[]{0f, 0.46f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRoundRect(border, width * 0.28f, width * 0.28f, sheenPaint);
        }
        super.onDraw(canvas);
    }
}
