package com.example.midtermproject.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.midtermproject.R;
import com.example.midtermproject.model.DisplayState;
import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.Venue;

import java.util.List;
import java.util.Locale;

/**
 * Vẽ toàn bộ sơ đồ lên một Canvas: nền, sân khấu, các chỗ và tên phân khu.
 * <p>
 * Thứ tự vẽ: nền, rồi (trong tọa độ sơ đồ, sau khi áp ma trận) sân khấu và các chỗ, rồi (trong tọa độ
 * màn hình) tên mọi phân khu. Tên phân khu vẽ trong tọa độ màn hình để luôn nằm ngang, cùng cỡ chữ và
 * đọc được ở mọi mức zoom và mọi góc xoay (ADR-07); viết hoa, có viền kem, kể cả tên hai bãi Standing
 * (đè lên mép sân khấu và bãi như mẫu G, Q48). Chữ "SÂN KHẤU" vẽ trong tọa độ sơ đồ nên xoay cùng sân khấu.
 * <p>
 * Không cấp phát gì trong {@link #draw}.
 */
public final class VenueRenderer {
    /**
     * Chỗ chỉ bị bỏ qua khi cách vùng nhìn xa hơn ngần này dp (đổi ra đơn vị sơ đồ khi so sánh), vì vòng chỗ đang
     * xem, quầng và nét viền vẽ rộng ra ngoài hình của chỗ.
     */
    private static final float CULL_MARGIN_DP = 24f;
    /** Biên độ rung ngang của chỗ không chọn được, tính bằng dp trên màn hình. */
    private static final float SHAKE_AMPLITUDE_DP = 4f;
    /** Biên độ gợn của mép sân khấu, tính bằng đơn vị sơ đồ: đủ để trông như miếng dán cắt tay. */
    private static final float STAGE_WOBBLE = 1.6f;
    /** Số điểm lấy mẫu trên mép sân khấu. */
    private static final int STAGE_POINTS = 96;

    private final SpotRenderer spotRenderer;
    private final MapLightShow lightShow;
    private final float density;

    private final int background;
    private final int stageFill;
    private final int stageStroke;
    private final int stageText;
    private final int labelText;
    private final int labelHalo;
    private final String stageLabel;

