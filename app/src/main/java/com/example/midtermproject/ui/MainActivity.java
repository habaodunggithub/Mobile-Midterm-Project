package com.example.midtermproject.ui;

import android.os.Bundle;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.midtermproject.R;
import com.example.midtermproject.data.MockVenueFactory;
import com.example.midtermproject.decor.ConcertTitleView;
import com.example.midtermproject.decor.LiquidPosterView;
import com.example.midtermproject.decor.StickerFrameView;
import com.example.midtermproject.decor.TickerText;
import com.example.midtermproject.map.VenueMapStateSource;
import com.example.midtermproject.map.VenueMapView;
import com.example.midtermproject.model.SelectionResult;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.model.ZoneType;
import com.example.midtermproject.state.SessionData;
import com.example.midtermproject.state.SessionStore;
import com.example.midtermproject.state.SpotFilter;
import com.example.midtermproject.state.VenueMapState;
import com.example.midtermproject.storage.PrefsSessionStore;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Màn hình duy nhất. Activity chỉ ghép các mảnh: tạo sơ đồ và {@link VenueMapState}, nghe cú chạm từ
 * {@link VenueMapView}, đưa mọi thay đổi qua state, và báo lý do khi chạm vào chỗ không chọn được.
 * Sơ đồ vẽ lại mỗi khi state đổi (dữ liệu đi một chiều: chạm, rồi state, rồi vẽ).
 */
