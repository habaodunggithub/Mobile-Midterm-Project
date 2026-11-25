package com.example.midtermproject.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.midtermproject.R;
import com.example.midtermproject.data.MockVenueFactory;
import com.example.midtermproject.map.SpotStateSource;
import com.example.midtermproject.map.VenueMapView;
import com.example.midtermproject.model.DisplayState;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.Venue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Từ Android 15, ứng dụng nhắm SDK 35 trở lên luôn vẽ tràn xuống dưới thanh hệ thống;
        // gọi thêm ở đây để các máy cũ hơn có cùng hành vi.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // Chừa lề cho thanh trạng thái, thanh điều hướng và vết khuyết màn hình.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        Venue venue = MockVenueFactory.create();
        VenueMapView mapView = findViewById(R.id.map_view);
        mapView.bind(venue, new PreviewStateSource());
    }

    /**
     * TẠM THỜI (gỡ ở Chặng 4, khi đã có VenueMapState): dữ liệu mẫu để xem đủ 5 trạng thái hiển thị và
     * vòng đánh dấu chỗ đang xem. Thứ tự ưu tiên giống REQUIREMENTS 6.3: vé của bạn, đã đặt, bị khóa,
     * đang chọn, còn trống.
     */
    private static final class PreviewStateSource implements SpotStateSource {
        private final Set<String> selected = new HashSet<>(Arrays.asList("VIP-G-B02", "VIP-G-B03", "STD-3-A03"));
        private final Set<String> mine = new HashSet<>(Arrays.asList("VIP-G-A05", "STA-A-A02", "STA-A-A03"));
        private final String focused = "VIP-G-B03";

        @Override
        public DisplayState displayStateOf(Spot spot) {
            if (mine.contains(spot.id)) {
                return DisplayState.MINE;
            }
            if (spot.baseStatus == SpotStatus.BOOKED) {
                return DisplayState.BOOKED;
            }
            if (spot.baseStatus == SpotStatus.LOCKED) {
                return DisplayState.LOCKED;
            }
            if (selected.contains(spot.id)) {
                return DisplayState.SELECTED;
            }
            return DisplayState.AVAILABLE;
        }

        @Override
        public boolean isDimmed(Spot spot) {
            return false;
        }

        @Override
        public boolean isFocused(Spot spot) {
            return spot.id.equals(focused);
        }
    }
}