    private final Paint stagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stageStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Hình sân khấu có mép lượn; dựng một lần cho mỗi sơ đồ (xem {@link #buildStagePath}). */
    private final Path stagePath = new Path();
    private Venue stagePathVenue;
    /** Tên phân khu viết hoa (mẫu G), tính một lần cho mỗi sơ đồ để onDraw không phải đổi chữ. */
    private String[] upperLabels = new String[0];
    private Venue labelsVenue;
    private final Paint stageTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final RectF rect = new RectF();
    private final float[] point = new float[2];

    public VenueRenderer(Context context) {
        spotRenderer = new SpotRenderer(context);
        density = context.getResources().getDisplayMetrics().density;
        lightShow = new MapLightShow(density, context.getColor(R.color.liquid_orange),
                context.getColor(R.color.liquid_violet), context.getColor(R.color.zone_standard),
                context.getColor(R.color.liquid_green));
        background = ContextCompat.getColor(context, R.color.map_background);
        stageFill = ContextCompat.getColor(context, R.color.stage_fill);
        stageStroke = ContextCompat.getColor(context, R.color.stage_stroke);
        stageText = ContextCompat.getColor(context, R.color.stage_text);
        labelText = ContextCompat.getColor(context, R.color.label_text);
        labelHalo = ContextCompat.getColor(context, R.color.label_halo);
        stageLabel = context.getString(R.string.stage_label);

        // Chữ lượn Kablammo chỉ dùng cho chữ to "SÂN KHẤU"; nhãn khu dùng Space Mono đậm (Q44).
        final Typeface display = fontOrDefault(context, R.font.kablammo);
        final Typeface mono = fontOrDefault(context, R.font.space_mono_bold);

        stagePaint.setStyle(Paint.Style.FILL);
        stagePaint.setColor(stageFill);
        stageStrokePaint.setStyle(Paint.Style.STROKE);
        stageStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        stageStrokePaint.setColor(stageStroke);
        stageTextPaint.setColor(stageText);
        stageTextPaint.setTextAlign(Paint.Align.CENTER);
        stageTextPaint.setTypeface(display);
        labelPaint.setColor(labelText);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(mono);
        labelHaloPaint.setColor(labelHalo);
        labelHaloPaint.setStyle(Paint.Style.STROKE);
        labelHaloPaint.setStrokeJoin(Paint.Join.ROUND);
        labelHaloPaint.setStrokeWidth(3f * density);
        labelHaloPaint.setTextAlign(Paint.Align.CENTER);
        labelHaloPaint.setTypeface(mono);
    }

    /** Lấy font trong res/font; nếu không đọc được (máy lỗi tài nguyên) thì dùng chữ đậm hệ thống. */
    private static Typeface fontOrDefault(Context context, int fontRes) {
        final Typeface font = ResourcesCompat.getFont(context, fontRes);
        return font != null ? font : Typeface.DEFAULT_BOLD;
    }

    /**
     * @param worldToScreen ma trận đổi tọa độ sơ đồ sang màn hình (scale, xoay, dịch)
     * @param scale         tỉ lệ hiện tại, dùng để chọn mức chi tiết và bề dày nét
     * @param angleRad      góc xoay hiện tại, để mã chỗ và ký hiệu luôn đứng thẳng trên màn hình
     * @param pulse         độ nhấp nháy của vòng chỗ đang xem (0 đến 1)
     * @param visibleBounds {trái, trên, phải, dưới} của vùng nhìn thấy theo tọa độ sơ đồ
     *                      ({@link MapCamera#getVisibleWorldBounds}); chỗ nằm ngoài khung này không được vẽ
     * @param effects       hiệu ứng đang chạy trên từng chỗ (nảy, vòng lan…), hỏi theo thời điểm {@code now}
     */
    public void draw(Canvas canvas, Venue venue, SpotStateSource states, Matrix worldToScreen,
                     float scale, float angleRad, float pulse, float[] visibleBounds,
                     SpotEffects effects, int width, int visibleHeight, long now) {
        canvas.drawColor(background);

        canvas.save();
        canvas.concat(worldToScreen);
        drawStage(canvas, venue, scale);
        drawSpots(canvas, venue, states, scale, angleRad, pulse, visibleBounds, effects, now);
        canvas.restore();

        if (lightShow.isRunning(now)) {
            point[0] = (venue.stageLeft + venue.stageRight) / 2f;
            point[1] = (venue.stageTop + venue.stageBottom) / 2f;
            worldToScreen.mapPoints(point);
            lightShow.draw(canvas, point[0], point[1], width, visibleHeight, now);
        }
        drawSectionLabels(canvas, venue, worldToScreen);
    }

    void playIntro(long now) {
        lightShow.startIntro(now);
    }

    void playBooking(long now) {
        lightShow.startBooking(now);
    }

    void clearLightShow() {
        lightShow.clear();
    }

    boolean hasActiveLightShow(long now) {
        return lightShow.isRunning(now);
    }

    private void drawStage(Canvas canvas, Venue venue, float scale) {
        if (stagePathVenue != venue) {
            buildStagePath(venue);
            stagePathVenue = venue;
        }
        final float height = venue.stageBottom - venue.stageTop;
        rect.set(venue.stageLeft, venue.stageTop, venue.stageRight, venue.stageBottom);
        canvas.drawPath(stagePath, stagePaint);
        // Viền cam dày 3dp trên màn hình ở mọi mức zoom (đổi dp ra đơn vị sơ đồ bằng cách chia cho scale).
        stageStrokePaint.setStrokeWidth(3f * density / scale);
        canvas.drawPath(stagePath, stageStrokePaint);

        // Chữ "SÂN KHẤU" to gần hết chiều cao sân khấu như mẫu G.
        stageTextPaint.setTextSize(height * 0.72f);
        stageTextPaint.getFontMetrics(fontMetrics);
        final float baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(stageLabel, rect.centerX(), baseline, stageTextPaint);
    }

    /**
     * Dựng mép sân khấu: một "siêu ellipse" (hình chữ nhật bo góc tròn trịa) cộng thêm gợn sóng nhỏ,
     * như miếng dán cắt bằng tay. Điểm thứ i ở góc t = 2πi/n:
     * x = cx + (hw + w(t))·sgn(cos t)·|cos t|^(2/k),  y = cy + (hh + w(t))·sgn(sin t)·|sin t|^(2/k),
     * với k = 10 (k càng lớn càng vuông) và w(t) = A·sin 7t + 0,6A·sin 13t.
     */
    private void buildStagePath(Venue venue) {
        final float cx = (venue.stageLeft + venue.stageRight) / 2f;
        final float cy = (venue.stageTop + venue.stageBottom) / 2f;
        final float hw = (venue.stageRight - venue.stageLeft) / 2f;
        final float hh = (venue.stageBottom - venue.stageTop) / 2f;
        final double power = 2.0 / 10.0;
        stagePath.reset();
        for (int i = 0; i < STAGE_POINTS; i++) {
            final double t = 2 * Math.PI * i / STAGE_POINTS;
            final double c = Math.cos(t), s = Math.sin(t);
            final double wobble = STAGE_WOBBLE * (Math.sin(7 * t) + 0.6 * Math.sin(13 * t));
            final float x = (float) (cx + (hw + wobble) * Math.signum(c) * Math.pow(Math.abs(c), power));
            final float y = (float) (cy + (hh + wobble) * Math.signum(s) * Math.pow(Math.abs(s), power));
            if (i == 0) {
                stagePath.moveTo(x, y);
            } else {
                stagePath.lineTo(x, y);
            }
        }
        stagePath.close();
    }

    private void drawSpots(Canvas canvas, Venue venue, SpotStateSource states, float scale, float angleRad,
                           float pulse, float[] visibleBounds, SpotEffects effects, long now) {
        spotRenderer.beginFrame(scale, (float) Math.toDegrees(angleRad), pulse);
        // Culling: bỏ qua chỗ nằm hẳn ngoài vùng nhìn (kể cả phần vòng sáng và viền vẽ rộng ra), nên khi
        // phóng to chỉ vẽ vài chục chỗ thay vì cả 224 chỗ.
        final float margin = CULL_MARGIN_DP * density / scale;
        final float left = visibleBounds[0] - margin;
        final float top = visibleBounds[1] - margin;
        final float right = visibleBounds[2] + margin;
        final float bottom = visibleBounds[3] + margin;
        // Hướng "ngang trên màn hình" đổi sang tọa độ sơ đồ: screen = s·R(θ)·world nên world = R(−θ)·screen / s,
        // và vector (1, 0) của màn hình thành (cos θ, −sin θ) / s. Nhờ vậy chỗ luôn rung ngang dù sơ đồ đang xoay.
        final float shakeUnit = SHAKE_AMPLITUDE_DP * density / scale;
        final float shakeX = (float) Math.cos(angleRad) * shakeUnit;
        final float shakeY = (float) -Math.sin(angleRad) * shakeUnit;
        final List<Spot> spots = venue.getSpots();
        for (int i = 0; i < spots.size(); i++) {
            final Spot spot = spots.get(i);
            if (spot.cx + spot.size < left || spot.cx - spot.size > right
                    || spot.cy + spot.size < top || spot.cy - spot.size > bottom) {
                continue;
            }
            // Hiệu ứng: phóng chỗ quanh tâm của nó (1 là bình thường, 0 là chưa hiện trong lúc mở màn).
            final float grow = effects.scaleOf(spot, now);
            if (grow <= 0f) {
                continue;
            }
            final float shake = effects.shakeOf(spot, now);
            final boolean transformed = grow != 1f || shake != 0f;
            if (transformed) {
                canvas.save();
                canvas.translate(shake * shakeX, shake * shakeY);
                canvas.scale(grow, grow, spot.cx, spot.cy);
            }
            final DisplayState state = states.displayStateOf(spot);
            spotRenderer.draw(canvas, spot, state, states.isDimmed(spot), states.isFocused(spot));
            if (transformed) {
                canvas.restore();
            }
            final float ring = effects.ringOf(spot, now);
            if (ring >= 0f) {
                spotRenderer.drawRing(canvas, spot, state, ring);
            }
        }
    }

    private void buildUpperLabels(Venue venue) {
        final Locale vietnamese = Locale.forLanguageTag("vi-VN");
        final List<Section> sections = venue.getSections();
        upperLabels = new String[sections.size()];
        for (int i = 0; i < sections.size(); i++) {
            upperLabels[i] = sections.get(i).name.toUpperCase(vietnamese);
        }
    }

    private void drawSectionLabels(Canvas canvas, Venue venue, Matrix worldToScreen) {
        if (labelsVenue != venue) {
            buildUpperLabels(venue);
            labelsVenue = venue;
        }
        final List<Spot> spots = venue.getSpots();
        final boolean detailed = !spots.isEmpty() && spotRenderer.showsDetails(spots.get(0).size);
        final float textSize = (detailed ? 12f : 10f) * density;
        labelPaint.setTextSize(textSize);
        labelHaloPaint.setTextSize(textSize);
        labelPaint.getFontMetrics(fontMetrics);
        final float centerOffset = (fontMetrics.ascent + fontMetrics.descent) / 2f;

        final List<Section> sections = venue.getSections();
        for (int i = 0; i < sections.size(); i++) {
            final Section section = sections.get(i);
            point[0] = section.labelX;
            point[1] = section.labelY;
            worldToScreen.mapPoints(point);
            canvas.drawText(upperLabels[i], point[0], point[1] - centerOffset, labelHaloPaint);
            canvas.drawText(upperLabels[i], point[0], point[1] - centerOffset, labelPaint);
        }
    }
}