public class MainActivity extends AppCompatActivity
        implements VenueMapView.OnSpotTapListener, DetailSheet.Callbacks, BookingConfirmDialog.Callback,
                   ResetDataDialog.Callback {
    // Các khóa của gói trạng thái tạm (REQUIREMENTS 7.2). Lựa chọn và vé của bạn không nằm trong đây:
    // chúng chỉ đến từ SessionStore để mỗi dữ liệu có đúng một nguồn.
    private static final String KEY_FOCUSED_ID = "state.focused_id";
    private static final String KEY_FILTER_ZONE = "filter.zone";
    private static final String KEY_FILTER_QUERY = "filter.query";
    private static final String KEY_FILTER_AVAILABLE_ONLY = "filter.available_only";
    private static final String KEY_FILTER_PRICE_RANGE = "filter.price_range";
    private static final String KEY_CAMERA_SNAPSHOT = "camera.snapshot";
    private static final String KEY_LEGEND_VISIBLE = "ui.legend_visible";

    private VenueMapState state;
    private SessionStore store;
    /** Bản đã ghi lần trước, để chỉ ghi khi lựa chọn hoặc vé của bạn thật sự đổi (đổi bộ lọc hay chỗ đang xem thì không ghi). */
    private List<String> savedSelected;
    private List<String> savedMine;
    private VenueMapView mapView;
    private DetailSheet detailSheet;
    private LegendCard legendCard;
    private FilterBar filterBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Từ Android 15, ứng dụng nhắm SDK 35 trở lên luôn vẽ tràn xuống dưới thanh hệ thống;
        // gọi thêm ở đây để các máy cũ hơn có cùng hành vi.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        StickerChrome.apply(findViewById(R.id.root));

        Venue venue = MockVenueFactory.create();
        state = new VenueMapState(venue);
        // Lựa chọn và vé của bạn đến từ dữ liệu đã lưu, và chỉ đọc ở đây một lần (ADR-08). Lần đầu mở app thì
        // dữ liệu rỗng (AC-7.7); mã sai hoặc hỏng được state tự loại (AC-7.6). Làm trước khi có người nghe state.
        store = new PrefsSessionStore(this);
        final SessionData saved = store.load();
        // Có gói tạm (xoay màn hình, hệ thống thu hồi process) thì lấy thêm chỗ đang xem và bộ lọc từ đó; không
        // thì dùng mặc định (AC-7.5). restore chỉ được gọi đúng một lần.
        final String focusedId = savedInstanceState == null ? null : savedInstanceState.getString(KEY_FOCUSED_ID);
        state.restore(saved.selected, saved.mine, focusedId, readFilter(savedInstanceState));
        savedSelected = state.snapshotSelectedIds();
        savedMine = state.snapshotMyTicketIds();
        mapView = findViewById(R.id.map_view);
        mapView.bind(venue, new VenueMapStateSource(state));
        // Mở màn chỉ chạy khi app mở mới, không chạy lại mỗi lần xoay máy (Activity được tạo lại).
        if (savedInstanceState == null) {
            mapView.playIntro();
        } else {
            // Tên "Gieo" cũng chỉ rơi xuống khi app mở mới; xoay máy thì hiện ngay ở vị trí cuối.
            ((ConcertTitleView) findViewById(R.id.concert_title)).showSettled();
        }
        // Dòng chữ chạy quanh viền sticker: tên khu và giá thấp nhất, dựng một lần từ dữ liệu.
        final StickerFrameView stickerFrame = findViewById(R.id.sticker_frame);
        stickerFrame.clipContent(mapView);
        stickerFrame.setTickerItems(TickerText.items(venue, getString(R.string.stage_label),
                getString(R.string.ticker_section_format), PriceFormatter::format));
        mapView.setOnSpotTapListener(this);

        // Các control lọc tự nghe state. Truyền cả khung gốc vì chúng nằm ở nhiều chỗ: thanh lọc phía trên, các thẻ
        // nổi trên sơ đồ và thông báo không có kết quả.
        filterBar = new FilterBar(findViewById(R.id.root), state);

        detailSheet = new DetailSheet(findViewById(R.id.bottom_sheet), venue, state, this);
        legendCard = new LegendCard(findViewById(R.id.card_legend), findViewById(R.id.btn_legend));
        if (savedInstanceState != null) {
            legendCard.setShown(savedInstanceState.getBoolean(KEY_LEGEND_VISIBLE, false));
            // View chưa có kích thước, nên ảnh chụp góc nhìn được áp khi View đo xong; ảnh hỏng thì dùng vừa khít.
            mapView.setPendingCameraSnapshot(savedInstanceState.getFloatArray(KEY_CAMERA_SNAPSHOT));
        }
        // Bảng chi tiết thu gọn che bao nhiêu thì sơ đồ, bảng chú thích và thông báo M7 phải chừa bấy nhiêu.
        // Sticker sơ đồ kết thúc cách bảng thu gọn một khe 10dp (mẫu G, Q48). Camera chừa thêm 16dp để vùng nhìn mặc định
        // cách mép trong của viền dưới đúng bằng ba cạnh kia.
        final int sheetGap = getResources().getDimensionPixelSize(R.dimen.sheet_side_margin);
        final int mapInset = getResources().getDimensionPixelSize(R.dimen.sticker_map_inset);
        detailSheet.setPeekHeightListener(peekHeightPx -> {
            final int covered = peekHeightPx + sheetGap;
            stickerFrame.setBottomInset(covered);
            mapView.setBottomInset(covered + mapInset);
            legendCard.setBottomInset(covered);
            filterBar.setBottomInset(covered);
        });
        // Mọi thay đổi đều đi qua state; state báo thì cả sơ đồ lẫn bảng chi tiết cùng vẽ lại.
        state.addListener(() -> {
            mapView.invalidate();
            detailSheet.render();
            saveSessionIfChanged();
        });

        // Cột nội dung chừa thanh trạng thái và vết khuyết. Bảng chi tiết chừa khe trên thanh điều hướng.
        final View contentColumn = findViewById(R.id.content_column);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) contentColumn.getLayoutParams();
            params.setMargins(bars.left, bars.top, bars.right, 0);
            contentColumn.setLayoutParams(params);
            detailSheet.setInsets(bars.left, bars.right, bars.bottom);
            return insets;
        });

        // Reset View chỉ đưa góc nhìn về mặc định, không động tới bộ lọc, lựa chọn hay chỗ đang xem (AC-2.5).
        findViewById(R.id.btn_reset_view).setOnClickListener(v -> mapView.resetView());

        // Nút ⋮ mở menu nhỏ (Q39); chọn "Làm mới dữ liệu" thì hỏi xác nhận trước (AC-8.1).
        final ImageButton moreButton = findViewById(R.id.btn_more);
        moreButton.setOnClickListener(v -> {
            final PopupMenu menu = new PopupMenu(this, moreButton);
            menu.inflate(R.menu.menu_map_more);
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_reset_data) {
                    showResetDialog();
                    return true;
                }
                return false;
            });
            menu.show();
        });

        // La bàn chỉ hiện khi sơ đồ lệch khỏi góc mặc định; kim xoay cùng sơ đồ nên luôn chỉ về sân khấu.
        ImageButton compass = findViewById(R.id.btn_compass);
        compass.setOnClickListener(v -> mapView.rotateToDefault());
        mapView.setOnAngleChangedListener((angleRad, atDefault) -> {
            compass.setRotation((float) Math.toDegrees(angleRad));
            compass.setVisibility(atDefault ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * Mỗi lần quay lại app thì đọc lại cài đặt hiệu ứng của hệ thống (người dùng có thể vừa đổi nó trong Cài đặt) và
     * bật hoặc tắt mọi chuyển động trang trí theo đó (Q43).
     */
    @Override
    protected void onResume() {
        super.onResume();
        final boolean animated = animationsEnabled();
        ((LiquidPosterView) findViewById(R.id.poster_background)).setAnimated(animated);
        ((ConcertTitleView) findViewById(R.id.concert_title)).setAnimated(animated);
        ((StickerFrameView) findViewById(R.id.sticker_frame)).setAnimated(animated);
        mapView.setEffectsEnabled(animated);
        detailSheet.setAnimated(animated);
    }

    /**
     * Hiệu ứng động bị tắt khi hệ số thời lượng animation của hệ thống bằng 0: đó là cách Android lưu lựa chọn
     * "Xóa hiệu ứng động" (Hỗ trợ tiếp cận) hoặc "Tỉ lệ thời lượng animator: tắt" (Tùy chọn cho nhà phát triển).
     * Đọc thẳng Settings.Global vì {@code ValueAnimator.areAnimatorsEnabled()} chỉ có từ Android 8 (API 26).
     */
    private boolean animationsEnabled() {
        return Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }

    /** Dựng lại bộ lọc từ gói tạm; gói thiếu hoặc khóa hỏng thì dùng giá trị mặc định (UC-11, error course). */
    private static SpotFilter readFilter(Bundle saved) {
        if (saved == null) {
            return SpotFilter.NONE;
        }
        ZoneType zone = null;
        final String zoneName = saved.getString(KEY_FILTER_ZONE);
        if (zoneName != null) {
            try {
                zone = ZoneType.valueOf(zoneName);
            } catch (IllegalArgumentException e) {
                zone = null;
            }
        }
        long minPrice = SpotFilter.PRICE_MIN;
        long maxPrice = SpotFilter.PRICE_MAX;
        final long[] range = saved.getLongArray(KEY_FILTER_PRICE_RANGE);
        if (range != null && range.length == 2) {
            minPrice = range[0];
            maxPrice = range[1];
        }
        final String query = saved.getString(KEY_FILTER_QUERY);
        return SpotFilter.NONE
                .withZone(zone)
                .withQuery(query == null ? "" : query)
                .withAvailableOnly(saved.getBoolean(KEY_FILTER_AVAILABLE_ONLY, false))
                .withPriceRange(minPrice, maxPrice);
    }

    /**
     * Ghi những thứ chưa nằm trong dữ liệu lưu bền: chỗ đang xem, bộ lọc, góc nhìn và bảng chú thích
     * (AC-7.1, AC-7.3). Góc xoay của sơ đồ không được ghi: sau khi tạo lại, góc về mặc định của hướng mới (AC-2.13).
     * Bảng chi tiết mở rộng hay thu gọn do {@code BottomSheetBehavior} tự lưu.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FOCUSED_ID, state.getFocusedId());
        final SpotFilter filter = state.getFilter();
        outState.putString(KEY_FILTER_ZONE, filter.zone == null ? null : filter.zone.name());
        outState.putString(KEY_FILTER_QUERY, filter.rawQuery);
        outState.putBoolean(KEY_FILTER_AVAILABLE_ONLY, filter.availableOnly);
        outState.putLongArray(KEY_FILTER_PRICE_RANGE, new long[]{filter.minPrice, filter.maxPrice});
        outState.putFloatArray(KEY_CAMERA_SNAPSHOT, mapView.getCameraSnapshot());
        outState.putBoolean(KEY_LEGEND_VISIBLE, legendCard.isShown());
    }

    /** Ghi lựa chọn (giữ thứ tự) và vé của bạn khi một trong hai khác bản đã ghi (UC-11). {@code apply()} nên không chặn giao diện. */
    private void saveSessionIfChanged() {
        final List<String> selected = state.snapshotSelectedIds();
        final List<String> mine = state.snapshotMyTicketIds();
        if (selected.equals(savedSelected) && mine.equals(savedMine)) {
            return;
        }
        store.save(selected, mine);
        savedSelected = selected;
        savedMine = mine;
    }

    @Override
    public void onSpotTapped(Spot spot) {
        final SelectionResult result = state.toggleSelection(spot.id);
        // Hiệu ứng trên chính chỗ vừa chạm (Q43): chọn hoặc bỏ chọn được thì chỗ nảy lên, bị từ chối thì chỗ lắc.
        if (result == SelectionResult.SELECTED || result == SelectionResult.DESELECTED) {
            mapView.playPop(spot);
        } else {
            mapView.playShake(spot);
        }
        showResult(result);
    }

    /** Bấm ✕ ở một dòng: bỏ chỗ khỏi lựa chọn; sơ đồ và bảng cùng vẽ lại nhờ state báo (UC-07). */
    @Override
    public void onRemoveRequested(Spot spot) {
        state.removeSelection(spot.id);
    }

    /** Bấm "Đặt N vé": hỏi xác nhận trước (AC-6.1). Dữ liệu hộp thoại lấy từ state ngay lúc bấm. */
    @Override
    public void onBookRequested() {
        // Bấm hai lần liên tiếp không được mở hai hộp thoại chồng nhau.
        if (state.getSelectedCount() == 0
                || getSupportFragmentManager().findFragmentByTag(BookingConfirmDialog.TAG) != null) {
            return;
        }
        BookingConfirmDialog.newInstance(state.snapshotSelectedIds(), state.getSelectedTotal())
                .show(getSupportFragmentManager(), BookingConfirmDialog.TAG);
    }

    /** Bấm "Xác nhận": đặt mọi chỗ đang chọn qua state rồi báo M6 (AC-6.2). */
    @Override
    public void onBookingConfirmed() {
        // Lấy danh sách chỗ trước khi đặt, vì đặt xong lựa chọn bị xóa; dùng để chạy làn sóng lật (Q43).
        final List<Spot> justBooked = state.getSelectedSpots();
        final int booked = state.bookSelection();
        if (booked > 0) {
            mapView.playBookingWave(justBooked);
            showMessage(getString(R.string.msg_booking_done, booked));
        }
    }

    private void showResetDialog() {
        if (getSupportFragmentManager().findFragmentByTag(ResetDataDialog.TAG) != null) {
            return;
        }
        ResetDataDialog.newInstance(state.getSelectedCount(), state.getMySpots().size())
                .show(getSupportFragmentManager(), ResetDataDialog.TAG);
    }

    /**
     * Bấm "Xác nhận": xóa lựa chọn, vé và chỗ đang xem qua state, xóa luôn dữ liệu đã lưu rồi báo M10.
     * Bộ lọc và góc nhìn không đổi (AC-8.2).
     */
    @Override
    public void onResetConfirmed() {
        // resetAll báo state, listener ghi bản rỗng; clear() sau đó xóa hẳn tệp lưu để mở lại app vẫn rỗng.
        state.resetAll();
        store.clear();
        showMessage(R.string.msg_data_reset);
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
        showMessage(getString(message));
    }

    private void showMessage(String message) {
        // Neo vào bảng chi tiết để thông báo nằm ngay trên bảng, không bị bảng hay thanh điều hướng che.
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.bottom_sheet)
                .show();
    }
}
