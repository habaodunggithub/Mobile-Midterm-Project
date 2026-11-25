package com.example.midtermproject.map;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.example.midtermproject.R;
import com.example.midtermproject.model.Venue;

/**
 * Custom View vẽ toàn bộ sơ đồ trên một Canvas duy nhất (yêu cầu bắt buộc của đề).
 * <p>
 * View chỉ lo việc ghép các mảnh: {@link MapCamera} giữ góc nhìn, {@link VenueRenderer} vẽ,
 * {@link SpotStateSource} cho biết trạng thái từng chỗ. Cử chỉ: kéo một ngón để pan; chụm hai ngón
 * để zoom quanh tâm hai ngón; hai ngón cùng dịch chuyển cũng kéo sơ đồ theo. Xoay, quán tính và
 * chạm chọn được thêm ở các commit và chặng sau.
 * <p>
 * Zoom hai ngón được tự đo (khoảng cách hai ngón) thay vì dùng {@code ScaleGestureDetector}, vì
 * detector đó chỉ bắt đầu khi hai ngón cách nhau ít nhất {@code getScaledMinimumScalingSpan()};
 * giá trị này đo được 446px (khoảng 170dp) trên emulator Android 15, nên zoom sẽ bị trễ rất khó chịu.
 * <p>
 * 4 constructor cùng gọi {@link #init} như trong bài giảng về Custom View.
 */
public class VenueMapView extends View {
    /** Lề giữa sơ đồ và mép vùng nhìn khi tính fit, tính bằng dp. */
    private static final float FIT_PADDING_DP = 16f;
    /** Khi kéo, mép sơ đồ được lùi vào trong vùng nhìn tối đa ngần này dp (AC-2.2). */
    private static final float PAN_MARGIN_DP = 48f;
    /** Thời gian hiệu ứng Reset View (AC-2.5). */
    private static final long RESET_DURATION_MS = 300L;

    private MapCamera camera;
    private VenueRenderer renderer;
    private final Matrix worldToScreen = new Matrix();

    private Venue venue;
    private SpotStateSource states;
    private int bottomInsetPx;
    /** Hiệu ứng camera đang chạy (Reset View, tự chỉnh thẳng, la bàn); chạm vào màn hình sẽ hủy nó. */
    private ValueAnimator cameraAnimator;

    private GestureDetector gestureDetector;
    private float touchSlop;

    // Trạng thái cử chỉ hai ngón.
    private boolean twoFingersDown;
    private float lastFocusX;
    private float lastFocusY;
    private float initialSpan;
    private float lastSpan;
    /** Khoảng cách hai ngón đã đổi quá {@code touchSlop}: bắt đầu zoom. */
    private boolean zooming;

    public VenueMapView(Context context) {
        super(context);
        init(context);
    }

