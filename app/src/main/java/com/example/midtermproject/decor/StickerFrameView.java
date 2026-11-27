package com.example.midtermproject.decor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.midtermproject.R;

/** Viền sticker cam lượn sóng, đè lên mép sơ đồ và để trống phần giữa. */
public class StickerFrameView extends View {
    /**
     * Bán kính bo góc của View nằm dưới viền (sơ đồ). Sơ đồ là hình chữ nhật lùi vào giữa dải viền; nếu để góc vuông
     * thì góc đó thò ra ngoài mép ngoài đã bo tròn của viền. Bo 44dp thì điểm góc (theo đường chéo, cách góc khung
     * khoảng 29dp) nằm giữa mép ngoài (khoảng 21dp, lồi ra tới 25dp ở chỗ gợn) và mép trong (khoảng 36dp), tức là
     * khuất dưới dải.
     */
    private static final float CONTENT_CORNER_DP = 44f;

    private StickerRelief relief;
    private final Path outerPath = new Path();
    private final Path innerPath = new Path();
    private final RectF rect = new RectF();

    private float density;
    private float margin;
    private float bandWidth;
    /**
     * Phần đáy khung dành cho bảng chi tiết (chiều cao bảng thu gọn + khe 10dp), tính bằng pixel. Sticker kết thúc
     * phía trên phần này, như mẫu G (Q48).
     */
    private int bottomInset;
    /** View nằm dưới viền (sơ đồ), để cập nhật phần bị cắt khi đáy sticker đổi. */
    private View content;

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

        relief = new StickerRelief(ContextCompat.getColor(context, R.color.sticker_band));

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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildPaths(w, h);
        if (content != null) {
            content.invalidateOutline();
        }
    }

    /** Dựng dải viền cho khung rộng {@code w}, cao {@code h} (đáy sticker = h − bottomInset). */
    private void buildPaths(int w, int h) {
        if (w == 0 || h == 0) {
            return;
        }
        final float bottom = h - bottomInset;
        outerPath.rewind();
        innerPath.rewind();
        rect.set(margin, margin, w - margin, bottom - margin);
        StickerOutline.addOuter(outerPath, rect, density);
        // Mép trong cũng lượn như mép ngoài (mẫu G), gợn nhỏ hơn và lệch pha để hai mép không song song.
        rect.inset(bandWidth, bandWidth);
        StickerOutline.addInner(innerPath, rect, density);
        rect.inset(-bandWidth, -bandWidth);
        relief.update(outerPath, innerPath, rect, bandWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        relief.draw(canvas, outerPath, innerPath);
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
