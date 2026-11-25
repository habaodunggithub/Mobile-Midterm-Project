package com.example.midtermproject.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.midtermproject.R;
import com.example.midtermproject.model.Venue;

/**
 * Custom View vẽ toàn bộ sơ đồ trên một Canvas duy nhất (yêu cầu bắt buộc của đề).
 * <p>
 * View chỉ lo việc ghép các mảnh: {@link MapCamera} giữ góc nhìn, {@link VenueRenderer} vẽ,
 * {@link SpotStateSource} cho biết trạng thái từng chỗ. Cử chỉ pan, zoom, xoay và chạm chọn được
 * thêm ở các chặng sau.
 * <p>
 * 4 constructor cùng gọi {@link #init} như trong bài giảng về Custom View.
 */
public class VenueMapView extends View {
    /** Lề giữa sơ đồ và mép vùng nhìn khi tính fit, tính bằng dp. */
    private static final float FIT_PADDING_DP = 16f;

    private MapCamera camera;
    private VenueRenderer renderer;
    private final Matrix worldToScreen = new Matrix();

    private Venue venue;
    private SpotStateSource states;
    private int bottomInsetPx;

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
        camera = new MapCamera(FIT_PADDING_DP * density);
        renderer = new VenueRenderer(context);
        setContentDescription(context.getString(R.string.map_content_description));
    }

    /** Gắn dữ liệu sơ đồ và nguồn trạng thái, rồi đưa góc nhìn về mặc định. */
    public void bind(Venue venue, SpotStateSource states) {
        this.venue = venue;
        this.states = states;
        camera.setWorldSize(venue.worldWidth, venue.worldHeight);
        camera.resetToFit();
        invalidate();
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
}
