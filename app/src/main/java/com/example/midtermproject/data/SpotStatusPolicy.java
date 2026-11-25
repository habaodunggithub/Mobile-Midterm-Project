package com.example.midtermproject.data;

import com.example.midtermproject.model.SpotStatus;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Quyết định trạng thái gốc của từng chỗ trong dữ liệu giả lập.
 * <ul>
 *   <li>LOCKED: 8 ghế ở các cụm cố định có lý do (bàn mixer, ghế bị cột che tầm nhìn).
 *       Không dùng số ngẫu nhiên.</li>
 *   <li>BOOKED: khoảng 1/3 các chỗ còn lại, bốc bằng {@link Random} có seed cố định.</li>
 *   <li>AVAILABLE: các chỗ còn lại.</li>
 * </ul>
 * Kết quả chỉ phụ thuộc vào seed và thứ tự gọi {@link #statusFor}, nên mỗi lần mở app đều giống nhau.
 */
final class SpotStatusPolicy {
    /** Tỉ lệ chỗ đã có người đặt (khoảng 1/3). */
    static final double BOOKED_RATE = 0.34;

    private static final Set<String> LOCKED_KEYS = buildLockedKeys();

    private final Random random;

    SpotStatusPolicy(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Gọi theo đúng thứ tự dựng chỗ. Chỗ LOCKED không tiêu thụ số ngẫu nhiên,
     * nên việc khóa hay không không làm lệch trạng thái của các chỗ khác.
     */
    SpotStatus statusFor(String sectionId, String rowLabel, int number) {
        if (LOCKED_KEYS.contains(key(sectionId, rowLabel, number))) {
            return SpotStatus.LOCKED;
        }
        return random.nextDouble() < BOOKED_RATE ? SpotStatus.BOOKED : SpotStatus.AVAILABLE;
    }

    private static Set<String> buildLockedKeys() {
        Set<String> keys = new HashSet<>();
        addLockedRange(keys, "VIP-G", "C", 3, 6);   // bàn mixer âm thanh, giữa hàng cuối VIP Giữa
        addLockedRange(keys, "STD-1", "D", 1, 2);   // cột che tầm nhìn, đầu hàng cuối Standard 1
        addLockedRange(keys, "STD-4", "D", 6, 7);   // cột che tầm nhìn, cuối hàng cuối Standard 4
        return Collections.unmodifiableSet(keys);
    }

    private static void addLockedRange(Set<String> keys, String sectionId, String rowLabel,
                                       int firstNumber, int lastNumber) {
        for (int number = firstNumber; number <= lastNumber; number++) {
            keys.add(key(sectionId, rowLabel, number));
        }
    }

    private static String key(String sectionId, String rowLabel, int number) {
        return sectionId + "-" + rowLabel + number;
    }
}
