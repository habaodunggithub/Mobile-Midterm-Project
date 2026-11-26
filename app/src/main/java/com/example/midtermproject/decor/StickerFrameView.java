package com.example.midtermproject.decor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.midtermproject.R;

import java.util.Collections;
import java.util.List;

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
    /** Biên độ gợn của mép ngoài và mép trong, tính bằng dp (giống mẫu G). */
    private static final float OUTER_WOBBLE_DP = 2.5f;
    private static final float INNER_WOBBLE_DP = 1.5f;
    /** Bán kính bo góc của mép trong và của đường chữ chạy, tính bằng dp. */
    private static final float CORNER_DP = 14f;
    /**
     * Bán kính bo góc của View nằm dưới viền (sơ đồ). Sơ đồ là hình chữ nhật lùi vào giữa dải viền; nếu để góc vuông
     * thì góc đó thò ra ngoài mép ngoài đã bo tròn của viền. Bo 44dp thì điểm góc (theo đường chéo, cách góc khung
     * khoảng 29dp) nằm giữa mép ngoài (khoảng 21dp, lồi ra tới 25dp ở chỗ gợn) và mép trong (khoảng 36dp), tức là
     * khuất dưới dải.
     */
    private static final float CONTENT_CORNER_DP = 44f;
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
    private final RectF oval = new RectF();
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();

    private float density;
    private float margin;
    private float bandWidth;
    /** Các mục của dòng chữ chạy (xem {@link TickerText#items}); chuỗi vẽ được ghép lại mỗi khi chu vi đổi. */
    private List<String> tickerItems = Collections.emptyList();
    private String ticker = "";
    /** Bề rộng một vòng chữ đã ghép cho vừa chu vi (xem {@link #fitTickerToLap}). */
    private float tickerWidth;
    /** Chu vi đường giữa dải, tức chiều dài một vòng đường chữ chạy. */
    private float lapLength;
    private float tickerBaseline;
    /**
     * Phần đáy khung dành cho bảng chi tiết (chiều cao bảng thu gọn + khe 10dp), tính bằng pixel. Sticker kết thúc
     * phía trên phần này, như mẫu G (Q48).
     */
    private int bottomInset;
    /** View nằm dưới viền (sơ đồ), để cập nhật phần bị cắt khi đáy sticker đổi. */
    private View content;
    /** Tắt khi người dùng tắt hiệu ứng động của hệ thống: chữ đứng yên. */
    private boolean animated = true;

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

    /**
     * Cắt View nằm dưới viền (sơ đồ) theo hình chữ nhật bo góc, để góc vuông của nó không thò ra ngoài viền, và bỏ
     * phần nằm dưới đáy sticker (sơ đồ vẫn kéo xuống dưới bảng chi tiết, nhưng phần đó không được vẽ). Mép dưới của
     * phần giữ lại nằm ở giữa dải viền dưới, nên khuất dưới viền như ba cạnh còn lại. Dùng outline của View nên phần
     * cắt do bộ dựng hình làm, không tốn gì trong {@code onDraw} của sơ đồ. View phải là anh em cùng khung cha với
     * viền này.
     */
    public void clipContent(View content) {
        this.content = content;
        final float radius = CONTENT_CORNER_DP * density;
        content.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // Giữa dải viền dưới, theo tọa độ của viền, rồi đổi sang tọa độ của View bị cắt.
                final float bandMiddle = getHeight() - bottomInset - margin - bandWidth / 2f;
                final int bottom = Math.round(bandMiddle - (view.getTop() - getTop()));
                outline.setRoundRect(0, 0, view.getWidth(), Math.max(0, Math.min(view.getHeight(), bottom)), radius);
            }
        });
        content.setClipToOutline(true);
    }

    /**
     * Đặt phần đáy khung dành cho bảng chi tiết: sticker co lên để kết thúc phía trên bảng (Q48). Dựng lại các
     * đường viền và phần cắt của sơ đồ.
     */
    public void setBottomInset(int px) {
        if (px == bottomInset) {
            return;
        }
        bottomInset = px;
        buildPaths(getWidth(), getHeight());
        if (content != null) {
            content.invalidateOutline();
        }
        invalidate();
    }

    /** Bật hoặc tắt chữ chạy (theo cài đặt hiệu ứng của hệ thống, xem MainActivity). */
    public void setAnimated(boolean animated) {
        this.animated = animated;
        invalidate();
    }

    /** Đặt các mục của dòng chữ chạy (xem {@link TickerText#items}); chuỗi vẽ được ghép cho vừa chu vi viền. */
    public void setTickerItems(List<String> items) {
        tickerItems = items != null ? items : Collections.<String>emptyList();
        fitTickerToLap();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildPaths(w, h);
        if (content != null) {
            content.invalidateOutline();
        }
    }

    /** Dựng dải viền và đường chữ chạy cho khung rộng {@code w}, cao {@code h} (đáy sticker = h − bottomInset). */
    private void buildPaths(int w, int h) {
        if (w == 0 || h == 0) {
            return;
        }
        final float corner = CORNER_DP * density;
        final float bottom = h - bottomInset;
        band.reset();
        rect.set(margin, margin, w - margin, bottom - margin);
        outline(band, rect, OUTER_WOBBLE_DP * density, 1);
        // Mép trong cũng lượn như mép ngoài (mẫu G), gợn nhỏ hơn và lệch pha để hai mép không song song.
        rect.inset(bandWidth, bandWidth);
        outline(band, rect, INNER_WOBBLE_DP * density, 2);

        // Đường chữ chạy nằm giữa dải, theo chiều kim đồng hồ, đi hai vòng liền nhau (xem onDraw).
        tickerPath.reset();
        rect.set(margin + bandWidth / 2f, margin + bandWidth / 2f, w - margin - bandWidth / 2f, bottom - margin - bandWidth / 2f);
        addRoundRectLap(rect, corner, true);
        addRoundRectLap(rect, corner, false);
        // Chu vi hình chữ nhật bo góc: bốn cạnh thẳng (đã trừ phần góc) cộng bốn cung một phần tư = một đường tròn.
        lapLength = 2f * (rect.width() + rect.height()) - 8f * corner + (float) (2 * Math.PI * corner);
        fitTickerToLap();
    }

    /**
     * Thêm một vòng hình chữ nhật bo góc vào {@link #tickerPath} bằng nét liền (không đóng hình), bắt đầu ở đầu cạnh
     * trên. Hai vòng nối tiếp nhau thành một đường duy nhất, vì {@code drawTextOnPath} chỉ đi theo một đường liền.
     */
    private void addRoundRectLap(RectF r, float c, boolean first) {
        if (first) {
            tickerPath.moveTo(r.left + c, r.top);
        } else {
            tickerPath.lineTo(r.left + c, r.top);
        }
        tickerPath.lineTo(r.right - c, r.top);
        oval.set(r.right - 2 * c, r.top, r.right, r.top + 2 * c);
        tickerPath.arcTo(oval, 270f, 90f, false);
        tickerPath.lineTo(r.right, r.bottom - c);
        oval.set(r.right - 2 * c, r.bottom - 2 * c, r.right, r.bottom);
        tickerPath.arcTo(oval, 0f, 90f, false);
        tickerPath.lineTo(r.left + c, r.bottom);
        oval.set(r.left, r.bottom - 2 * c, r.left + 2 * c, r.bottom);
        tickerPath.arcTo(oval, 90f, 90f, false);
        tickerPath.lineTo(r.left, r.top + c);
        oval.set(r.left, r.top, r.left + 2 * c, r.top + 2 * c);
        tickerPath.arcTo(oval, 180f, 90f, false);
    }

    /**
     * Ghép một vòng chữ vừa khít chu vi viền. Space Mono là font đơn cách nên mọi ký tự rộng bằng nhau: số ký tự vừa
     * một vòng = phần nguyên của (chu vi / bề rộng một ký tự). {@link TickerText#fitToColumns} ghép các mục nguyên
     * cho đủ đúng số ký tự đó. Phần dư (chưa tới một ký tự) chỉ là một khoảng trống rất nhỏ ở chỗ nối vòng.
     */
    private void fitTickerToLap() {
        final float advance = tickerPaint.measureText("M");
        if (lapLength <= 0f || advance <= 0f || tickerItems.isEmpty()) {
            ticker = "";
            tickerWidth = 0f;
            return;
        }
        final int columns = (int) (lapLength / advance);
        ticker = TickerText.fitToColumns(tickerItems, columns);
        tickerWidth = tickerPaint.measureText(ticker);
    }

    /**
     * Mép lượn: "siêu ellipse" (chữ nhật bo góc tròn trịa) cộng gợn sóng nhỏ, đúng công thức của mẫu G:
     * x = cx + (hw + w)·sgn(cos t)·|cos t|^(2/k), tương tự với y, với k = 10 và
     * w(t) = A·sin(7t + φ) + 0,6A·sin(13t − 2φ), φ là pha riêng của mỗi mép.
     */
    private static void outline(Path path, RectF r, float wobble, int phase) {
        final double power = 2.0 / 10.0;
        final float cx = r.centerX(), cy = r.centerY(), hw = r.width() / 2f, hh = r.height() / 2f;
        for (int i = 0; i < OUTLINE_POINTS; i++) {
            final double t = 2 * Math.PI * i / OUTLINE_POINTS;
            final double c = Math.cos(t), s = Math.sin(t);
            final double w = wobble * (Math.sin(7 * t + phase) + 0.6 * Math.sin(13 * t - 2 * phase));
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
        final float travelled = animated
                ? (SystemClock.uptimeMillis() / 1000f * TICKER_SPEED_DP * density) % tickerWidth : 0f;
        // Đường dài hai vòng. Vòng chữ bắt đầu ở W − quãng đã chạy, nên phần cuối của nó vắt qua điểm nối sang vòng
        // đường thứ hai và phủ luôn đoạn đầu: chữ liền mạch, không chữ nào bị cắt ngang ở điểm nối.
        canvas.drawTextOnPath(ticker, tickerPath, tickerWidth - travelled, tickerBaseline, tickerPaint);
        if (animated) {
            postInvalidateOnAnimation();
        }
    }

    /**
     * Chạm xuống ngoài phần giữa sticker (trên dải viền, ở lề, hay ở khe giữa sticker và bảng chi tiết) thì giữ lại,
     * không cho lọt xuống sơ đồ: phần sơ đồ ở đó bị che hoặc bị cắt nên không nhìn thấy, không được chọn. Chạm ở phần
     * giữa thì bỏ qua để sơ đồ xử lý.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        final float x = event.getX(), y = event.getY();
        final float inner = margin + bandWidth;
        final boolean insideInner = x > inner && x < getWidth() - inner
                && y > inner && y < getHeight() - bottomInset - inner;
        return !insideInner;
    }
}
