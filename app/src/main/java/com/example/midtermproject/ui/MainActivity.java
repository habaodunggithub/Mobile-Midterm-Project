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
        mapView.bind(venue, new BaseStatusSource());
    }

    /** Tạm thời: chỉ tô theo trạng thái gốc, chưa có lựa chọn, bộ lọc hay chỗ đang xem. */
    private static final class BaseStatusSource implements SpotStateSource {
        @Override
        public DisplayState displayStateOf(Spot spot) {
            if (spot.baseStatus == SpotStatus.BOOKED) {
                return DisplayState.BOOKED;
            }
            if (spot.baseStatus == SpotStatus.LOCKED) {
                return DisplayState.LOCKED;
            }
            return DisplayState.AVAILABLE;
        }

        @Override
        public boolean isDimmed(Spot spot) {
            return false;
        }

        @Override
        public boolean isFocused(Spot spot) {
            return false;
        }
    }
}
