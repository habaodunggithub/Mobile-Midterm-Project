package com.example.midtermproject.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermproject.R;
import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.state.VenueMapState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Danh sách trong bảng chi tiết: nhóm "Đang chọn (N)" (có nút ✕) và nhóm "Vé của bạn (M)" (không có ✕,
 * chỉ hiện khi M &gt; 0). Danh sách <b>không phải nguồn dữ liệu</b>: mỗi lần {@link #refresh()} nó dựng
 * lại các dòng từ {@link VenueMapState}, nên một chỗ không thể xuất hiện hai lần (AC-5.6).
 */
final class SpotListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    /** Việc người dùng làm trên một dòng; Activity quyết định cách xử lý (đi qua state). */
    interface Listener {
        void onSpotClicked(Spot spot);

        void onRemoveClicked(Spot spot);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SPOT = 1;
    private static final int TYPE_EMPTY = 2;

    /** Một dòng đã sẵn sàng hiển thị. {@code key} nhận diện dòng khi so sánh hai danh sách cũ và mới. */
    private static final class Row {
        final int type;
        final String key;
        final String title;
        final Spot spot;
        final boolean removable;
        final boolean outOfFilter;

        Row(int type, String key, String title, Spot spot, boolean removable, boolean outOfFilter) {
            this.type = type;
            this.key = key;
            this.title = title;
            this.spot = spot;
            this.removable = removable;
            this.outOfFilter = outOfFilter;
        }

        /** Cùng key nhưng có thể đổi nội dung (ví dụ số trong tiêu đề, hay chip "ngoài bộ lọc"). */
        boolean sameContent(Row other) {
            return Objects.equals(title, other.title) && removable == other.removable
                    && outOfFilter == other.outOfFilter;
        }
    }

    private final VenueMapState state;
    private final Listener listener;
    private final Map<String, String> sectionNames = new HashMap<>();
    private final List<Row> rows = new ArrayList<>();

    SpotListAdapter(Context context, Venue venue, VenueMapState state, Listener listener) {
        this.state = state;
        this.listener = listener;
        for (Section section : venue.getSections()) {
            sectionNames.put(section.id, section.name);
        }
        refresh(context);
    }

    /**
     * Dựng lại các dòng từ state rồi so với danh sách đang hiện: chỉ dòng nào đổi mới được cập nhật,
     * dòng bị bỏ trượt ra, dòng mới trượt vào. Mỗi lần gọi đều dựng từ đầu nên không thể có dòng trùng.
     */
    void refresh(Context context) {
        final List<Row> next = buildRows(context);
        final List<Row> previous = new ArrayList<>(rows);
        final DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return previous.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return previous.get(oldPosition).key.equals(next.get(newPosition).key);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return previous.get(oldPosition).sameContent(next.get(newPosition));
            }
        });
        rows.clear();
        rows.addAll(next);
        diff.dispatchUpdatesTo(this);
    }

    private List<Row> buildRows(Context context) {
        final List<Row> result = new ArrayList<>();
        final List<Spot> selected = state.getSelectedSpots();
        result.add(new Row(TYPE_HEADER, "header:selected",
                context.getString(R.string.sheet_section_selected, selected.size()), null, false, false));
        if (selected.isEmpty()) {
            result.add(new Row(TYPE_EMPTY, "empty", null, null, false, false));
        }
        for (Spot spot : selected) {
            result.add(new Row(TYPE_SPOT, "spot:" + spot.id, null, spot, true, !state.matchesFilter(spot)));
        }
        final List<Spot> mine = state.getMySpots();
        if (!mine.isEmpty()) {
            result.add(new Row(TYPE_HEADER, "header:mine",
                    context.getString(R.string.sheet_section_mine, mine.size()), null, false, false));
            for (Spot spot : mine) {
                result.add(new Row(TYPE_SPOT, "spot:" + spot.id, null, spot, false, false));
            }
        }
        return result;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderHolder(inflater.inflate(R.layout.item_list_header, parent, false));
            case TYPE_EMPTY:
                return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_list_empty, parent, false)) {
                };
            default:
                return new SpotHolder(inflater.inflate(R.layout.item_spot, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(row);
        } else if (holder instanceof SpotHolder) {
            ((SpotHolder) holder).bind(row);
        }
    }

    private static final class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        HeaderHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_list_header);
            // Trình đọc màn hình đọc đây là tiêu đề của nhóm bên dưới.
            ViewCompat.setAccessibilityHeading(title, true);
        }

        void bind(Row row) {
            title.setText(row.title);
        }
    }

    private final class SpotHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView detail;
        private final TextView outOfFilterChip;
        private final ImageButton removeButton;

        SpotHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_spot_title);
            detail = itemView.findViewById(R.id.tv_spot_detail);
            outOfFilterChip = itemView.findViewById(R.id.tv_spot_filtered);
            removeButton = itemView.findViewById(R.id.btn_remove);
        }

        void bind(Row row) {
            final Spot spot = row.spot;
            final Context context = itemView.getContext();
            title.setText(context.getString(R.string.sheet_spot_title,
                    sectionNames.get(spot.sectionId), spot.label));
            detail.setText(context.getString(R.string.sheet_spot_detail,
                    spot.rowLabel, PriceFormatter.format(spot.price)));
            outOfFilterChip.setVisibility(row.outOfFilter ? View.VISIBLE : View.GONE);
            removeButton.setVisibility(row.removable ? View.VISIBLE : View.GONE);
            removeButton.setContentDescription(context.getString(R.string.sheet_remove_description, spot.label));
            removeButton.setOnClickListener(v -> listener.onRemoveClicked(spot));
            itemView.setOnClickListener(v -> listener.onSpotClicked(spot));
        }
    }
}
