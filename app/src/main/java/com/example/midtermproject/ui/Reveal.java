package com.example.midtermproject.ui;

import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * Hiện và ẩn một thẻ nổi hay nút nhỏ bằng hiệu ứng "bung ra từ nút bấm" (REQUIREMENTS Q49): khi hiện, View phóng từ
 * 90% lên 100% và rõ dần trong 200ms, tâm phóng đặt ở phía nút đã mở nó; khi ẩn, View thu về 92% và mờ đi trong
 * 130ms (ẩn nhanh hơn hiện để giao diện có cảm giác nhanh nhạy) rồi mới GONE.
 * <p>
 * Chỉ đổi alpha và scale nên không làm xô lệch bố cục. View chưa gắn vào màn hình (ví dụ lúc khôi phục sau khi xoay
 * máy) hoặc khi hiệu ứng bị tắt thì đổi ngay, không chạy hiệu ứng.
 */
final class Reveal {
    private static final long SHOW_MS = 200L;
    private static final long HIDE_MS = 130L;
    private static final float SHOW_FROM_SCALE = 0.9f;
    private static final float HIDE_TO_SCALE = 0.92f;

    private Reveal() {
    }

    /**
     * @param pivotX, pivotY tâm phóng theo tỉ lệ bề rộng và chiều cao của View (0 là mép trái/trên, 1 là mép
     *                       phải/dưới), đặt về phía nút đã mở nó
     */
    static void show(View view, float pivotX, float pivotY, boolean animate) {
        view.animate().cancel();
        final boolean wasShown = view.getVisibility() == View.VISIBLE && view.getAlpha() == 1f;
        view.setVisibility(View.VISIBLE);
        if (!animate || !view.isAttachedToWindow() || wasShown) {
            resetTransform(view);
            return;
        }
        view.setAlpha(0f);
        view.setScaleX(SHOW_FROM_SCALE);
        view.setScaleY(SHOW_FROM_SCALE);
        // View vừa từ GONE sang VISIBLE nên chưa có kích thước; đợi tới ngay trước khi vẽ để đặt tâm phóng đúng.
        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                view.setPivotX(view.getWidth() * pivotX);
                view.setPivotY(view.getHeight() * pivotY);
                view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(SHOW_MS)
                        .setInterpolator(new DecelerateInterpolator()).withEndAction(null).start();
                return true;
            }
        });
    }

    static void hide(View view, boolean animate) {
        view.animate().cancel();
        if (view.getVisibility() != View.VISIBLE) {
            return;
        }
        if (!animate || !view.isAttachedToWindow()) {
            view.setVisibility(View.GONE);
            resetTransform(view);
            return;
        }
        view.animate().alpha(0f).scaleX(HIDE_TO_SCALE).scaleY(HIDE_TO_SCALE).setDuration(HIDE_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    resetTransform(view);
                })
                .start();
    }

    private static void resetTransform(View view) {
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }
}
