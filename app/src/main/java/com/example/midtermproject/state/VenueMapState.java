package com.example.midtermproject.state;

import com.example.midtermproject.model.DisplayState;
import com.example.midtermproject.model.SelectionResult;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.Venue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Nguồn dữ liệu duy nhất cho phần người dùng thay đổi được: các chỗ đang chọn (theo thứ tự chọn), vé
 * của bạn, chỗ đang xem và bộ lọc. Sơ đồ, bộ lọc và bảng chi tiết chỉ đọc từ đây; mọi thay đổi đều
 * đi qua các lệnh bên dưới, và mỗi lệnh báo {@link Listener} đúng một lần.
 * <p>
 * Dữ liệu gốc của chỗ ({@link Spot#baseStatus}) không bao giờ đổi. "Đang chọn" chỉ là việc id nằm
 * trong tập đã chọn, nên bỏ chọn thì chỗ tự trở về đúng dáng vẻ cũ (REQUIREMENTS 6.3).
 * <p>
 * Bất biến (REQUIREMENTS 6.2): đã chọn và vé của bạn không giao nhau; mọi chỗ đã chọn có trạng thái
 * gốc AVAILABLE; số chỗ đã chọn không quá {@link #MAX_SELECTION}; chỗ đang xem là null hoặc có thật.
 * <p>
 * Java thuần. Việc tra cứu dùng các tập băm tính sẵn, không tạo đối tượng, nên gọi được trong
 * lúc vẽ.
 */
public final class VenueMapState {
    /** Số chỗ tối đa mỗi lần đặt (REQUIREMENTS Q8). */
    public static final int MAX_SELECTION = 8;

    /** Được gọi sau mỗi lệnh làm đổi trạng thái. */
    public interface Listener {
        void onVenueMapStateChanged();
    }

    private final Venue venue;
    private final LinkedHashSet<String> selectedIds = new LinkedHashSet<>();
    private final LinkedHashSet<String> myTicketIds = new LinkedHashSet<>();
    private String focusedId;
    private SpotFilter filter = SpotFilter.NONE;
    /** Các chỗ khớp bộ lọc hiện tại; tính lại mỗi khi bộ lọc hoặc vé của bạn đổi. */
    private final Set<String> matchingIds = new HashSet<>();
    private final List<Listener> listeners = new ArrayList<>();

    public VenueMapState(Venue venue) {
        this.venue = venue;
        recomputeMatches();
    }

    // ---- Lệnh: mọi thay đổi đi qua đây ----

    /**
     * Xử lý một lần chạm vào chỗ: bật hoặc tắt lựa chọn, hoặc từ chối kèm lý do. Dù thành công hay
     * bị từ chối, chỗ vừa chạm đều trở thành chỗ đang xem (AC-3.1 đến AC-3.7).
     *
     * @throws IllegalArgumentException nếu {@code spotId} không phải mã chỗ có thật
     */
    public SelectionResult toggleSelection(String spotId) {
        final Spot spot = requireSpot(spotId);
        focusedId = spot.id;
        final SelectionResult result = decide(spot);
        notifyListeners();
        return result;
    }

    private SelectionResult decide(Spot spot) {
        if (myTicketIds.contains(spot.id)) {
            return SelectionResult.REJECTED_MINE;
        }
        if (spot.baseStatus == SpotStatus.BOOKED) {
            return SelectionResult.REJECTED_BOOKED;
        }
        if (spot.baseStatus == SpotStatus.LOCKED) {
            return SelectionResult.REJECTED_LOCKED;
        }
        // Chỗ ngoài bộ lọc không bật tắt được bằng cách chạm, kể cả khi đang được chọn (ADR-05);
        // muốn bỏ thì dùng nút xóa trong bảng chi tiết.
        if (!matchingIds.contains(spot.id)) {
            return SelectionResult.REJECTED_FILTERED;
        }
        if (selectedIds.remove(spot.id)) {
            return SelectionResult.DESELECTED;
        }
        if (selectedIds.size() >= MAX_SELECTION) {
            return SelectionResult.REJECTED_LIMIT;
        }
        selectedIds.add(spot.id);
        return SelectionResult.SELECTED;
    }

    /** Bỏ một chỗ khỏi lựa chọn (nút xóa trong bảng chi tiết). Không có trong lựa chọn thì không làm gì. */
    public void removeSelection(String spotId) {
        if (selectedIds.remove(spotId)) {
            notifyListeners();
        }
    }

    /** Đặt chỗ đang xem; {@code null} là không có chỗ nào. */
    public void setFocused(String spotIdOrNull) {
        if (spotIdOrNull != null) {
            requireSpot(spotIdOrNull);
        }
        if (spotIdOrNull == null ? focusedId == null : spotIdOrNull.equals(focusedId)) {
            return;
        }
        focusedId = spotIdOrNull;
        notifyListeners();
    }

    /** Đổi bộ lọc. Lựa chọn và vé của bạn không bị ảnh hưởng (ADR-05, AC-4.9). */
    public void setFilter(SpotFilter newFilter) {
        final SpotFilter next = newFilter == null ? SpotFilter.NONE : newFilter;
        final boolean unchanged = filter.equals(next);
        filter = next;      // luôn lưu bản mới để giữ đúng chuỗi người dùng đã gõ
        if (!unchanged) {
            recomputeMatches();
            notifyListeners();
        }
    }

    /**
     * Đặt vé: mọi chỗ đang chọn (kể cả chỗ ngoài bộ lọc) thành vé của bạn, lựa chọn về rỗng (AC-6.2).
     * Chỗ đang xem mà vừa được đặt thì bỏ khỏi chỗ đang xem (UC-10). Chọn rỗng thì không làm gì.
     *
     * @return số vé vừa đặt
     */
    public int bookSelection() {
        final int booked = selectedIds.size();
        if (booked == 0) {
            return 0;
        }
        if (selectedIds.contains(focusedId)) {
            focusedId = null;
        }
        myTicketIds.addAll(selectedIds);
        selectedIds.clear();
        // Chỗ thành vé của bạn thì tiêu chí "chỉ chỗ trống" không còn khớp nữa, nên phải tính lại.
        recomputeMatches();
        notifyListeners();
        return booked;
    }

    /**
     * Khôi phục toàn bộ trạng thái từ dữ liệu đã lưu (REQUIREMENTS UC-11). Dữ liệu đọc lên có thể
     * cũ hoặc hỏng, nên mọi thứ đều được kiểm lại để các bất biến luôn đúng (AC-7.6):
     * <ul>
     *   <li>mã không tồn tại hoặc trùng thì bỏ;</li>
     *   <li>chỗ không còn trạng thái gốc AVAILABLE thì bỏ (vé và lựa chọn chỉ có ở chỗ từng trống);</li>
     *   <li>chỗ vừa chọn vừa là vé thì vé thắng, chỗ đó không còn trong lựa chọn;</li>
     *   <li>lựa chọn dài quá {@link #MAX_SELECTION} thì giữ những chỗ đứng đầu.</li>
     * </ul>
     * Báo người nghe đúng một lần.
     */
    public void restore(List<String> selected, List<String> mine, String focused, SpotFilter newFilter) {
        selectedIds.clear();
        myTicketIds.clear();
        if (mine != null) {
            for (String id : mine) {
                if (isAvailableSpot(id)) {
                    myTicketIds.add(id);
                }
            }
        }
        if (selected != null) {
            for (String id : selected) {
                if (selectedIds.size() >= MAX_SELECTION) {
                    break;
                }
                if (isAvailableSpot(id) && !myTicketIds.contains(id)) {
                    selectedIds.add(id);
                }
            }
        }
        focusedId = (focused != null && venue.findSpot(focused) != null) ? focused : null;
        filter = newFilter == null ? SpotFilter.NONE : newFilter;
        recomputeMatches();
        notifyListeners();
    }

    /**
     * Làm mới dữ liệu: xóa lựa chọn, vé của bạn và chỗ đang xem. Bộ lọc giữ nguyên (AC-8.2).
     * Báo người nghe đúng một lần.
     */
    public void resetAll() {
        selectedIds.clear();
        myTicketIds.clear();
        focusedId = null;
        // Hết vé của bạn thì các chỗ đó lại khớp tiêu chí "chỉ chỗ trống", nên phải tính lại.
        recomputeMatches();
        notifyListeners();
    }

    private boolean isAvailableSpot(String spotId) {
        final Spot spot = venue.findSpot(spotId);
        return spot != null && spot.baseStatus == SpotStatus.AVAILABLE;
    }

    // ---- Truy vấn ----

    /** Trạng thái dùng để vẽ, theo thứ tự ưu tiên ở REQUIREMENTS 6.3. */
    public DisplayState getDisplayState(Spot spot) {
        if (myTicketIds.contains(spot.id)) {
            return DisplayState.MINE;
        }
        if (spot.baseStatus == SpotStatus.BOOKED) {
            return DisplayState.BOOKED;
        }
        if (spot.baseStatus == SpotStatus.LOCKED) {
            return DisplayState.LOCKED;
        }
        if (selectedIds.contains(spot.id)) {
            return DisplayState.SELECTED;
        }
        return DisplayState.AVAILABLE;
    }

    public boolean isSelected(String spotId) {
        return selectedIds.contains(spotId);
    }

    public boolean isMine(String spotId) {
        return myTicketIds.contains(spotId);
    }

    public boolean matchesFilter(Spot spot) {
        return matchingIds.contains(spot.id);
    }

    public int getMatchCount() {
        return matchingIds.size();
    }

    public SpotFilter getFilter() {
        return filter;
    }

    public String getFocusedId() {
        return focusedId;
    }

    public Spot getFocusedSpot() {
        return focusedId == null ? null : venue.findSpot(focusedId);
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    /** Các chỗ đang chọn theo đúng thứ tự đã chọn. */
    public List<Spot> getSelectedSpots() {
        return spotsOf(selectedIds);
    }

    public long getSelectedTotal() {
        long total = 0L;
        for (String id : selectedIds) {
            total += venue.findSpot(id).price;
        }
        return total;
    }

    public List<Spot> getMySpots() {
        return spotsOf(myTicketIds);
    }

    public ArrayList<String> snapshotSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public ArrayList<String> snapshotMyTicketIds() {
        return new ArrayList<>(myTicketIds);
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    // ---- Nội bộ ----

    private Spot requireSpot(String spotId) {
        final Spot spot = venue.findSpot(spotId);
        if (spot == null) {
            throw new IllegalArgumentException("Unknown spot id: " + spotId);
        }
        return spot;
    }

    private List<Spot> spotsOf(Set<String> ids) {
        final List<Spot> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            result.add(venue.findSpot(id));
        }
        return result;
    }

    private void recomputeMatches() {
        matchingIds.clear();
        final List<Spot> spots = venue.getSpots();
        for (int i = 0; i < spots.size(); i++) {
            final Spot spot = spots.get(i);
            if (filter.matches(spot, myTicketIds.contains(spot.id))) {
                matchingIds.add(spot.id);
            }
        }
    }

    private void notifyListeners() {
        // Sao chép để một người nghe có thể tự gỡ mình ngay trong lúc được gọi.
        for (Listener listener : new ArrayList<>(listeners)) {
            listener.onVenueMapStateChanged();
        }
    }
}
