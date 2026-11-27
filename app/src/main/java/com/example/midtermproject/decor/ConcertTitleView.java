package com.example.midtermproject.decor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;

import com.example.midtermproject.R;

/**
 * Tên đêm nhạc "Gieo" ở đầu màn hình (REQUIREMENTS Q49): chữ lượn Kablammo màu kem, viền tím dày vẽ phía sau chữ.
 * Mỗi chữ là một hình riêng để có thể rơi xuống lần lượt khi mở app rồi lắc lư chậm; thời gian và góc lấy từ
 * {@link TitleMotion}.
 * <p>
 * Vị trí và bề rộng từng chữ được đo một lần khi đổi chữ, nên {@link #onDraw} không cấp phát gì. Khi hiệu ứng hệ
 * thống bị tắt, chữ đứng yên ở vị trí cuối và View không vẽ lại liên tục.
 */
public class ConcertTitleView extends View {
    private static final float TEXT_SP = 44f;
    private static final float STROKE_DP = 2.5f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] motion = new float[3];
    private float density;
    private char[] letters = new char[0];
    private float[] letterWidths = new float[0];
    private float textWidth;
    private float ascent;
    private float descent;
    /** Mốc bắt đầu rơi; {@code Long.MIN_VALUE} là chưa vẽ lần nào. */
    private long start = Long.MIN_VALUE;
    /** Bỏ qua pha rơi (ví dụ khi Activity được tạo lại lúc xoay máy). */
    private boolean settled;
    private boolean animated = true;

    public ConcertTitleView(Context context) {
        super(context);
        init(context);
    }

    public ConcertTitleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConcertTitleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ConcertTitleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        final Typeface display = ResourcesCompat.getFont(context, R.font.kablammo);
        final float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SP,
                context.getResources().getDisplayMetrics());
        fill.setTypeface(display != null ? display : Typeface.DEFAULT_BOLD);
        fill.setTextSize(size);
        fill.setColor(ContextCompat.getColor(context, R.color.surface));
        stroke.set(fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        // Nét vẽ nằm giữa đường viền chữ nên dày gấp đôi: nửa trong bị chữ kem phủ, nửa ngoài lộ ra thành viền.
        stroke.setStrokeWidth(2f * STROKE_DP * density);
        stroke.setColor(ContextCompat.getColor(context, R.color.sticker_outline));
        setText(context.getString(R.string.concert_name));
        ViewCompat.setAccessibilityHeading(this, true);
    }

    /** Đổi chữ; đo vị trí từng chữ ngay tại đây. */
    public void setText(String text) {
        letters = text.toCharArray();
        letterWidths = new float[letters.length];
        fill.getTextWidths(text, letterWidths);
        textWidth = 0f;
        for (float w : letterWidths) {
            textWidth += w;
        }
        final Paint.FontMetrics metrics = fill.getFontMetrics();
        ascent = -metrics.ascent;
        descent = metrics.descent;
        setContentDescription(getContext().getString(R.string.concert_name_description, text));
        requestLayout();
        invalidate();
    }

    /** Bật hoặc tắt chuyển động (theo cài đặt hiệu ứng của hệ thống, xem MainActivity). */
    public void setAnimated(boolean animated) {
        this.animated = animated;
        invalidate();
    }

    /** Hiện chữ ở trạng thái đã rơi xong, không chạy lại pha rơi (dùng khi Activity được tạo lại). */
    public void showSettled() {
        settled = true;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int pad = (int) Math.ceil(STROKE_DP * density);
        final int w = (int) Math.ceil(textWidth) + 2 * pad + getPaddingLeft() + getPaddingRight();
        final int h = (int) Math.ceil(ascent + descent) + 2 * pad + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final long now = SystemClock.uptimeMillis();
        if (start == Long.MIN_VALUE) {
            // Lần vẽ đầu tiên: bắt đầu rơi từ đây, hoặc coi như đã rơi xong nếu không chạy pha rơi.
            start = settled || !animated ? now - TitleMotion.dropEnd(letters.length) : now;
        }
        final long elapsed = animated ? now - start : TitleMotion.dropEnd(letters.length);
        final float baseline = getPaddingTop() + STROKE_DP * density + ascent;
        float x = getPaddingLeft() + STROKE_DP * density;
        for (int i = 0; i < letters.length; i++) {
            TitleMotion.letterAt(i, elapsed, motion);
            final float half = letterWidths[i] / 2f;
            canvas.save();
            // Xoay quanh chân chữ (giữa đáy) rồi dịch theo phương dọc.
            canvas.translate(x + half, baseline + motion[TitleMotion.DY] * density);
            canvas.rotate(motion[TitleMotion.ROTATION]);
            final int alpha = Math.round(255 * motion[TitleMotion.ALPHA]);
            stroke.setAlpha(alpha);
            fill.setAlpha(alpha);
            canvas.drawText(letters, i, 1, -half, 0f, stroke);
            canvas.drawText(letters, i, 1, -half, 0f, fill);
            canvas.restore();
            x += letterWidths[i];
        }
        if (animated) {
            postInvalidateOnAnimation();
        }
    }
}
