package com.example.midtermproject.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;

import com.example.midtermproject.R;
import com.example.midtermproject.model.DisplayState;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotKind;
import com.example.midtermproject.model.ZoneType;

/**
 * Vẽ từng chỗ theo trạng thái hiển thị (REQUIREMENTS 6.3), trong tọa độ sơ đồ (world units).
 * <p>
 * Mọi Paint, Path, RectF và mảng tạm được tạo một lần trong constructor, nên {@link #draw} không
 * cấp phát gì (yêu cầu hiệu năng của đề). Bề dày nét và cỡ vòng đánh dấu được tính theo pixel màn
 * hình: gọi {@link #beginFrame} một lần mỗi khung hình để báo tỉ lệ hiện tại.
 * <p>
 * Mỗi trạng thái khác nhau bằng nhiều kênh, không chỉ bằng màu (vẽ theo mẫu G, Q48): ghế trống là hình đặc màu
 * khu; đang chọn có vòng mực đậm và dấu ✓; vé của bạn màu xanh lá có ★; đã đặt màu be có ✕; bị khóa là nền kem
 * viền mực có gạch chéo. Mã chỗ và các ký hiệu luôn đứng thẳng trên màn hình dù sơ đồ xoay
 * (vẽ ngược chiều góc xoay quanh tâm chỗ), nếu không khi dọc chữ sẽ nằm ngang sang một bên.
 */
public final class SpotRenderer {
    /** Độ trong suốt (0-255) của chỗ ngoài bộ lọc: khoảng 22%. */
    private static final int DIMMED_ALPHA = 56;
        /** Chỗ có đường kính trên màn hình từ ngưỡng này trở lên mới vẽ mã chỗ và ký hiệu. */
    private static final float DETAIL_MIN_DIAMETER_DP = 22f;
    /**
     * Gạch chéo của chỗ bị khóa hiện từ ngưỡng nhỏ hơn: chỉ có 8 chỗ khóa, và nếu thiếu gạch chéo
     * thì ở mức Fit chúng chỉ là vòng trắng, dễ nhầm với chỗ rỗng.
     */
    private static final float LOCK_SLASH_MIN_DIAMETER_DP = 9f;

    private final float density;
    private final float detailMinDiameterPx;
    private final float lockSlashMinDiameterPx;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final RectF rect = new RectF();
    private final Path star = new Path();
    private final float[] lines = new float[8];

    // Màu theo khu, đánh chỉ số bằng ZoneType.ordinal().
    private final int[] zoneFill = new int[3];
    private final int[] onZone = new int[3];
    private final int selected, onSelected, mine, onMine;
    private final int booked, onBooked;
    private final int lockedFill, lockedStroke;
    private final int focusHalo, focusInk;

    /** Một pixel màn hình bằng bao nhiêu world units ở tỉ lệ hiện tại (1 / scale). */
    private float pixel = 1f;
    private float scale = 1f;
    /** Góc (độ) cần xoay ngược lại để chữ và ký hiệu đứng thẳng trên màn hình. */
    private float uprightDegrees;
    /** 0 đến 1: độ "nhấp nháy" của vòng chỗ đang xem khi vừa được định vị; 0 là vòng bình thường. */
    private float pulse;
    private boolean dimmed;

