package com.example.midtermproject.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;

import com.example.midtermproject.R;
import com.example.midtermproject.decor.StickerOutline;
import com.example.midtermproject.decor.StickerRelief;

/** Giữ nền, viền và bóng đổ ôm quanh phần bảng đang hiện khi kéo. */
public final class FloatingSheetLayout extends LinearLayout {
    private static final float CORNER_DP = 26f;
    // Thẻ thấp hơn khung sơ đồ nên dùng cùng dáng gợn với biên độ nhỏ hơn.
    private static final float WAVE_SCALE = 0.7f;
    private final float density = getResources().getDisplayMetrics().density;
    private final float bandWidth = getResources().getDimension(R.dimen.sheet_band_width);
    private final int contentBottomInset = getResources().getDimensionPixelSize(R.dimen.sheet_content_inset);
    private final Path outerPath = new Path();
    private final Path innerPath = new Path();
    private final RectF contour = new RectF();
    private final StickerRelief relief;
    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int cardLeft;
    private int cardRight;
    private int cardBottom;

    public FloatingSheetLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        relief = new StickerRelief(context.getColor(R.color.sticker_band));
        surfacePaint.setColor(context.getColor(R.color.surface));
        setWillNotDraw(false);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // Bóng theo hình chữ nhật bo góc để dùng được cả trên các bản Android cũ.
                outline.setRoundRect(cardLeft, 0, cardRight, cardBottom, CORNER_DP * density);
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateCardBounds();
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        super.offsetTopAndBottom(offset);
        updateCardBounds();
    }

    private void updateCardBounds() {
        // BottomSheetBehavior dịch cả bảng. Khi thu gọn, danh sách vẫn được đo bên dưới đáy màn hình.
        final int visibleHeight = Math.min(getHeight(), ((View) getParent()).getHeight() - getTop());
        final int left = getPaddingLeft();
        final int right = getWidth() - getPaddingRight();
        final int bottom = Math.max(0, visibleHeight - getPaddingBottom());
        if (cardLeft == left && cardRight == right && cardBottom == bottom) {
            return;
        }
        cardLeft = left;
        cardRight = right;
        cardBottom = bottom;

        outerPath.rewind();
        innerPath.rewind();
        // Chừa biên độ gợn của mép ngoài để nét sóng nằm trọn trong thẻ.
        final float waveInset = 4f * density * WAVE_SCALE;
        contour.set(left + waveInset, waveInset, right - waveInset, bottom - waveInset);
        if (!contour.isEmpty()) {
            StickerOutline.addOuter(outerPath, contour, density, WAVE_SCALE);
            contour.inset(bandWidth, bandWidth);
            if (!contour.isEmpty()) {
                StickerOutline.addInner(innerPath, contour, density, WAVE_SCALE);
            }
        }
        contour.set(left, 0, right, bottom);
        relief.update(outerPath, innerPath, contour, bandWidth);
        invalidateOutline();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(outerPath, surfacePaint);
        relief.draw(canvas, outerPath, innerPath);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Khi kéo, đáy thẻ dịch qua danh sách. Giữ chữ và hiệu ứng chạm phía trên viền ở từng khung hình.
        final int saved = canvas.save();
        canvas.clipRect(cardLeft, 0, cardRight, cardBottom - contentBottomInset);
        canvas.clipPath(innerPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saved);
    }
}
