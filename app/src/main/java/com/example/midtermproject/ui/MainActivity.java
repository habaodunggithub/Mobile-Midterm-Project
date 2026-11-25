package com.example.midtermproject.ui;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.midtermproject.R;
import com.example.midtermproject.data.MockVenueFactory;
import com.example.midtermproject.map.VenueMapStateSource;
import com.example.midtermproject.map.VenueMapView;
import com.example.midtermproject.model.SelectionResult;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.state.VenueMapState;
import com.google.android.material.snackbar.Snackbar;

/**
 * Màn hình duy nhất. Activity chỉ ghép các mảnh: tạo sơ đồ và {@link VenueMapState}, nghe cú chạm từ
 * {@link VenueMapView}, đưa mọi thay đổi qua state, và báo lý do khi chạm vào chỗ không chọn được.
 * Sơ đồ vẽ lại mỗi khi state đổi (dữ liệu đi một chiều: chạm, rồi state, rồi vẽ).
 */
public class MainActivity extends AppCompatActivity
        implements VenueMapView.OnSpotTapListener, DetailSheet.Callbacks {
    private VenueMapState state;
    private VenueMapView mapView;
    private DetailSheet detailSheet;
    private LegendCard legendCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Từ Android 15, ứng dụng nhắm SDK 35 trở lên luôn vẽ tràn xuống dưới thanh hệ thống;
        // gọi thêm ở đây để các máy cũ hơn có cùng hành vi.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        Venue venue = MockVenueFactory.create();
        state = new VenueMapState(venue);
        mapView = findViewById(R.id.map_view);
        mapView.bind(venue, new VenueMapStateSource(state));
        mapView.setOnSpotTapListener(this);

        detailSheet = new DetailSheet(findViewById(R.id.bottom_sheet), venue, state, this);
        legendCard = new LegendCard(findViewById(R.id.card_legend), findViewById(R.id.btn_legend));
        // Bảng chi tiết thu gọn che bao nhiêu thì cả sơ đồ lẫn bảng chú thích phải chừa bấy nhiêu.
        detailSheet.setPeekHeightListener(peekHeightPx -> {
            mapView.setBottomInset(peekHeightPx);
            legendCard.setBottomInset(peekHeightPx);
        });
        // Mọi thay đổi đều đi qua state; state báo thì cả sơ đồ lẫn bảng chi tiết cùng vẽ lại.
        state.addListener(() -> {
            mapView.invalidate();
            detailSheet.render();
        });

        // Chừa lề cho thanh trạng thái và vết khuyết màn hình bằng lề của khung chứa sơ đồ. Phía dưới thì
        // bảng chi tiết tự chừa chỗ cho thanh điều hướng (nền bảng kéo dài xuống tận đáy màn hình).
        final View mapContainer = findViewById(R.id.map_container);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mapContainer.getLayoutParams();
            params.setMargins(bars.left, bars.top, bars.right, 0);
            mapContainer.setLayoutParams(params);
            detailSheet.setInsets(bars.left, bars.right, bars.bottom);
            return insets;
        });

        // Reset View chỉ đưa góc nhìn về mặc định, không động tới bộ lọc, lựa chọn hay chỗ đang xem (AC-2.5).
        findViewById(R.id.btn_reset_view).setOnClickListener(v -> mapView.resetView());

        // La bàn chỉ hiện khi sơ đồ lệch khỏi góc mặc định; kim xoay cùng sơ đồ nên luôn chỉ về sân khấu.
        ImageButton compass = findViewById(R.id.btn_compass);
        compass.setOnClickListener(v -> mapView.rotateToDefault());
        mapView.setOnAngleChangedListener((angleRad, atDefault) -> {
            compass.setRotation((float) Math.toDegrees(angleRad));
            compass.setVisibility(atDefault ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onSpotTapped(Spot spot) {
        showResult(state.toggleSelection(spot.id));
    }

    /** Bấm ✕ ở một dòng: bỏ chỗ khỏi lựa chọn; sơ đồ và bảng cùng vẽ lại nhờ state báo (UC-07). */
    @Override
    public void onRemoveRequested(Spot spot) {
        state.removeSelection(spot.id);
    }

    /**
     * Bấm một dòng: thu bảng về dạng thu gọn để thấy sơ đồ (Q34), đặt chỗ làm chỗ đang xem rồi chạy
     * hiệu ứng đưa chỗ về giữa sơ đồ (UC-08, AC-5.4).
     */
    @Override
    public void onLocateRequested(Spot spot) {
        detailSheet.collapse();
        state.setFocused(spot.id);
        mapView.locateSpot(spot);
    }

    /** Chạm vào lối đi, sân khấu hoặc ngoài sơ đồ: bỏ chỗ đang xem, giữ nguyên lựa chọn (AC-3.8). */
    @Override
    public void onEmptyTapped() {
        state.setFocused(null);
    }

    /** Chọn hoặc bỏ chọn thành công thì rung nhẹ (Q33); bị từ chối thì báo lý do (M1 đến M5). */
    private void showResult(SelectionResult result) {
        switch (result) {
            case SELECTED:
            case DESELECTED:
                mapView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                break;
            case REJECTED_MINE:
                showMessage(R.string.msg_mine);
                break;
            case REJECTED_BOOKED:
                showMessage(R.string.msg_booked);
                break;
            case REJECTED_LOCKED:
                showMessage(R.string.msg_locked);
                break;
            case REJECTED_FILTERED:
                showMessage(R.string.msg_out_of_filter);
                break;
            case REJECTED_LIMIT:
                showMessage(R.string.msg_limit_reached);
                break;
            default:
                break;
        }
    }

    private void showMessage(@StringRes int message) {
        // Neo vào bảng chi tiết để thông báo nằm ngay trên bảng, không bị bảng hay thanh điều hướng che.
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.bottom_sheet)
                .show();
    }
}
