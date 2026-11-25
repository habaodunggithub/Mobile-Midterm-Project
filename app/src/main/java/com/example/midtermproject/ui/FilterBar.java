package com.example.midtermproject.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import com.example.midtermproject.R;
import com.example.midtermproject.model.ZoneType;
import com.example.midtermproject.state.SpotFilter;
import com.example.midtermproject.state.VenueMapState;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * Điều khiển các control lọc: ô tìm kiếm, chip khu, công tắc "Chỉ chỗ trống" và thanh khoảng giá.
 * Mỗi thao tác của người dùng chỉ gọi {@code state.setFilter(...)}; ngược lại, mỗi khi state báo bộ
 * lọc đổi (ví dụ Xóa lọc hoặc khôi phục khi mở lại), các control được cập nhật theo state. Nhờ vậy bộ
 * lọc chỉ có một bản thật, nằm ở {@link VenueMapState}, còn control chỉ là hình chiếu của nó.
 */
final class FilterBar {
    /** Bước của thanh giá: 100.000 đồng. Từ PRICE_MIN tới PRICE_MAX có đúng {@link #PRICE_STEPS} bước. */
    private static final float PRICE_STEP = 100_000f;
    private static final int PRICE_STEPS = Math.round((SpotFilter.PRICE_MAX - SpotFilter.PRICE_MIN) / PRICE_STEP);
    /** Dấu ngăn các tiêu chí trong dòng "Đang lọc". */
    private static final String CRITERIA_SEPARATOR = " · ";

    private final Context context;
    private final VenueMapState state;
    private final TextInputEditText search;
    private final ChipGroup zoneChips;
    private final SwitchMaterial availableOnly;
    private final RangeSlider priceSlider;
    private final TextView priceRange;
    private final View statusCard;
    private final TextView statusText;
    private final View noResultBanner;
    /** Lề dưới ban đầu của thông báo M7, để cộng thêm chiều cao bảng chi tiết thay vì thay thế nó. */
    private final int bannerEdgeMarginPx;
    /** Đang cập nhật control từ state: bỏ qua sự kiện do chính việc đó gây ra, để không lặp vô hạn. */
    private boolean syncing;
    /** Ngón tay đang kéo thanh giá: chỉ áp dụng khi nhấc tay để không tính lại bộ lọc liên tục. */
    private boolean draggingPrice;

    /** @param root view chứa mọi control lọc (cả thanh lọc lẫn thẻ giá nổi trên sơ đồ) */
    FilterBar(View root, VenueMapState state) {
        this.context = root.getContext();
        this.state = state;
        this.search = root.findViewById(R.id.et_search);
        this.zoneChips = root.findViewById(R.id.chip_group_zone);
        this.availableOnly = root.findViewById(R.id.switch_available_only);
        this.priceSlider = root.findViewById(R.id.range_price);
        this.priceRange = root.findViewById(R.id.tv_price_range);
        this.statusCard = root.findViewById(R.id.layout_filter_status);
        this.statusText = root.findViewById(R.id.tv_filter_status);
        this.noResultBanner = root.findViewById(R.id.layout_no_result);
        this.bannerEdgeMarginPx = ((ViewGroup.MarginLayoutParams) noResultBanner.getLayoutParams()).bottomMargin;

        bindSearch();
        bindZoneChips();
        bindAvailableOnly();
        bindPriceSlider();
        bindAdvancedToggle(root);
        // Xóa lọc đưa mọi tiêu chí về mặc định; lựa chọn và vé của bạn không bị đụng tới (AC-4.8).
        // Có hai nút cùng làm việc này: ở dòng "Đang lọc" và ở thông báo không có kết quả.
        final View.OnClickListener clearFilter = v -> state.setFilter(SpotFilter.NONE);
        root.findViewById(R.id.btn_clear_filter).setOnClickListener(clearFilter);
        root.findViewById(R.id.btn_clear_filter_no_result).setOnClickListener(clearFilter);

        state.addListener(this::syncFromState);
        syncFromState();
    }

