package com.example.weather.ui;

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
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeatherEffectOverlayView extends View {

    public enum Mode {
        CLEAR,
        CLOUDY,
        OVERCAST,
        RAIN,
        SNOW,
        STORM,
        NIGHT
    }

    private final Paint primaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ambientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random(24L);
    private Mode mode = Mode.CLEAR;
    private ValueAnimator animator;
    private float progress;
    private float depthOffsetX;
    private float depthOffsetY;

    public WeatherEffectOverlayView(Context context) {
        super(context);
        init();
    }

    public WeatherEffectOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeatherEffectOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        primaryPaint.setStyle(Paint.Style.FILL);
        secondaryPaint.setStyle(Paint.Style.FILL);
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStyle(Paint.Style.FILL);
        ambientPaint.setStyle(Paint.Style.FILL);
        detailPaint.setStyle(Paint.Style.FILL);
        ensureAnimator();
    }

    public void setMode(@NonNull Mode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            buildParticles(getWidth(), getHeight());
            invalidate();
        }
    }

    public void setDepthOffset(float x, float y) {
        depthOffsetX = x;
        depthOffsetY = y;
        invalidate();
    }

    private void ensureAnimator() {
        if (animator != null) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(4600L);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildParticles(w, h);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureAnimator();
        if (animator != null && !animator.isStarted()) {
            animator.start();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        switch (mode) {
            case CLEAR:
                drawClear(canvas);
                break;
            case CLOUDY:
                drawCloudy(canvas);
                break;
            case OVERCAST:
                drawOvercast(canvas);
                break;
            case RAIN:
                drawRain(canvas);
                break;
            case SNOW:
                drawSnow(canvas);
                break;
            case STORM:
                drawStorm(canvas);
                break;
            case NIGHT:
                drawNight(canvas);
                break;
        }
    }

    private void drawClear(Canvas canvas) {
        drawHorizonGlow(canvas,
                Color.argb(86, 255, 219, 190),
                Color.argb(36, 162, 196, 255),
                0.72f,
                0.93f);
        drawHorizonGlow(canvas,
                Color.argb(48, 255, 164, 202),
                Color.argb(18, 118, 222, 255),
                0.62f,
                0.85f);

        float cx = getWidth() * 0.78f + depthOffsetX * 0.32f;
        float cy = getHeight() * 0.17f + depthOffsetY * 0.2f;
        float radius = getWidth() * 0.16f;
        glowPaint.setShader(new RadialGradient(
                cx,
                cy,
                radius,
                new int[]{
                        Color.argb(168, 255, 214, 110),
                        Color.argb(116, 255, 132, 206),
                        Color.argb(82, 134, 223, 255),
                        Color.TRANSPARENT
                },
                new float[]{0f, 0.32f, 0.62f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius, glowPaint);

        primaryPaint.setShader(new RadialGradient(
                cx,
                cy,
                radius * 0.55f,
                new int[]{
                        Color.argb(235, 255, 248, 226),
                        Color.argb(186, 255, 207, 126),
                        Color.argb(64, 240, 166, 216),
                        Color.argb(10, 255, 214, 120)
                },
                new float[]{0f, 0.52f, 0.8f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius * 0.55f, primaryPaint);

        accentPaint.setShader(null);
        accentPaint.setColor(Color.argb(72, 255, 255, 255));
        accentPaint.setStrokeWidth(getWidth() * 0.0042f);
        canvas.drawCircle(cx, cy, radius * (0.78f + 0.04f * (float) Math.sin(progress * Math.PI * 2)), accentPaint);

        drawOrbit(canvas, cx, cy, radius * 0.92f, 0.08f);

        secondaryPaint.setColor(Color.argb(84, 255, 247, 237));
        for (Particle particle : particles) {
            float pulse = 0.45f + 0.55f * (float) Math.sin((progress + particle.seed) * Math.PI * 2);
            canvas.drawCircle(
                    particle.x + depthOffsetX * (0.15f + particle.seed * 0.08f),
                    particle.y + depthOffsetY * 0.12f,
                    particle.size * pulse,
                    secondaryPaint
            );
        }
        drawWaveBand(canvas, Color.argb(24, 255, 255, 255), 0.74f, 0.042f);
    }

    private void drawCloudy(Canvas canvas) {
        drawHorizonGlow(canvas,
                Color.argb(84, 248, 225, 196),
                Color.argb(34, 188, 206, 240),
                0.72f,
                0.94f);
        glowPaint.setShader(new RadialGradient(
                getWidth() * 0.68f + depthOffsetX * 0.16f,
                getHeight() * 0.2f + depthOffsetY * 0.08f,
                getWidth() * 0.18f,
                new int[]{Color.argb(72, 255, 191, 138), Color.argb(22, 255, 227, 188), Color.TRANSPARENT},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(getWidth() * 0.68f + depthOffsetX * 0.16f,
                getHeight() * 0.2f + depthOffsetY * 0.08f, getWidth() * 0.18f, glowPaint);
        for (Particle particle : particles) {
            float phase = (progress + particle.seed) % 1f;
            float drift = phase * getWidth() * 0.18f;
            float blurScale = 0.82f + particle.seed * 0.36f;
            drawGlassCloud(canvas,
                    particle.x - drift + depthOffsetX * (0.08f + particle.seed * 0.12f),
                    particle.y + depthOffsetY * 0.07f,
                    particle.size * blurScale,
                    particle.alpha
            );
        }
        drawWaveBand(canvas, Color.argb(30, 236, 244, 255), 0.8f, 0.03f);
    }

    private void drawOvercast(Canvas canvas) {
        drawHorizonGlow(canvas,
                Color.argb(58, 222, 232, 246),
                Color.argb(18, 126, 144, 178),
                0.74f,
                0.96f);
        for (Particle particle : particles) {
            float phase = (progress + particle.seed * 0.6f) % 1f;
            float drift = phase * getWidth() * 0.1f;
            float size = particle.size * (1.04f + particle.seed * 0.32f);
            drawMetalCloud(canvas,
                    particle.x - drift + depthOffsetX * (0.06f + particle.seed * 0.08f),
                    particle.y + depthOffsetY * 0.06f,
                    size,
                    particle.alpha
            );
        }
        drawWaveBand(canvas, Color.argb(20, 204, 220, 236), 0.82f, 0.024f);
    }

    private void drawRain(Canvas canvas) {
        drawHorizonGlow(canvas,
                Color.argb(54, 182, 217, 255),
                Color.argb(18, 112, 152, 212),
                0.73f,
                0.96f);
        float groundY = getHeight() * 0.845f + depthOffsetY * 0.08f;
        drawRainGroundPlane(canvas, groundY, false);
        drawRainField(canvas, false, groundY);
        drawGlassCloud(canvas,
                getWidth() * 0.76f + depthOffsetX * 0.22f,
                getHeight() * 0.17f + depthOffsetY * 0.08f,
                getWidth() * 0.12f,
                138
        );
        drawWaveBand(canvas, Color.argb(18, 118, 178, 255), 0.83f, 0.026f);
    }

    private void drawSnow(Canvas canvas) {
        drawHorizonGlow(canvas,
                Color.argb(86, 242, 250, 255),
                Color.argb(30, 186, 218, 255),
                0.77f,
                0.91f);
        drawGlassCloud(canvas,
                getWidth() * 0.72f + depthOffsetX * 0.14f,
                getHeight() * 0.16f + depthOffsetY * 0.08f,
                getWidth() * 0.1f,
                124
        );
        for (Particle particle : particles) {
            float y = (particle.y + progress * particle.speed * getHeight()) % (getHeight() + particle.size * 2.4f);
            float x = particle.x
                    + (float) Math.sin((progress + particle.seed) * Math.PI * 2) * 14f
                    + depthOffsetX * (0.18f + particle.seed * 0.12f);
            drawCrystal(canvas, x, y, particle.size * 0.8f);
        }
        drawWaveBand(canvas, Color.argb(34, 255, 255, 255), 0.84f, 0.024f);
    }

    private void drawStorm(Canvas canvas) {
        drawHorizonGlow(canvas,
                Color.argb(72, 104, 136, 255),
                Color.argb(26, 132, 238, 255),
                0.7f,
                0.96f);
        drawGlassCloud(canvas,
                getWidth() * 0.74f + depthOffsetX * 0.22f,
                getHeight() * 0.17f + depthOffsetY * 0.08f,
                getWidth() * 0.15f,
                150
        );
        drawGlassCloud(canvas,
                getWidth() * 0.56f + depthOffsetX * 0.18f,
                getHeight() * 0.21f + depthOffsetY * 0.06f,
                getWidth() * 0.1f,
                116
        );
        float groundY = getHeight() * 0.84f + depthOffsetY * 0.08f;
        drawRainGroundPlane(canvas, groundY, true);
        drawRainField(canvas, true, groundY);
        if (progress > 0.55f && progress < 0.61f) {
            drawLightning(canvas);
        }
        drawWaveBand(canvas, Color.argb(26, 166, 206, 255), 0.83f, 0.03f);
    }

    private void drawNight(Canvas canvas) {
        drawHorizonGlow(canvas,
                Color.argb(42, 251, 237, 198),
                Color.argb(18, 138, 165, 255),
                0.75f,
                0.95f);
        float cx = getWidth() * 0.82f + depthOffsetX * 0.26f;
        float cy = getHeight() * 0.16f + depthOffsetY * 0.12f;
        primaryPaint.setShader(new RadialGradient(
                cx,
                cy,
                getWidth() * 0.12f,
                new int[]{Color.argb(112, 255, 244, 214), Color.argb(30, 255, 244, 214), Color.TRANSPARENT},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, getWidth() * 0.12f, primaryPaint);
        secondaryPaint.setColor(Color.argb(210, 255, 255, 255));
        for (Particle particle : particles) {
            float flicker = 0.35f + 0.65f * (float) Math.sin((progress + particle.seed) * Math.PI * 2);
            secondaryPaint.setAlpha((int) (200 * flicker));
            canvas.drawCircle(
                    particle.x + depthOffsetX * 0.12f,
                    particle.y + depthOffsetY * 0.05f,
                    particle.size,
                    secondaryPaint
            );
        }
        drawWaveBand(canvas, Color.argb(20, 240, 220, 176), 0.83f, 0.022f);
    }

    private void drawGlassCloud(Canvas canvas, float centerX, float centerY, float size, int alpha) {
        int primary = Math.max(0, Math.min(255, alpha));
        int secondary = Math.max(0, Math.min(255, alpha - 36));
        primaryPaint.setShader(new RadialGradient(
                centerX,
                centerY,
                size,
                new int[]{
                        Color.argb(primary, 255, 255, 255),
                        Color.argb(secondary, 210, 232, 255),
                        Color.TRANSPARENT
                },
                new float[]{0f, 0.65f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX - size * 0.42f, centerY + size * 0.08f, size * 0.34f, primaryPaint);
        canvas.drawCircle(centerX, centerY - size * 0.04f, size * 0.46f, primaryPaint);
        canvas.drawCircle(centerX + size * 0.44f, centerY + size * 0.08f, size * 0.3f, primaryPaint);

        accentPaint.setShader(null);
        accentPaint.setColor(Color.argb(Math.max(0, alpha - 70), 255, 255, 255));
        accentPaint.setStrokeWidth(Math.max(1.6f, size * 0.02f));
        RectF shell = new RectF(centerX - size * 0.82f, centerY - size * 0.44f,
                centerX + size * 0.82f, centerY + size * 0.44f);
        canvas.drawArc(shell, 210f, 124f, false, accentPaint);
    }

    private void drawMetalCloud(Canvas canvas, float centerX, float centerY, float size, int alpha) {
        primaryPaint.setShader(new LinearGradient(
                centerX - size,
                centerY - size * 0.2f,
                centerX + size,
                centerY + size * 0.36f,
                new int[]{
                        Color.argb(alpha, 244, 248, 255),
                        Color.argb(Math.max(0, alpha - 18), 178, 190, 208),
                        Color.argb(Math.max(0, alpha - 36), 126, 138, 156)
                },
                new float[]{0f, 0.52f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX - size * 0.4f, centerY + size * 0.1f, size * 0.32f, primaryPaint);
        canvas.drawCircle(centerX, centerY - size * 0.06f, size * 0.44f, primaryPaint);
        canvas.drawCircle(centerX + size * 0.42f, centerY + size * 0.08f, size * 0.28f, primaryPaint);

        detailPaint.setStyle(Paint.Style.FILL);
        detailPaint.setColor(Color.argb(Math.max(0, alpha - 50), 78, 92, 112));
        canvas.drawOval(centerX - size * 0.56f, centerY + size * 0.26f,
                centerX + size * 0.56f, centerY + size * 0.42f, detailPaint);

        accentPaint.setShader(null);
        accentPaint.setColor(Color.argb(Math.max(0, alpha - 82), 255, 255, 255));
        accentPaint.setStrokeWidth(Math.max(1.2f, size * 0.018f));
        canvas.drawArc(new RectF(centerX - size * 0.74f, centerY - size * 0.4f,
                centerX + size * 0.74f, centerY + size * 0.38f), 218f, 108f, false, accentPaint);
    }

    private void buildParticles(int width, int height) {
        particles.clear();
        if (width <= 0 || height <= 0) {
            return;
        }
        int count;
        switch (mode) {
            case RAIN:
                count = 40;
                break;
            case STORM:
                count = 44;
                break;
            case SNOW:
                count = 26;
                break;
            case NIGHT:
                count = 18;
                break;
            case OVERCAST:
                count = 7;
                break;
            case CLOUDY:
                count = 8;
                break;
            default:
                count = 8;
                break;
        }
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    random.nextFloat() * width,
                    random.nextFloat() * height,
                    4f + random.nextFloat() * 16f,
                    0.28f + random.nextFloat() * 0.9f,
                    90 + random.nextInt(120),
                    random.nextFloat()
            ));
        }
    }

    private void drawRainField(Canvas canvas, boolean storm, float groundY) {
        accentPaint.setStrokeWidth(getWidth() * (storm ? 0.0036f : 0.003f));
        accentPaint.setShader(new LinearGradient(
                0f,
                0f,
                0f,
                getHeight(),
                new int[]{
                        Color.argb(0, 214, 236, 250),
                        Color.argb(storm ? 166 : 138, 214, 236, 250),
                        Color.argb(0, 214, 236, 250)
                },
                new float[]{0f, 0.38f, 1f},
                Shader.TileMode.CLAMP
        ));
        detailPaint.setStyle(Paint.Style.STROKE);
        for (Particle particle : particles) {
            float cycle = (progress * (1.05f + particle.speed) + particle.seed * 1.18f) % 1f;
            float laneShift = (particle.seed - 0.5f) * getWidth() * 0.08f;
            float x = particle.x + laneShift + depthOffsetX * (0.12f + particle.seed * 0.09f);
            float impactY = groundY + (particle.seed - 0.5f) * 18f;
            float startY = -getHeight() * (0.34f + particle.seed * 0.18f);
            float endY = impactY - particle.size * 1.2f;
            float y = startY + (endY - startY) * cycle;
            float tail = particle.size * (storm ? 5.4f : 4.6f);
            float slant = particle.size * (storm ? 1.28f : 1.04f);
            canvas.drawLine(x, y, x - slant, y + tail, accentPaint);

            if (cycle < 0.18f) {
                detailPaint.setStyle(Paint.Style.FILL);
                detailPaint.setColor(Color.argb(storm ? 118 : 92, 226, 246, 255));
                canvas.drawCircle(x - slant * 0.15f, y + tail * 0.18f, Math.max(1.8f, particle.size * 0.13f), detailPaint);
                detailPaint.setStyle(Paint.Style.STROKE);
            }

            float impactProgress = Math.max(0f, Math.min(1f, (cycle - 0.78f) / 0.22f));
            if (impactProgress > 0f) {
                drawRaindropImpact(canvas, x - slant * 0.32f, impactY, impactProgress, storm, particle);
            }
        }
        accentPaint.setShader(null);
        detailPaint.setStyle(Paint.Style.FILL);
    }

    private void drawRaindropImpact(Canvas canvas, float x, float y, float impactProgress,
                                    boolean storm, Particle particle) {
        float rippleRadius = particle.size * (1.1f + impactProgress * (storm ? 4.1f : 3.3f));
        float rippleHeight = rippleRadius * (storm ? 0.3f : 0.25f);
        int rippleAlpha = (int) ((storm ? 144 : 126) * (1f - impactProgress));
        detailPaint.setStyle(Paint.Style.STROKE);
        detailPaint.setStrokeWidth(Math.max(1.5f, particle.size * 0.18f));
        detailPaint.setColor(Color.argb(rippleAlpha, 210, 242, 255));
        canvas.drawOval(x - rippleRadius, y - rippleHeight, x + rippleRadius, y + rippleHeight, detailPaint);
        canvas.drawOval(
                x - rippleRadius * 0.56f,
                y - rippleHeight * 0.42f,
                x + rippleRadius * 0.56f,
                y + rippleHeight * 0.42f,
                detailPaint
        );

        float crownHeight = particle.size * (storm ? 1.18f : 0.92f) * (1f - impactProgress);
        detailPaint.setColor(Color.argb((int) ((storm ? 164 : 132) * (1f - impactProgress)), 226, 247, 255));
        canvas.drawLine(x, y - crownHeight * 0.1f, x - particle.size * 0.38f, y - crownHeight, detailPaint);
        canvas.drawLine(x, y - crownHeight * 0.08f, x + particle.size * 0.34f, y - crownHeight * 0.9f, detailPaint);
        canvas.drawLine(x, y - crownHeight * 0.14f, x, y - crownHeight * 1.18f, detailPaint);

        detailPaint.setStyle(Paint.Style.FILL);
        detailPaint.setColor(Color.argb((int) ((storm ? 112 : 88) * (1f - impactProgress)), 188, 232, 255));
        canvas.drawCircle(x, y - crownHeight * 0.56f, Math.max(1.3f, particle.size * 0.12f), detailPaint);
    }

    private void drawRainGroundPlane(Canvas canvas, float groundY, boolean storm) {
        glowPaint.setShader(new LinearGradient(
                0f,
                groundY - getHeight() * 0.06f,
                0f,
                getHeight(),
                new int[]{
                        Color.argb(0, 196, 230, 255),
                        Color.argb(storm ? 42 : 34, 184, 224, 255),
                        Color.argb(storm ? 62 : 48, 94, 164, 226)
                },
                new float[]{0f, 0.42f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0f, groundY - getHeight() * 0.02f, getWidth(), getHeight(), glowPaint);
        glowPaint.setShader(null);

        accentPaint.setShader(null);
        accentPaint.setColor(Color.argb(storm ? 96 : 82, 228, 246, 255));
        accentPaint.setStrokeWidth(getWidth() * 0.0024f);
        canvas.drawLine(0f, groundY, getWidth(), groundY, accentPaint);
    }

    private void drawCrystal(Canvas canvas, float centerX, float centerY, float size) {
        Path crystal = new Path();
        crystal.moveTo(centerX, centerY - size);
        crystal.lineTo(centerX + size * 0.72f, centerY);
        crystal.lineTo(centerX, centerY + size);
        crystal.lineTo(centerX - size * 0.72f, centerY);
        crystal.close();
        ambientPaint.setColor(Color.argb(170, 238, 248, 255));
        canvas.drawPath(crystal, ambientPaint);
        accentPaint.setShader(null);
        accentPaint.setColor(Color.argb(186, 255, 255, 255));
        accentPaint.setStrokeWidth(Math.max(1.2f, size * 0.18f));
        canvas.drawPath(crystal, accentPaint);
    }

    private void drawOrbit(Canvas canvas, float centerX, float centerY, float radius, float progressOffset) {
        accentPaint.setShader(null);
        accentPaint.setColor(Color.argb(42, 255, 255, 255));
        accentPaint.setStrokeWidth(getWidth() * 0.003f);
        RectF orbit = new RectF(centerX - radius, centerY - radius * 0.72f,
                centerX + radius, centerY + radius * 0.72f);
        canvas.drawOval(orbit, accentPaint);
        float angle = (progress + progressOffset) * 360f;
        float orbitX = (float) (centerX + Math.cos(Math.toRadians(angle)) * radius);
        float orbitY = (float) (centerY + Math.sin(Math.toRadians(angle)) * radius * 0.72f);
        detailPaint.setColor(Color.argb(180, 255, 245, 225));
        canvas.drawCircle(orbitX, orbitY, getWidth() * 0.008f, detailPaint);
    }

    private void drawHorizonGlow(Canvas canvas, int glowColor, int lowerColor, float centerYRatio, float bottomRatio) {
        float centerY = getHeight() * centerYRatio + depthOffsetY * 0.1f;
        glowPaint.setShader(new RadialGradient(
                getWidth() * 0.5f,
                centerY,
                getWidth() * 0.68f,
                new int[]{glowColor, lowerColor, Color.TRANSPARENT},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0f, getHeight() * 0.34f, getWidth(), getHeight() * bottomRatio, glowPaint);
        glowPaint.setShader(null);
    }

    private void drawWaveBand(Canvas canvas, int color, float baseHeightRatio, float amplitudeRatio) {
        Path path = new Path();
        float baseY = getHeight() * baseHeightRatio + depthOffsetY * 0.08f;
        float amplitude = getHeight() * amplitudeRatio;
        path.moveTo(0f, getHeight());
        path.lineTo(0f, baseY);
        for (int i = 0; i <= 6; i++) {
            float x = getWidth() * i / 6f;
            float y = (float) (baseY + Math.sin((progress * Math.PI * 2) + i * 0.9f) * amplitude);
            path.lineTo(x, y);
        }
        path.lineTo(getWidth(), getHeight());
        path.close();
        ambientPaint.setColor(color);
        canvas.drawPath(path, ambientPaint);
    }

    private void drawLightning(Canvas canvas) {
        Path bolt = new Path();
        float startX = getWidth() * 0.72f + depthOffsetX * 0.14f;
        float startY = getHeight() * 0.08f;
        bolt.moveTo(startX, startY);
        bolt.lineTo(startX - getWidth() * 0.04f, startY + getHeight() * 0.1f);
        bolt.lineTo(startX + getWidth() * 0.01f, startY + getHeight() * 0.1f);
        bolt.lineTo(startX - getWidth() * 0.06f, startY + getHeight() * 0.24f);
        bolt.lineTo(startX + getWidth() * 0.02f, startY + getHeight() * 0.14f);
        bolt.lineTo(startX - getWidth() * 0.015f, startY + getHeight() * 0.14f);
        bolt.close();
        ambientPaint.setColor(Color.argb(230, 233, 247, 255));
        canvas.drawPath(bolt, ambientPaint);
    }

    private static class Particle {
        final float x;
        final float y;
        final float size;
        final float speed;
        final int alpha;
        final float seed;

        Particle(float x, float y, float size, float speed, int alpha, float seed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.alpha = alpha;
            this.seed = seed;
        }
    }
}
