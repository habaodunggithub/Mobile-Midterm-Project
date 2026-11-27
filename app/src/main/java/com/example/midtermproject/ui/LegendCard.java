package com.example.midtermproject.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.example.midtermproject.R;

/**
 * Bảng chú thích (legend): thẻ nổi ở đáy vùng sơ đồ, mở và đóng bằng nút ⓘ (AC-1.4). Mỗi mục là một
 * mẫu vẽ giống hệt cách {@code SpotRenderer} vẽ chỗ, kèm chữ; mẫu và renderer cùng lấy màu từ
 * {@code colors.xml} nên đổi màu ở một nơi thì cả hai cùng đổi. Mặc định đóng.
 */
final class LegendCard {
    /** Mọi màu và ký hiệu đang được vẽ trên sơ đồ, theo thứ tự đọc: khu, trạng thái và chỗ đang xem, rồi hình dạng. */
    private static final Entry[] ENTRIES = {
            new Entry(R.drawable.legend_standing, R.string.zone_standing),
            new Entry(R.drawable.legend_vip, R.string.zone_vip),
            new Entry(R.drawable.legend_standard, R.string.zone_standard),
            new Entry(R.drawable.legend_selected, R.string.state_selected),
            new Entry(R.drawable.legend_mine, R.string.state_mine),
            new Entry(R.drawable.legend_booked, R.string.state_booked),
            new Entry(R.drawable.legend_locked, R.string.state_locked),
            new Entry(R.drawable.legend_out_of_filter, R.string.state_out_of_filter),
            new Entry(R.drawable.legend_focus, R.string.legend_focus),
            new Entry(R.drawable.legend_seat, R.string.legend_seat),
            new Entry(R.drawable.legend_standing_cell, R.string.legend_standing_cell),
    };

    private static final class Entry {
        @DrawableRes final int swatch;
        @StringRes final int label;

        Entry(@DrawableRes int swatch, @StringRes int label) {
            this.swatch = swatch;
            this.label = label;
        }
    }

    private final Context context;
    private final GridLayout card;
    private final ImageButton toggleButton;
    /** Khoảng cách tới đáy vùng sơ đồ ngoài phần bảng chi tiết che, để thẻ không chìm dưới bảng. */
    private final int edgeMarginPx;
    /** Thẻ đang mở hay đóng (trạng thái cuối, kể cả khi hiệu ứng còn đang chạy). */
    private boolean shown;
    private boolean animated = true;

    LegendCard(GridLayout card, ImageButton toggleButton) {
        this.context = card.getContext();
        this.card = card;
        this.toggleButton = toggleButton;
        this.edgeMarginPx = ((ViewGroup.MarginLayoutParams) card.getLayoutParams()).bottomMargin;

        final LayoutInflater inflater = LayoutInflater.from(context);
        for (Entry entry : ENTRIES) {
            // attachToRoot=false nhưng truyền cha để các thuộc tính layout_* được hiểu là của GridLayout.
            final View item = inflater.inflate(R.layout.item_legend, card, false);
            ((ImageView) item.findViewById(R.id.legend_swatch)).setImageResource(entry.swatch);
            ((TextView) item.findViewById(R.id.legend_label)).setText(entry.label);
            card.addView(item);
        }
        toggleButton.setOnClickListener(v -> setShown(!isShown()));
        setShown(false);
    }

    /** Lấy từ biến {@link #shown} chứ không từ View, vì lúc đang bung ra hay thu lại View chưa ở trạng thái cuối. */
    boolean isShown() {
        return shown;
    }

    /** Bật hoặc tắt hiệu ứng bung ra (theo cài đặt hiệu ứng của hệ thống, xem MainActivity). */
    void setAnimated(boolean animated) {
        this.animated = animated;
    }

    void setShown(boolean shown) {
        this.shown = shown;
        // Thẻ nằm ở đáy sơ đồ, nút ⓘ ở góc trên phải: bung ra từ góc trên phải của thẻ, về phía nút (Q49).
        if (shown) {
            Reveal.show(card, 1f, 0f, animated);
        } else {
            Reveal.hide(card, animated);
        }
        // Trình đọc màn hình nghe việc nút sắp làm, không phải trạng thái cũ.
        toggleButton.setContentDescription(context.getString(
                shown ? R.string.legend_hide_description : R.string.legend_show_description));
    }

    /** Đặt đáy thẻ ngay trên bảng chi tiết đang thu gọn (cao {@code peekHeightPx}). */
    void setBottomInset(int peekHeightPx) {
        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
        params.bottomMargin = peekHeightPx + edgeMarginPx;
        card.setLayoutParams(params);
    }
}
