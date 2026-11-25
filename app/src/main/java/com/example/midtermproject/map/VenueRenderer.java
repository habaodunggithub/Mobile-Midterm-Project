package com.example.midtermproject.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;

import com.example.midtermproject.R;
import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.model.ZoneType;

import java.util.List;

/**
 * Vẽ toàn bộ sơ đồ lên một Canvas: nền, sân khấu, các chỗ và tên phân khu.
 * <p>
 * Thứ tự vẽ: nền, rồi (trong tọa độ sơ đồ, sau khi áp ma trận) sân khấu, tên hai bãi Standing và
 * các chỗ, rồi (trong tọa độ màn hình) tên các phân khu còn lại. Tên phân khu ghế vẽ trong tọa độ
 * màn hình để luôn nằm ngang, cùng cỡ chữ và đọc được ở mọi mức zoom và mọi góc xoay (ADR-07).
 * Chữ "SÂN KHẤU" và tên bãi Standing thì vẽ trong tọa độ sơ đồ, nên xoay cùng sân khấu: hành lang
 * giữa sân khấu và bãi Standing quá hẹp để chứa chữ nằm ngang khi sơ đồ xoay dọc.
 * <p>
 * Không cấp phát gì trong {@link #draw}.
 */
public final class VenueRenderer {
    /**
     * Chỗ chỉ bị bỏ qua khi cách vùng nhìn xa hơn ngần này dp (đổi ra đơn vị sơ đồ khi so sánh), vì vòng chỗ đang
     * xem, quầng và nét viền vẽ rộng ra ngoài hình của chỗ.
     */
    private static final float CULL_MARGIN_DP = 24f;

    private final SpotRenderer spotRenderer;
    private final float density;

    private final int background;
    private final int stageFill;
    private final int stageText;
    private final int labelText;
    private final int labelHalo;
    private final String stageLabel;

    private final Paint stagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stageTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint standingLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final RectF rect = new RectF();
    private final float[] point = new float[2];