    public VenueMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VenueMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public VenueMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        final float density = context.getResources().getDisplayMetrics().density;
        camera = new MapCamera(FIT_PADDING_DP * density, PAN_MARGIN_DP * density);
        renderer = new VenueRenderer(context);
        setContentDescription(context.getString(R.string.map_content_description));

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        gestureDetector = new GestureDetector(context, new PanListener());
    }

    /** Gắn dữ liệu sơ đồ và nguồn trạng thái, rồi đưa góc nhìn về mặc định. */
    public void bind(Venue venue, SpotStateSource states) {
        this.venue = venue;
        this.states = states;
        camera.setWorldSize(venue.worldWidth, venue.worldHeight);
        camera.resetToFit();
        invalidate();
    }

    /** Đưa sơ đồ về góc nhìn mặc định (vị trí, tỉ lệ và góc xoay) bằng hiệu ứng mượt (AC-2.5). */
    public void resetView() {
        animateCameraTo(camera.fitTarget(), RESET_DURATION_MS);
    }

    /**
     * Nội suy camera từ trạng thái hiện tại tới {tỉ lệ, góc, tx, ty} của {@code target}. Góc đi theo
     * chiều ngắn nhất. Một hiệu ứng mới thay thế hiệu ứng đang chạy.
     */
    private void animateCameraTo(float[] target, long durationMs) {
        cancelCameraAnimation();
        final float startScale = camera.getScale();
        final float startAngle = camera.getAngle();
        final float startTx = camera.getTx();
        final float startTy = camera.getTy();
        final float angleDelta = MapCamera.shortestAngleDelta(startAngle, target[1]);

        cameraAnimator = ValueAnimator.ofFloat(0f, 1f);
        cameraAnimator.setDuration(durationMs);
        cameraAnimator.setInterpolator(new DecelerateInterpolator());
        cameraAnimator.addUpdateListener(animation -> {
            final float f = (float) animation.getAnimatedValue();
            camera.set(startScale + (target[0] - startScale) * f,
                    MapCamera.normalizeAngle(startAngle + angleDelta * f),
                    startTx + (target[2] - startTx) * f,
                    startTy + (target[3] - startTy) * f);
            invalidate();
        });
        cameraAnimator.start();
    }

    private void cancelCameraAnimation() {
        if (cameraAnimator != null) {
            cameraAnimator.cancel();
            cameraAnimator = null;
        }
    }

    /** Chiều cao phần dưới bị bảng chi tiết (thu gọn) che; sơ đồ được căn giữa phía trên phần đó. */
    public void setBottomInset(int px) {
        bottomInsetPx = px;
        camera.setViewport(getWidth(), getHeight(), bottomInsetPx);
        camera.resetToFit();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        cancelCameraAnimation();
        camera.setViewport(width, height, bottomInsetPx);
        camera.resetToFit();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (venue == null || states == null || !camera.isReady()) {
            return;
        }
        // screen = s * R(theta) * world + t: phóng, xoay quanh gốc, rồi dịch.
        worldToScreen.setScale(camera.getScale(), camera.getScale());
        worldToScreen.postRotate((float) Math.toDegrees(camera.getAngle()));
        worldToScreen.postTranslate(camera.getTx(), camera.getTy());
        renderer.draw(canvas, venue, states, worldToScreen, camera.getScale(), camera.getAngle());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (venue == null || !camera.isReady()) {
            return super.onTouchEvent(event);
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // Chạm vào màn hình giữa lúc đang có hiệu ứng thì dừng ngay tại góc nhìn hiện tại (AC-2.6).
            cancelCameraAnimation();
        }
        trackTwoFingers(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    /**
     * Cử chỉ hai ngón. Mỗi lần di chuyển làm lần lượt: (1) kéo sơ đồ theo độ dịch của tâm hai ngón,
     * (2) zoom theo tỉ số khoảng cách hai ngón so với lần trước, quanh tâm mới. Nhờ thứ tự này,
     * điểm sơ đồ dưới tâm hai ngón luôn bám theo tay (AC-2.3).
     * <p>
     * Zoom chỉ bắt đầu khi khoảng cách hai ngón đã đổi quá {@code touchSlop}, để hai ngón cùng kéo
     * không vô tình phóng to thu nhỏ; phần trước ngưỡng bị bỏ nên không bị giật khi bắt đầu.
     */
    private void trackTwoFingers(MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL || event.getPointerCount() < 2) {
            twoFingersDown = false;
            return;
        }
        final float focusX = (event.getX(0) + event.getX(1)) / 2f;
        final float focusY = (event.getY(0) + event.getY(1)) / 2f;
        final float span = (float) Math.hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0));
        if (!twoFingersDown || action == MotionEvent.ACTION_POINTER_DOWN) {
            twoFingersDown = true;
            lastFocusX = focusX;
            lastFocusY = focusY;
            initialSpan = span;
            lastSpan = span;
            zooming = false;
            return;
        }
        if (action != MotionEvent.ACTION_MOVE) {
            return;
        }
        camera.panBy(focusX - lastFocusX, focusY - lastFocusY);
        lastFocusX = focusX;
        lastFocusY = focusY;

        if (!zooming && Math.abs(span - initialSpan) > touchSlop) {
            zooming = true;
            lastSpan = span;
        } else if (zooming && lastSpan > 0f) {
            camera.zoomBy(span / lastSpan, focusX, focusY);
            lastSpan = span;
        }
        invalidate();
    }

    /** Kéo một ngón: pan theo ngón tay (AC-2.1). */
    private final class PanListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            // Phải trả về true, nếu không GestureDetector bỏ qua cả chuỗi cử chỉ.
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent first, MotionEvent current, float distanceX, float distanceY) {
            if (current.getPointerCount() > 1) {
                return false;
            }
            // distanceX là (vị trí cũ - vị trí mới), nên dịch sơ đồ theo chiều ngược lại.
            camera.panBy(-distanceX, -distanceY);
            invalidate();
            return true;
        }
    }
}
