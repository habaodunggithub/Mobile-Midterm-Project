package com.example.midtermproject.decor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.midtermproject.R;

/**
 * Viền sticker quanh sơ đồ: một dải cam dày, mép ngoài lượn như miếng dán cắt tay, trên dải có dòng chữ
 * chạy vòng quanh (REQUIREMENTS Q42). Nằm đè lên mép của {@code VenueMapView}; phần giữa để trống nên
 * sơ đồ lộ ra bên trong.
 * <p>
 * Dải là phần nằm giữa hai đường: mép ngoài lượn ({@link #outline}) và mép trong là hình chữ nhật bo góc.
 * Vẽ bằng một Path có luật tô EVEN_ODD: điểm nằm trong cả hai đường thì không tô, nên chỉ có dải được tô.
 * <p>
 * Chữ chạy: vẽ bằng {@code drawTextOnPath} dọc theo đường giữa dải, dịch dần theo thời gian. Khi chữ
 * dài hơn một vòng thì vẽ thêm bản sao nối tiếp, nên dòng chữ không bao giờ bị hở.
 * {@link #onDraw} không cấp phát gì; mọi Path được dựng lại trong {@link #onSizeChanged}.
 */
public class StickerFrameView extends View {
    /** Biên độ gợn của mép ngoài, tính bằng dp. */
    private static final float WOBBLE_DP = 2.5f;
    /** Bán kính bo góc của mép trong và của đường chữ chạy, tính bằng dp. */
    private static final float CORNER_DP = 14f;
    /** Tốc độ chữ chạy, dp mỗi giây. */
    private static final float TICKER_SPEED_DP = 40f;
    private static final float TICKER_TEXT_SP = 9.5f;
    /** Số điểm lấy mẫu trên mép ngoài lượn. */
    private static final int OUTLINE_POINTS = 240;

    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path band = new Path();
    private final Path tickerPath = new Path();
    private final RectF rect = new RectF();
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();

    private float density;
    private float margin;
    private float bandWidth;
    private String ticker = "";
    private float tickerWidth;
    private float tickerLength;
    private float tickerBaseline;

    public StickerFrameView(Context context) {
        super(context);
        init(context);
    }

    public StickerFrameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StickerFrameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public StickerFrameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        final Resources res = context.getResources();
        density = res.getDisplayMetrics().density;
        margin = res.getDimension(R.dimen.sticker_margin);
        bandWidth = res.getDimension(R.dimen.sticker_band);

        bandPaint.setStyle(Paint.Style.FILL);
        bandPaint.setColor(ContextCompat.getColor(context, R.color.sticker_band));
        band.setFillType(Path.FillType.EVEN_ODD);

        tickerPaint.setColor(ContextCompat.getColor(context, R.color.sticker_band_text));
        tickerPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TICKER_TEXT_SP,
                res.getDisplayMetrics()));
        final Typeface mono = ResourcesCompat.getFont(context, R.font.space_mono_bold);
        tickerPaint.setTypeface(mono != null ? mono : Typeface.MONOSPACE);
        tickerPaint.getFontMetrics(fontMetrics);
        // Đặt chữ giữa dải: dịch đường cơ sở xuống nửa chiều cao chữ.
        tickerBaseline = -(fontMetrics.ascent + fontMetrics.descent) / 2f;

        // Trang trí: chữ chạy lặp lại thông tin đã có trên sơ đồ, trình đọc màn hình bỏ qua.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Đặt nội dung một vòng chữ chạy (xem {@link TickerText}); đo bề rộng một lần ở đây. */
    public void setTickerText(String text) {
        ticker = text != null ? text : "";
        tickerWidth = tickerPaint.measureText(ticker);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final float corner = CORNER_DP * density;
        band.reset();
        rect.set(margin, margin, w - margin, h - margin);
        outline(band, rect, WOBBLE_DP * density);
        rect.inset(bandWidth, bandWidth);
        band.addRoundRect(rect, corner, corner, Path.Direction.CW);

        // Đường chữ chạy nằm giữa dải, theo chiều kim đồng hồ.
        tickerPath.reset();
        rect.set(margin + bandWidth / 2f, margin + bandWidth / 2f, w - margin - bandWidth / 2f, h - margin - bandWidth / 2f);
        tickerPath.addRoundRect(rect, corner, corner, Path.Direction.CW);
        tickerLength = 2f * (rect.width() + rect.height());
    }

    /**
     * Mép ngoài lượn: "siêu ellipse" (chữ nhật bo góc tròn trịa) cộng gợn sóng nhỏ, cùng công thức với mép
     * sân khấu trong {@code VenueRenderer}: x = cx + (hw + w)·sgn(cos t)·|cos t|^(2/k), tương tự với y.
     */
    private static void outline(Path path, RectF r, float wobble) {
        final double power = 2.0 / 12.0;
        final float cx = r.centerX(), cy = r.centerY(), hw = r.width() / 2f, hh = r.height() / 2f;
        for (int i = 0; i < OUTLINE_POINTS; i++) {
            final double t = 2 * Math.PI * i / OUTLINE_POINTS;
            final double c = Math.cos(t), s = Math.sin(t);
            final double w = wobble * (Math.sin(11 * t) + 0.6 * Math.sin(23 * t + 1));
            final float x = (float) (cx + (hw + w) * Math.signum(c) * Math.pow(Math.abs(c), power));
            final float y = (float) (cy + (hh + w) * Math.signum(s) * Math.pow(Math.abs(s), power));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(band, bandPaint);
        if (tickerWidth <= 0f) {
            return;
        }
        // Quãng chữ đã chạy, lấy phần dư theo bề rộng một vòng chữ để vẽ lặp liền mạch.
        final float travelled = (SystemClock.uptimeMillis() / 1000f * TICKER_SPEED_DP * density) % tickerWidth;
        for (float offset = -travelled; offset < tickerLength; offset += tickerWidth) {
            canvas.drawTextOnPath(ticker, tickerPath, offset, tickerBaseline, tickerPaint);
        }
        postInvalidateOnAnimation();
    }

    /**
     * Chạm xuống trên dải viền thì giữ lại, không cho lọt xuống sơ đồ: chỗ nằm dưới viền không nhìn thấy
     * nên không được chọn. Chạm ở phần giữa (sơ đồ) hay ngoài sticker thì bỏ qua để View bên dưới xử lý.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        final float x = event.getX(), y = event.getY();
        final boolean insideOuter = x >= margin && x <= getWidth() - margin && y >= margin && y <= getHeight() - margin;
        final float inner = margin + bandWidth;
        final boolean insideInner = x > inner && x < getWidth() - inner && y > inner && y < getHeight() - inner;
        return insideOuter && !insideInner;
    }
}