    private void bindSearch() {
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (!syncing) {
                    state.setFilter(state.getFilter().withQuery(text.toString()));
                }
            }
        });
        // Bấm nút tìm trên bàn phím chỉ để thu bàn phím: bộ lọc đã áp dụng từ lúc gõ.
        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return false;
            }
            hideKeyboard(view);
            view.clearFocus();
            return true;
        });
    }

    private void bindZoneChips() {
        zoneChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!syncing && !checkedIds.isEmpty()) {
                state.setFilter(state.getFilter().withZone(zoneOf(checkedIds.get(0))));
            }
        });
    }

    private void bindAvailableOnly() {
        availableOnly.setOnCheckedChangeListener((button, checked) -> {
            if (!syncing) {
                state.setFilter(state.getFilter().withAvailableOnly(checked));
            }
        });
    }

    private void bindPriceSlider() {
        // Hai đầu và bước lấy từ SpotFilter để thanh giá và bộ lọc luôn cùng một khoảng.
        priceSlider.setValueFrom(SpotFilter.PRICE_MIN);
        priceSlider.setValueTo(SpotFilter.PRICE_MAX);
        priceSlider.setStepSize(PRICE_STEP);
        // Nhãn nổi trên núm kéo: "1,9 tr".
        priceSlider.setLabelFormatter(value ->
                context.getString(R.string.price_million, PriceFormatter.formatMillions((long) value)));
        priceSlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(RangeSlider slider) {
                draggingPrice = true;
            }

            @Override
            public void onStopTrackingTouch(RangeSlider slider) {
                draggingPrice = false;
                applyPriceFromSlider();
            }
        });
        // Dòng chữ khoảng giá chạy theo núm khi kéo. Thay đổi không do chạm (bàn phím, trình đọc màn hình)
        // không có lúc "nhấc tay" nên áp dụng ngay.
        priceSlider.addOnChangeListener((slider, value, fromUser) -> {
            showPriceRange();
            if (!syncing && !draggingPrice) {
                applyPriceFromSlider();
            }
        });
    }

    /** Nút "Giá" mở và đóng thẻ giá nổi trên sơ đồ; bố cục ngang để thẻ luôn hiện nên không có nút này. */
    private void bindAdvancedToggle(View root) {
        final MaterialButton toggle = root.findViewById(R.id.btn_filter_advanced);
        final View priceCard = root.findViewById(R.id.card_price_filter);
        if (toggle != null) {
            toggle.addOnCheckedChangeListener((button, checked) ->
                    priceCard.setVisibility(checked ? View.VISIBLE : View.GONE));
        }
    }

    private void applyPriceFromSlider() {
        final List<Float> values = priceSlider.getValues();
        state.setFilter(state.getFilter().withPriceRange(Math.round(values.get(0)), Math.round(values.get(1))));
    }

    private void showPriceRange() {
        final List<Float> values = priceSlider.getValues();
        priceRange.setText(context.getString(R.string.filter_price_range,
                PriceFormatter.formatMillions(Math.round(values.get(0))),
                PriceFormatter.formatMillions(Math.round(values.get(1)))));
    }

    /** Đưa các control về đúng bộ lọc hiện tại của state. */
    private void syncFromState() {
        final SpotFilter filter = state.getFilter();
        syncing = true;
        // Chỉ đặt lại chữ khi khác, nếu không con trỏ nhảy về đầu mỗi lần người dùng gõ một ký tự.
        if (!search.getText().toString().equals(filter.rawQuery)) {
            search.setText(filter.rawQuery);
            search.setSelection(search.length());
        }
        zoneChips.check(chipIdOf(filter.zone));
        availableOnly.setChecked(filter.availableOnly);
        // Không đặt lại khi đang kéo: thanh giá đang là nguồn của giá trị, state sẽ cập nhật sau khi nhấc tay.
        if (!draggingPrice) {
            priceSlider.setValues(snapToStep(filter.minPrice), snapToStep(filter.maxPrice));
        }
        showPriceRange();
        syncing = false;
        renderStatus();
        // Không chỗ nào khớp: mọi chỗ đã bị làm mờ, hiện thông báo M7 kèm nút Xóa lọc (AC-4.7).
        noResultBanner.setVisibility(state.getMatchCount() == 0 ? View.VISIBLE : View.GONE);
    }

    /** Đặt thông báo M7 giữa phần sơ đồ nằm trên bảng chi tiết đang thu gọn (cao {@code peekHeightPx}). */
    void setBottomInset(int peekHeightPx) {
        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) noResultBanner.getLayoutParams();
        params.bottomMargin = peekHeightPx + bannerEdgeMarginPx;
        noResultBanner.setLayoutParams(params);
    }

    /** Dòng "Đang lọc" chỉ hiện khi có ít nhất một tiêu chí khác mặc định (AC-4.6). */
    private void renderStatus() {
        final SpotFilter filter = state.getFilter();
        statusCard.setVisibility(filter.isActive() ? View.VISIBLE : View.GONE);
        if (filter.isActive()) {
            statusText.setText(context.getString(R.string.filter_status, describe(filter), state.getMatchCount()));
        }
    }

    /** Liệt kê các tiêu chí khác mặc định, ví dụ: {@code VIP · "A0" · Chỉ chỗ trống · 1,9–2,5 tr}. */
    private String describe(SpotFilter filter) {
        final StringBuilder text = new StringBuilder();
        if (filter.zone != null) {
            appendCriterion(text, context.getString(zoneNameOf(filter.zone)));
        }
        if (!filter.normalizedQuery.isEmpty()) {
            appendCriterion(text, context.getString(R.string.filter_status_query, filter.rawQuery.trim()));
        }
        if (filter.availableOnly) {
            appendCriterion(text, context.getString(R.string.filter_available_only));
        }
        if (filter.minPrice > SpotFilter.PRICE_MIN || filter.maxPrice < SpotFilter.PRICE_MAX) {
            appendCriterion(text, context.getString(R.string.filter_status_price,
                    PriceFormatter.formatMillions(filter.minPrice), PriceFormatter.formatMillions(filter.maxPrice)));
        }
        return text.toString();
    }

    private static void appendCriterion(StringBuilder text, String criterion) {
        if (text.length() > 0) {
            text.append(CRITERIA_SEPARATOR);
        }
        text.append(criterion);
    }

    /**
     * Làm tròn giá về bước gần nhất của thanh giá, trong khoảng cho phép. {@code RangeSlider} ném lỗi
     * nếu nhận giá trị lệch bước, mà bộ lọc khôi phục từ dữ liệu cũ có thể không đúng bước.
     */
    private static float snapToStep(long price) {
        final int steps = Math.round((price - SpotFilter.PRICE_MIN) / PRICE_STEP);
        return SpotFilter.PRICE_MIN + Math.max(0, Math.min(steps, PRICE_STEPS)) * PRICE_STEP;
    }

    private void hideKeyboard(View view) {
        final InputMethodManager keyboard =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /** Chip "Tất cả" ứng với {@code null}, đúng như {@code SpotFilter.zone}. */
    private static ZoneType zoneOf(@IdRes int chipId) {
        if (chipId == R.id.chip_zone_standing) {
            return ZoneType.STANDING;
        }
        if (chipId == R.id.chip_zone_vip) {
            return ZoneType.VIP;
        }
        if (chipId == R.id.chip_zone_standard) {
            return ZoneType.STANDARD;
        }
        return null;
    }

    @StringRes
    private static int zoneNameOf(ZoneType zone) {
        switch (zone) {
            case STANDING:
                return R.string.zone_standing;
            case VIP:
                return R.string.zone_vip;
            default:
                return R.string.zone_standard;
        }
    }

    @IdRes
    private static int chipIdOf(ZoneType zone) {
        if (zone == null) {
            return R.id.chip_zone_all;
        }
        switch (zone) {
            case STANDING:
                return R.id.chip_zone_standing;
            case VIP:
                return R.id.chip_zone_vip;
            default:
                return R.id.chip_zone_standard;
        }
    }
}
