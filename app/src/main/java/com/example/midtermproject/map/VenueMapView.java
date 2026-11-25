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
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.example.midtermproject.R;
import com.example.midtermproject.model.Venue;

/**
 * Custom View vẽ toàn bộ sơ đồ trên một Canvas duy nhất (yêu cầu bắt buộc của đề).
 * <p>
 * View chỉ lo việc ghép các mảnh: {@link MapCamera} giữ góc nhìn, {@link VenueRenderer} vẽ,
 * {@link SpotStateSource} cho biết trạng thái từng chỗ. Cử chỉ: kéo một ngón để pan; chụm hai ngón
 * để zoom quanh tâm hai ngón; xoay hai ngón để xoay sơ đồ (tự chỉnh thẳng khi nhấc tay gần góc
 * vuông); hai ngón cùng dịch chuyển cũng kéo sơ đồ theo; vuốt nhanh rồi nhấc tay thì sơ đồ trượt
 * tiếp và chậm dần. Chạm chọn được thêm ở chặng sau.
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
    /** Thời gian hiệu ứng đưa sơ đồ về góc mặc định bằng nút la bàn (AC-2.11). */
    private static final long COMPASS_DURATION_MS = 300L;
    /** Thời gian hiệu ứng Reset View (AC-2.5). */
    private static final long RESET_DURATION_MS = 300L;
    /** Hai ngón phải xoay quá ngần này độ thì sơ đồ mới bắt đầu xoay (REQUIREMENTS Q32). */
    private static final float ROTATION_THRESHOLD_DEG = 6f;
    /** Nhấc tay khi lệch không quá ngần này độ so với góc vuông thì tự chỉnh thẳng (AC-2.10). */
    private static final float SNAP_TOLERANCE_DEG = 8f;
    private static final long SNAP_DURATION_MS = 150L;

    private MapCamera camera;
    private VenueRenderer renderer;
    private final Matrix worldToScreen = new Matrix();

    private Venue venue;
    private SpotStateSource states;
    private int bottomInsetPx;
    /** Hiệu ứng camera đang chạy (Reset View, tự chỉnh thẳng, la bàn); chạm vào màn hình sẽ hủy nó. */
    private ValueAnimator cameraAnimator;

    private OnAngleChangedListener angleListener;
    private float notifiedAngle = Float.NaN;
    private boolean notifiedAtDefault;

    private GestureDetector gestureDetector;
    private float touchSlop;
    private final RotationTracker rotationTracker =
            new RotationTracker((float) Math.toRadians(ROTATION_THRESHOLD_DEG));

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

    /** Được báo mỗi khi góc xoay của sơ đồ đổi, để Activity xoay kim và ẩn/hiện nút la bàn. */
    public interface OnAngleChangedListener {
        void onAngleChanged(float angleRad, boolean atDefault);
    }

    public void setOnAngleChangedListener(OnAngleChangedListener listener) {
        this.angleListener = listener;
        notifiedAngle = Float.NaN;
        notifyAngleIfChanged();
    }

    /** Nút la bàn: xoay về góc mặc định theo chiều ngắn nhất, giữ tâm và tỉ lệ (AC-2.11). */
    public void rotateToDefault() {
        if (camera.isReady()) {
            animateRotationAboutCenter(
                    MapCamera.shortestAngleDelta(camera.getAngle(), camera.defaultAngle()), COMPASS_DURATION_MS);
        }
    }

    /** Mọi chỗ làm đổi camera đều gọi hàm này để vẽ lại và báo cho người nghe góc xoay. */
    private void onCameraChanged() {
        invalidate();
        notifyAngleIfChanged();
    }

    private void notifyAngleIfChanged() {
        if (angleListener == null || !camera.isReady()) {
            return;
        }
        final float angle = camera.getAngle();
        final boolean atDefault = camera.isAtDefaultAngle();
        if (angle != notifiedAngle || atDefault != notifiedAtDefault) {
            notifiedAngle = angle;
            notifiedAtDefault = atDefault;
            angleListener.onAngleChanged(angle, atDefault);
        }
    }

    /** Gắn dữ liệu sơ đồ và nguồn trạng thái, rồi đưa góc nhìn về mặc định. */
    public void bind(Venue venue, SpotStateSource states) {
        this.venue = venue;
        this.states = states;
        camera.setWorldSize(venue.worldWidth, venue.worldHeight);
        camera.resetToFit();
        onCameraChanged();
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
            onCameraChanged();
        });
        cameraAnimator.start();
    }

    /**
     * Xoay sơ đồ thêm {@code delta} radian quanh tâm vùng nhìn trong {@code durationMs}. Mỗi khung
     * hình xoay thêm phần góc của khung đó, nên điểm ở tâm vùng nhìn đứng yên.
     */
    private void animateRotationAboutCenter(float delta, long durationMs) {
        cancelCameraAnimation();
        final float centerX = getWidth() / 2f;
        final float centerY = (getHeight() - bottomInsetPx) / 2f;
        final float[] previous = {0f};
        cameraAnimator = ValueAnimator.ofFloat(0f, 1f);
        cameraAnimator.setDuration(durationMs);
        cameraAnimator.setInterpolator(new DecelerateInterpolator());
        cameraAnimator.addUpdateListener(animation -> {
            final float f = (float) animation.getAnimatedValue();
            camera.rotateBy(delta * (f - previous[0]), centerX, centerY);
            previous[0] = f;
            onCameraChanged();
        });
        cameraAnimator.start();
    }

    /**
     * Trượt tiếp sau khi nhấc tay với vận tốc (vx, vy) px/giây, chậm dần theo {@link FlingDecay}.
     * Mỗi khung hình dịch thêm phần quãng đường của khung đó qua {@link MapCamera#panBy}, nên luôn bị
     * kẹp trong giới hạn; dừng khi hết đà hoặc khi chạm biên làm sơ đồ không dịch được nữa (AC-2.14).
     */
    private void startFling(float vx, float vy) {
        final long duration = FlingDecay.durationMillis((float) Math.hypot(vx, vy));
        if (duration <= 0L) {
            return;
        }
        cancelCameraAnimation();
        final float[] travelled = {0f, 0f};
        cameraAnimator = ValueAnimator.ofFloat(0f, 1f);
        cameraAnimator.setDuration(duration);
        cameraAnimator.setInterpolator(new LinearInterpolator());
        cameraAnimator.addUpdateListener(animation -> {
            final long elapsed = animation.getCurrentPlayTime();
            final float dx = FlingDecay.distanceAt(vx, elapsed) - travelled[0];
            final float dy = FlingDecay.distanceAt(vy, elapsed) - travelled[1];
            travelled[0] += dx;
            travelled[1] += dy;
            final float beforeX = camera.getTx();
            final float beforeY = camera.getTy();
            camera.panBy(dx, dy);
            onCameraChanged();
            // Dừng khi đã muốn dịch nhưng camera không dịch được nữa (chạm biên).
            final boolean wantedToMove = Math.abs(dx) > 0.01f || Math.abs(dy) > 0.01f;
            if (wantedToMove && camera.getTx() == beforeX && camera.getTy() == beforeY) {
                animation.cancel();
            }
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
        onCameraChanged();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        cancelCameraAnimation();
        camera.setViewport(width, height, bottomInsetPx);
        camera.resetToFit();
        notifyAngleIfChanged();
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
            if (twoFingersDown) {
                twoFingersDown = false;
                finishTwoFingerGesture(action != MotionEvent.ACTION_CANCEL);
            }
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
            rotationTracker.begin(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
            return;
        }
        if (action != MotionEvent.ACTION_MOVE) {
            return;
        }
        camera.panBy(focusX - lastFocusX, focusY - lastFocusY);
        lastFocusX = focusX;
        lastFocusY = focusY;

        final float rotation = rotationTracker.update(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
        if (rotation != 0f) {
            camera.rotateBy(rotation, focusX, focusY);
        }

        if (!zooming && Math.abs(span - initialSpan) > touchSlop) {
            zooming = true;
            lastSpan = span;
        } else if (zooming && lastSpan > 0f) {
            camera.zoomBy(span / lastSpan, focusX, focusY);
            lastSpan = span;
        }
        onCameraChanged();
    }

    /** Khi nhấc ngón: nếu vừa xoay và góc gần một góc vuông thì tự chỉnh thẳng (AC-2.10). */
    private void finishTwoFingerGesture(boolean allowSnap) {
        if (!allowSnap || !rotationTracker.isEngaged()) {
            return;
        }
        final float target = camera.nearestRightAngle((float) Math.toRadians(SNAP_TOLERANCE_DEG));
        if (!Float.isNaN(target)) {
            animateRotationAboutCenter(MapCamera.shortestAngleDelta(camera.getAngle(), target), SNAP_DURATION_MS);
        }
    }

    /** Kéo một ngón: pan theo ngón tay (AC-2.1). */
    private final class PanListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            // Phải trả về true, nếu không GestureDetector bỏ qua cả chuỗi cử chỉ.
            return true;
        }

        @Override
        public boolean onFling(MotionEvent first, MotionEvent current, float velocityX, float velocityY) {
            if (current.getPointerCount() > 1 || twoFingersDown) {
                return false;
            }
            startFling(velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent first, MotionEvent current, float distanceX, float distanceY) {
            if (current.getPointerCount() > 1) {
                return false;
            }
            // distanceX là (vị trí cũ - vị trí mới), nên dịch sơ đồ theo chiều ngược lại.
            camera.panBy(-distanceX, -distanceY);
            onCameraChanged();
            return true;
        }
    }
}