    public VenueRenderer(Context context) {
        spotRenderer = new SpotRenderer(context);
        density = context.getResources().getDisplayMetrics().density;
        background = ContextCompat.getColor(context, R.color.map_background);
        stageFill = ContextCompat.getColor(context, R.color.stage_fill);
        stageText = ContextCompat.getColor(context, R.color.stage_text);
        labelText = ContextCompat.getColor(context, R.color.label_text);
        labelHalo = ContextCompat.getColor(context, R.color.label_halo);
        stageLabel = context.getString(R.string.stage_label);

        stagePaint.setStyle(Paint.Style.FILL);
        stagePaint.setColor(stageFill);
        stageTextPaint.setColor(stageText);
        stageTextPaint.setTextAlign(Paint.Align.CENTER);
        stageTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        stageTextPaint.setLetterSpacing(0.08f);
        standingLabelPaint.setColor(labelText);
        standingLabelPaint.setTextAlign(Paint.Align.CENTER);
        standingLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        labelPaint.setColor(labelText);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        labelHaloPaint.setColor(labelHalo);
        labelHaloPaint.setStyle(Paint.Style.STROKE);
        labelHaloPaint.setStrokeJoin(Paint.Join.ROUND);
        labelHaloPaint.setStrokeWidth(3f * density);
        labelHaloPaint.setTextAlign(Paint.Align.CENTER);
        labelHaloPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /**
     * @param worldToScreen ma trận đổi tọa độ sơ đồ sang màn hình (scale, xoay, dịch)
     * @param scale         tỉ lệ hiện tại, dùng để chọn mức chi tiết và bề dày nét
     * @param angleRad      góc xoay hiện tại, để mã chỗ và ký hiệu luôn đứng thẳng trên màn hình
     * @param pulse         độ nhấp nháy của vòng chỗ đang xem (0 đến 1)
     * @param visibleBounds {trái, trên, phải, dưới} của vùng nhìn thấy theo tọa độ sơ đồ
     *                      ({@link MapCamera#getVisibleWorldBounds}); chỗ nằm ngoài khung này không được vẽ
     */
    public void draw(Canvas canvas, Venue venue, SpotStateSource states, Matrix worldToScreen,
                     float scale, float angleRad, float pulse, float[] visibleBounds) {
        canvas.drawColor(background);

        canvas.save();
        canvas.concat(worldToScreen);
        drawStage(canvas, venue);
        drawStandingLabels(canvas, venue);
        drawSpots(canvas, venue, states, scale, angleRad, pulse, visibleBounds);
        canvas.restore();

        drawSectionLabels(canvas, venue, worldToScreen);
    }

    private void drawStage(Canvas canvas, Venue venue) {
        final float height = venue.stageBottom - venue.stageTop;
        rect.set(venue.stageLeft, venue.stageTop, venue.stageRight, venue.stageBottom);
        final float corner = Math.min(height / 2f, 10f);
        canvas.drawRoundRect(rect, corner, corner, stagePaint);

        stageTextPaint.setTextSize(height * 0.42f);
        stageTextPaint.getFontMetrics(fontMetrics);
        final float baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(stageLabel, rect.centerX(), baseline, stageTextPaint);
    }

    /** Tên hai bãi Standing, nằm trong hành lang giữa sân khấu và bãi. */
    private void drawStandingLabels(Canvas canvas, Venue venue) {
        // Cỡ chữ theo chiều cao sân khấu (50 đơn vị): 13 đơn vị vừa với hành lang rộng 30.
        standingLabelPaint.setTextSize((venue.stageBottom - venue.stageTop) * 0.26f);
        standingLabelPaint.getFontMetrics(fontMetrics);
        final float centerOffset = (fontMetrics.ascent + fontMetrics.descent) / 2f;
        final List<Section> sections = venue.getSections();
        for (int i = 0; i < sections.size(); i++) {
            final Section section = sections.get(i);
            if (section.zone == ZoneType.STANDING) {
                canvas.drawText(section.name, section.labelX, section.labelY - centerOffset, standingLabelPaint);
            }
        }
    }

    private void drawSpots(Canvas canvas, Venue venue, SpotStateSource states, float scale, float angleRad,
                           float pulse, float[] visibleBounds) {
        spotRenderer.beginFrame(scale, (float) Math.toDegrees(angleRad), pulse);
        // Culling: bỏ qua chỗ nằm hẳn ngoài vùng nhìn (kể cả phần vòng sáng và viền vẽ rộng ra), nên khi
        // phóng to chỉ vẽ vài chục chỗ thay vì cả 224 chỗ.
        final float margin = CULL_MARGIN_DP * density / scale;
        final float left = visibleBounds[0] - margin;
        final float top = visibleBounds[1] - margin;
        final float right = visibleBounds[2] + margin;
        final float bottom = visibleBounds[3] + margin;
        final List<Spot> spots = venue.getSpots();
        for (int i = 0; i < spots.size(); i++) {
            final Spot spot = spots.get(i);
            if (spot.cx + spot.size < left || spot.cx - spot.size > right
                    || spot.cy + spot.size < top || spot.cy - spot.size > bottom) {
                continue;
            }
            spotRenderer.draw(canvas, spot, states.displayStateOf(spot),
                    states.isDimmed(spot), states.isFocused(spot));
        }
    }

    private void drawSectionLabels(Canvas canvas, Venue venue, Matrix worldToScreen) {
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
            if (section.zone == ZoneType.STANDING) {
                continue;
            }
            point[0] = section.labelX;
            point[1] = section.labelY;
            worldToScreen.mapPoints(point);
            canvas.drawText(section.name, point[0], point[1] - centerOffset, labelHaloPaint);
            canvas.drawText(section.name, point[0], point[1] - centerOffset, labelPaint);
        }
    }
}
