package com.example.midtermproject.decor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.midtermproject.R;

/**
 * Nền poster phủ cả màn hình: nền hồng sen với các dải lỏng cam, xanh lá, tím uốn lượn liên tục
 * (REQUIREMENTS Q42, Q43). Chỉ là trang trí, nằm dưới mọi View khác và không nhận chạm.
 * <p>
 * Hình các dải do {@link LiquidColumns} tính theo đơn vị dp; khổ thiết kế rộng 360dp được lặp lại
 * theo chiều ngang, nên khi xoay ngang dải không bị phình to mà chỉ có thêm dải.
 * <p>
 * Chuyển động: mỗi lần vẽ xong, View hẹn vẽ lại ở khung hình kế tiếp ({@code postInvalidateOnAnimation}).
 * Khi Activity vào nền thì hệ thống ngừng vẽ, nên vòng lặp tự dừng và không tốn pin.
 * {@link #onDraw} không cấp phát gì: các mảng mép được cấp trong {@link #onSizeChanged}.
 */
public class LiquidPosterView extends View {
    /** Thời gian để pha dải tăng 2π (một vòng uốn), tính bằng mili giây. */
    private static final double SWAY_CYCLE_MS = 9000.0;
    /** Khoảng cách giữa hai điểm lấy mẫu trên mép dải, tính bằng dp. */
    private static final float STEP_DP = 10f;

    private final LiquidColumns columns = LiquidColumns.poster();
    private final Paint[] columnPaints = new Paint[3];
    private final Path path = new Path();
    private int background;
    private float density;
    private int samples;
    private float[] left = new float[0];
    private float[] right = new float[0];
    /** Tắt khi người dùng tắt hiệu ứng động của hệ thống: dải đứng yên ở hình ban đầu. */
    private boolean animated = true;

    public LiquidPosterView(Context context) {
        super(context);
        init(context);
    }

    public LiquidPosterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LiquidPosterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LiquidPosterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        background = ContextCompat.getColor(context, R.color.poster_background);
        columnPaints[LiquidColumns.COLOR_ORANGE] = fillPaint(ContextCompat.getColor(context, R.color.liquid_orange));
        columnPaints[LiquidColumns.COLOR_GREEN] = fillPaint(ContextCompat.getColor(context, R.color.liquid_green));
        columnPaints[LiquidColumns.COLOR_VIOLET] = fillPaint(ContextCompat.getColor(context, R.color.liquid_violet));
        // Trang trí thuần túy: trình đọc màn hình bỏ qua.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Bật hoặc tắt chuyển động (theo cài đặt hiệu ứng của hệ thống, xem MainActivity). */
    public void setAnimated(boolean animated) {
        this.animated = animated;
        invalidate();
    }

    private static Paint fillPaint(int color) {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        return paint;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        samples = LiquidColumns.samplesFor(h / density, STEP_DP);
        left = new float[samples];
        right = new float[samples];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(background);
        if (samples == 0) {
            return;
        }
        // Pha thời gian, lấy phần dư theo chu kỳ để số không lớn dần làm mất độ chính xác.
        final double time = animated
                ? (SystemClock.uptimeMillis() / SWAY_CYCLE_MS * 2 * Math.PI) % LiquidColumns.PERIOD : 0.0;
        final float tileWidth = LiquidColumns.DESIGN_WIDTH * density;

        canvas.save();
        canvas.scale(density, density);  // từ đây vẽ theo dp
        for (int i = 0; i < columns.count(); i++) {
            columns.edges(i, samples, STEP_DP, time, left, right);
            buildColumnPath();
            final Paint paint = columnPaints[columns.colorSlot(i)];
            // Lặp khổ thiết kế theo chiều ngang cho tới khi phủ hết bề rộng View.
            for (float offset = 0; offset * density < getWidth() + tileWidth; offset += LiquidColumns.DESIGN_WIDTH) {
                canvas.save();
                canvas.translate(offset, 0);
                canvas.drawPath(path, paint);
                canvas.restore();
            }
        }
        canvas.restore();
        if (animated) {
            postInvalidateOnAnimation();
        }
    }

    /** Nối mép trái từ trên xuống, rồi mép phải từ dưới lên, thành một hình kín. */
    private void buildColumnPath() {
        path.reset();
        path.moveTo(left[0], -STEP_DP);
        for (int k = 1; k < samples; k++) {
            path.lineTo(left[k], (k - 1) * STEP_DP);
        }
        for (int k = samples - 1; k >= 0; k--) {
            path.lineTo(right[k], (k - 1) * STEP_DP);
        }
        path.close();
    }
}
