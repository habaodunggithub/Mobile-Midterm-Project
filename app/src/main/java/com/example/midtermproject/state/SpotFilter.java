package com.example.midtermproject.state;

import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.ZoneType;

import java.util.Locale;

/**
 * Bộ lọc chỗ theo bốn tiêu chí (REQUIREMENTS F4): khu, mã chỗ, chỉ chỗ còn trống, khoảng giá. Một
 * chỗ chỉ "khớp" khi thỏa TẤT CẢ tiêu chí đang bật.
 * <p>
 * Bất biến: mỗi lần đổi tạo ra một bản mới ({@code withX}). Java thuần.
 */
public final class SpotFilter {
    /** Giá thấp nhất và cao nhất trong dữ liệu giả lập, cũng là hai đầu của thanh lọc giá. */
    public static final long PRICE_MIN = 400_000L;
    public static final long PRICE_MAX = 2_500_000L;

    /** Không lọc gì: Tất cả khu, ô tìm kiếm rỗng, không giới hạn trạng thái, đủ khoảng giá. */
    public static final SpotFilter NONE = new SpotFilter(null, "", false, PRICE_MIN, PRICE_MAX);

    /** Khu được chọn, {@code null} là Tất cả. */
    public final ZoneType zone;
    /** Chuỗi người dùng gõ, giữ nguyên để hiển thị lại trong ô tìm kiếm. */
    public final String rawQuery;
    /** {@link #rawQuery} đã chuẩn hóa: viết hoa, bỏ khoảng trắng và dấu gạch. */
    public final String normalizedQuery;
    public final boolean availableOnly;
    public final long minPrice;
    public final long maxPrice;

    private SpotFilter(ZoneType zone, String rawQuery, boolean availableOnly, long minPrice, long maxPrice) {
        this.zone = zone;
        this.rawQuery = rawQuery;
        this.normalizedQuery = normalize(rawQuery);
        this.availableOnly = availableOnly;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public SpotFilter withZone(ZoneType newZone) {
        return new SpotFilter(newZone, rawQuery, availableOnly, minPrice, maxPrice);
    }

    public SpotFilter withQuery(String newQuery) {
        return new SpotFilter(zone, newQuery == null ? "" : newQuery, availableOnly, minPrice, maxPrice);
    }

    public SpotFilter withAvailableOnly(boolean newAvailableOnly) {
        return new SpotFilter(zone, rawQuery, newAvailableOnly, minPrice, maxPrice);
    }

    public SpotFilter withPriceRange(long newMin, long newMax) {
        return new SpotFilter(zone, rawQuery, availableOnly, Math.min(newMin, newMax), Math.max(newMin, newMax));
    }

    /** Có ít nhất một tiêu chí khác mặc định thì hiện dòng điều kiện và nút Xóa lọc (AC-4.6). */
    public boolean isActive() {
        return zone != null
                || !normalizedQuery.isEmpty()
                || availableOnly
                || minPrice > PRICE_MIN
                || maxPrice < PRICE_MAX;
    }

    /**
     * Chỗ có khớp bộ lọc không.
     *
     * @param isMine chỗ này là vé của người dùng; cần cho tiêu chí "chỉ chỗ trống", vì vé của bạn
     *               không còn trống dù trạng thái gốc vẫn là AVAILABLE. Chỗ đang chọn vẫn tính là trống.
     */
    public boolean matches(Spot spot, boolean isMine) {
        if (zone != null && spot.zone != zone) {
            return false;
        }
        if (!normalizedQuery.isEmpty() && !spot.searchKey.contains(normalizedQuery)) {
            return false;
        }
        if (availableOnly && (spot.baseStatus != SpotStatus.AVAILABLE || isMine)) {
            return false;
        }
        return spot.price >= minPrice && spot.price <= maxPrice;
    }

    /** Viết hoa, bỏ mọi khoảng trắng và dấu gạch, nên "vip g b05", "VIP-G-B05" và "gb05" đều khớp (Q18). */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        final StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            final char c = raw.charAt(i);
            if (!Character.isWhitespace(c) && c != '-') {
                builder.append(c);
            }
        }
        return builder.toString().toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SpotFilter)) {
            return false;
        }
        final SpotFilter that = (SpotFilter) other;
        return zone == that.zone
                && normalizedQuery.equals(that.normalizedQuery)
                && availableOnly == that.availableOnly
                && minPrice == that.minPrice
                && maxPrice == that.maxPrice;
    }

    @Override
    public int hashCode() {
        int result = zone == null ? 0 : zone.hashCode();
        result = 31 * result + normalizedQuery.hashCode();
        result = 31 * result + (availableOnly ? 1 : 0);
        result = 31 * result + Long.hashCode(minPrice);
        result = 31 * result + Long.hashCode(maxPrice);
        return result;
    }
}
