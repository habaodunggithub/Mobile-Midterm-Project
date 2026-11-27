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
    /**
     * Chỗ có đường kính trên màn hình từ ngưỡng này trở lên mới vẽ mã chỗ (A05…): nhỏ hơn thì chữ không đọc được.
     * Các ký hiệu trạng thái ✓ ✕ ★ và gạch chéo thì vẽ ở mọi mức zoom, như mẫu G (Q48).
     */
    private static final float DETAIL_MIN_DIAMETER_DP = 22f;

    private final float density;
    private final float detailMinDiameterPx;

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
    /** Độ đậm (0–255) của chỗ đang vẽ: 255 khi rõ hẳn, {@link #DIMMED_ALPHA} khi mờ hẳn, ở giữa khi đang mờ dần. */
    private int dimAlpha = 255;

    public SpotRenderer(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        detailMinDiameterPx = DETAIL_MIN_DIAMETER_DP * density;

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
     * @param dimLevel mức mờ từ 0 (rõ) đến 1 (mờ hẳn, chỗ ngoài bộ lọc); ở giữa khi đang mờ dần (Q49).
     *                 Vẽ mờ nhưng giữ nguyên hình dạng và trạng thái
     * @param focused chỗ đang xem: thêm vòng đôi bên ngoài
     */
    public void draw(Canvas canvas, Spot spot, DisplayState state, float dimLevel, boolean focused) {
        this.dimmed = dimLevel > 0f;
        // Nội suy tuyến tính độ đậm: 255 + (DIMMED_ALPHA − 255)·mức mờ.
        this.dimAlpha = Math.round(255 + (DIMMED_ALPHA - 255) * dimLevel);
        final boolean detail = showsDetails(spot.size);
        switch (state) {
            case SELECTED:
                drawSelected(canvas, spot);
                break;
            case MINE:
                drawMine(canvas, spot);
                break;
            case BOOKED:
                drawBooked(canvas, spot);
                break;
            case LOCKED:
                drawLocked(canvas, spot);
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

    private void drawSelected(Canvas canvas, Spot spot) {
        drawRingAround(canvas, spot, selected);
        shape(canvas, spot, spot.size, fillPaint(selected));
        drawCheck(canvas, spot, onSelected);
    }

    private void drawMine(Canvas canvas, Spot spot) {
        shape(canvas, spot, spot.size, fillPaint(mine));
        drawStar(canvas, spot, onMine);
    }

    private void drawBooked(Canvas canvas, Spot spot) {
        shape(canvas, spot, spot.size, fillPaint(booked));
        drawCross(canvas, spot, onBooked);
    }

    private void drawLocked(Canvas canvas, Spot spot) {
        // Mẫu G: nền kem, viền mực tím dày 16% bán kính (theo sơ đồ, nên dày lên khi zoom như các phần khác của chỗ).
        final float strokeWorld = spot.size * 0.16f;
        shape(canvas, spot, spot.size - strokeWorld / 2f, fillPaint(lockedFill));
        shape(canvas, spot, spot.size - strokeWorld / 2f, strokePaint(lockedStroke, strokeWorld * scale, 255));
        drawSlash(canvas, spot, lockedStroke);
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
            text.setAlpha(dimAlpha);
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
        canvas.drawLines(lines, glyphPaint(color, r * 0.24f));
        canvas.restore();
    }

    private void drawCross(Canvas canvas, Spot spot, int color) {
        final float x = spot.cx, y = spot.cy, d = spot.size * 0.38f;
        lines[0] = x - d; lines[1] = y - d; lines[2] = x + d; lines[3] = y + d;
        lines[4] = x - d; lines[5] = y + d; lines[6] = x + d; lines[7] = y - d;
        canvas.save();
        canvas.rotate(uprightDegrees, x, y);
        canvas.drawLines(lines, glyphPaint(color, spot.size * 0.19f));
        canvas.restore();
    }

    private void drawSlash(Canvas canvas, Spot spot, int color) {
        final float x = spot.cx, y = spot.cy, d = spot.size * 0.55f;
        canvas.save();
        canvas.rotate(uprightDegrees, x, y);
        canvas.drawLine(x - d, y + d, x + d, y - d,
                glyphPaint(color, spot.size * 0.19f));
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
            fill.setAlpha(dimAlpha);
        }
        return fill;
    }

    private Paint strokePaint(int color, float widthPx, int alpha) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(color);
        stroke.setStrokeWidth(widthPx * pixel);
        stroke.setAlpha(dimmed ? alpha * dimAlpha / 255 : alpha);
        return stroke;
    }

    private Paint glyphPaint(int color, float widthWorld) {
        glyph.setColor(color);
        glyph.setStrokeWidth(widthWorld);
        if (dimmed) {
            glyph.setAlpha(dimAlpha);
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
        final float sparkRadius = radius + 3f * density * pixel;
        final float length = 5f * density * pixel * (1f - progress);
        final Paint spark = strokePaint(color, 1.6f * density, Math.round(210f * (1f - progress)));
        for (int direction = 0; direction < 4; direction++) {
            final float x = (direction < 2 ? 0.7071f : -0.7071f);
            final float y = (direction % 2 == 0 ? 0.7071f : -0.7071f);
            canvas.drawLine(spot.cx + x * sparkRadius, spot.cy + y * sparkRadius,
                    spot.cx + x * (sparkRadius + length), spot.cy + y * (sparkRadius + length), spark);
        }
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