    public SpotRenderer(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        detailMinDiameterPx = DETAIL_MIN_DIAMETER_DP * density;
        lockSlashMinDiameterPx = LOCK_SLASH_MIN_DIAMETER_DP * density;

        zoneFill[ZoneType.STANDING.ordinal()] = color(context, R.color.zone_standing);
        onZone[ZoneType.STANDING.ordinal()] = color(context, R.color.on_zone_standing);
        zoneFill[ZoneType.VIP.ordinal()] = color(context, R.color.zone_vip);
        onZone[ZoneType.VIP.ordinal()] = color(context, R.color.on_zone_vip);
        zoneFill[ZoneType.STANDARD.ordinal()] = color(context, R.color.zone_standard);
        onZone[ZoneType.STANDARD.ordinal()] = color(context, R.color.on_zone_standard);

        selected = color(context, R.color.spot_selected);
        onSelected = color(context, R.color.on_spot_selected);
        mine = color(context, R.color.spot_mine);
        onMine = color(context, R.color.on_spot_mine);
        booked = color(context, R.color.spot_booked);
        onBooked = color(context, R.color.on_spot_booked);
        lockedFill = color(context, R.color.spot_locked_fill);
        lockedStroke = color(context, R.color.spot_locked_stroke);
        focusHalo = color(context, R.color.focus_ring_halo);
        focusInk = color(context, R.color.focus_ring);

        glyph.setStyle(Paint.Style.STROKE);
        glyph.setStrokeCap(Paint.Cap.ROUND);
        glyph.setStrokeJoin(Paint.Join.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        buildUnitStar();
    }

    /**
     * Gọi một lần mỗi khung hình, trước khi vẽ các chỗ.
     *
     * @param angleDegrees góc xoay hiện tại của sơ đồ, theo chiều kim đồng hồ
     * @param pulse        độ nhấp nháy của vòng chỗ đang xem, từ 0 đến 1 (xem {@link #drawFocusRing})
     */
    public void beginFrame(float scale, float angleDegrees, float pulse) {
        this.scale = scale;
        this.pixel = 1f / scale;
        this.uprightDegrees = -angleDegrees;
        this.pulse = pulse;
    }

    /** Chỗ bán kính {@code radiusWorld} có đủ to trên màn hình để vẽ mã chỗ và ký hiệu không. */
    public boolean showsDetails(float radiusWorld) {
        return 2f * radiusWorld * scale >= detailMinDiameterPx;
    }

    /**
     * Vẽ một chỗ.
     *
     * @param dimmed  chỗ nằm ngoài bộ lọc: vẽ mờ nhưng giữ nguyên hình dạng và trạng thái
     * @param focused chỗ đang xem: thêm vòng đôi bên ngoài
     */
    public void draw(Canvas canvas, Spot spot, DisplayState state, boolean dimmed, boolean focused) {
        this.dimmed = dimmed;
        final boolean detail = showsDetails(spot.size);
        switch (state) {
            case SELECTED:
                drawSelected(canvas, spot, detail);
                break;
            case MINE:
                drawMine(canvas, spot, detail);
                break;
            case BOOKED:
                drawBooked(canvas, spot, detail);
                break;
            case LOCKED:
                drawLocked(canvas, spot, detail, 2f * spot.size * scale >= lockSlashMinDiameterPx);
                break;
            default:
                drawAvailable(canvas, spot, detail);
                break;
        }
        if (focused) {
            drawFocusRing(canvas, spot);
        }
    }

    private void drawAvailable(Canvas canvas, Spot spot, boolean detail) {
        final int zone = spot.zone.ordinal();
        // Mẫu G: ghế trống chỉ là hình đặc màu khu, không viền.
        shape(canvas, spot, spot.size, fillPaint(zoneFill[zone]));
        if (detail) {
            drawLabel(canvas, spot, onZone[zone]);
        }
    }

    private void drawSelected(Canvas canvas, Spot spot, boolean detail) {
        drawRingAround(canvas, spot, selected);
        shape(canvas, spot, spot.size, fillPaint(selected));
        if (detail) {
            drawCheck(canvas, spot, onSelected);
        }
    }

    private void drawMine(Canvas canvas, Spot spot, boolean detail) {
        shape(canvas, spot, spot.size, fillPaint(mine));
        if (detail) {
            drawStar(canvas, spot, onMine);
        }
    }

    private void drawBooked(Canvas canvas, Spot spot, boolean detail) {
        shape(canvas, spot, spot.size, fillPaint(booked));
        if (detail) {
            drawCross(canvas, spot, onBooked);
        }
    }

    private void drawLocked(Canvas canvas, Spot spot, boolean detail, boolean slash) {
        // Mẫu G: nền kem, viền mực tím dày 16% bán kính (theo sơ đồ, nên dày lên khi zoom như các phần khác của chỗ).
        final float strokeWorld = spot.size * 0.16f;
        shape(canvas, spot, spot.size - strokeWorld / 2f, fillPaint(lockedFill));
        shape(canvas, spot, spot.size - strokeWorld / 2f, strokePaint(lockedStroke, strokeWorld * scale, 255));
        if (slash) {
            drawSlash(canvas, spot, lockedStroke);
        }
    }

    /**
     * Vòng mực quanh chỗ đang chọn (mẫu G): cách mép chỗ 40% bán kính, dày 24% bán kính, đậm hẳn. Tính theo đơn vị
     * sơ đồ nên vòng to ra cùng chỗ khi zoom, và vẫn thấy rõ từ xa ở mức Fit.
     */
    private void drawRingAround(Canvas canvas, Spot spot, int color) {
        final float widthWorld = spot.size * 0.24f;
        shape(canvas, spot, spot.size * 1.4f, strokePaint(color, widthWorld * scale, 255));
    }

    /**
     * Vòng đôi: viền trắng bên dưới, vòng mực bên trên, nên thấy được trên mọi màu nền. Khi vừa định
     * vị, vòng nở ra và dày lên theo {@code pulse} để thu hút mắt, rồi trở về bình thường.
     */
    private void drawFocusRing(Canvas canvas, Spot spot) {
        dimmed = false;
        final float grow = 1f + 3.2f * pulse;
        final float radius = spot.size + 4.2f * density * pixel * grow;
        shape(canvas, spot, radius, strokePaint(focusHalo, (4f + 4f * pulse) * density, 255));
        shape(canvas, spot, radius, strokePaint(focusInk, (1.8f + 2.4f * pulse) * density, 255));
    }

    private void drawLabel(Canvas canvas, Spot spot, int color) {
        text.setColor(color);
        text.setTextSize(spot.size * 0.62f);
        if (dimmed) {
            text.setAlpha(DIMMED_ALPHA);
        }
        text.getFontMetrics(fontMetrics);
        canvas.save();
        canvas.rotate(uprightDegrees, spot.cx, spot.cy);
        canvas.drawText(spot.label, spot.cx, spot.cy - (fontMetrics.ascent + fontMetrics.descent) / 2f, text);
        canvas.restore();
    }

    private void drawCheck(Canvas canvas, Spot spot, int color) {
        final float x = spot.cx, y = spot.cy, r = spot.size;
        lines[0] = x - r * 0.42f; lines[1] = y + r * 0.02f; lines[2] = x - r * 0.08f; lines[3] = y + r * 0.36f;
        lines[4] = x - r * 0.08f; lines[5] = y + r * 0.36f; lines[6] = x + r * 0.46f; lines[7] = y - r * 0.32f;
        canvas.save();
        canvas.rotate(uprightDegrees, x, y);
        canvas.drawLines(lines, glyphPaint(color, Math.max(1.6f * density * pixel, r * 0.16f)));
        canvas.restore();
    }

    private void drawCross(Canvas canvas, Spot spot, int color) {
        final float x = spot.cx, y = spot.cy, d = spot.size * 0.38f;
        lines[0] = x - d; lines[1] = y - d; lines[2] = x + d; lines[3] = y + d;
        lines[4] = x - d; lines[5] = y + d; lines[6] = x + d; lines[7] = y - d;
        canvas.save();
        canvas.rotate(uprightDegrees, x, y);
        canvas.drawLines(lines, glyphPaint(color, Math.max(1.4f * density * pixel, spot.size * 0.13f)));
        canvas.restore();
    }

    private void drawSlash(Canvas canvas, Spot spot, int color) {
        final float x = spot.cx, y = spot.cy, d = spot.size * 0.55f;
        canvas.save();
        canvas.rotate(uprightDegrees, x, y);
        canvas.drawLine(x - d, y + d, x + d, y - d,
                glyphPaint(color, Math.max(1.5f * density * pixel, spot.size * 0.13f)));
        canvas.restore();
    }

    private void drawStar(Canvas canvas, Spot spot, int color) {
        canvas.save();
        canvas.translate(spot.cx, spot.cy);
        canvas.rotate(uprightDegrees);
        canvas.scale(spot.size, spot.size);
        canvas.drawPath(star, fillPaint(color));
        canvas.restore();
    }

    /** Ghế là hình tròn, ô đứng là hình vuông bo góc. */
    private void shape(Canvas canvas, Spot spot, float radius, Paint paint) {
        if (spot.kind == SpotKind.STANDING_CELL) {
            rect.set(spot.cx - radius, spot.cy - radius, spot.cx + radius, spot.cy + radius);
            canvas.drawRoundRect(rect, radius * 0.22f, radius * 0.22f, paint);
        } else {
            canvas.drawCircle(spot.cx, spot.cy, radius, paint);
        }
    }

    private Paint fillPaint(int color) {
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(color);
        if (dimmed) {
            fill.setAlpha(DIMMED_ALPHA);
        }
        return fill;
    }

    private Paint strokePaint(int color, float widthPx, int alpha) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(color);
        stroke.setStrokeWidth(widthPx * pixel);
        stroke.setAlpha(dimmed ? Math.min(alpha, DIMMED_ALPHA) : alpha);
        return stroke;
    }

    private Paint glyphPaint(int color, float widthWorld) {
        glyph.setColor(color);
        glyph.setStrokeWidth(widthWorld);
        if (dimmed) {
            glyph.setAlpha(DIMMED_ALPHA);
        }
        return glyph;
    }

    /**
     * Vòng lan quanh chỗ khi vừa chọn, bỏ chọn hoặc vừa thành vé của bạn (Q43). Vòng nở từ mép chỗ ra tới
     * 3,2 lần bán kính và mờ dần: bán kính = r·(1 + 2,2p), độ đậm = 1 − p, với p là tiến độ từ 0 đến 1.
     */
    public void drawRing(Canvas canvas, Spot spot, DisplayState state, float progress) {
        dimmed = false;
        final int color = state == DisplayState.MINE ? mine : selected;
        final float radius = spot.size * (1f + 2.2f * progress);
        shape(canvas, spot, radius, strokePaint(color, 2.5f * density, (int) (255 * (1f - progress))));
    }

    /** Ngôi sao 5 cánh bán kính 1 (cánh ngoài 0,6, cánh trong 0,26 như mẫu G), dựng một lần rồi phóng theo chỗ. */
    private void buildUnitStar() {
        for (int i = 0; i < 10; i++) {
            final double angle = -Math.PI / 2 + i * Math.PI / 5;
            final float radius = (i % 2 == 0) ? 0.6f : 0.26f;
            final float px = (float) (radius * Math.cos(angle));
            final float py = (float) (radius * Math.sin(angle));
            if (i == 0) {
                star.moveTo(px, py);
            } else {
                star.lineTo(px, py);
            }
        }
        star.close();
    }

    private static int color(Context context, int resId) {
        return ContextCompat.getColor(context, resId);
    }
}
