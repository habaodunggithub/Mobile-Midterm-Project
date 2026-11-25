package com.example.midtermproject.map;

import com.example.midtermproject.model.Spot;

import java.util.List;

/**
 * Tìm chỗ nằm dưới điểm chạm. Chỉ làm phần hình học: xét mọi chỗ, không biết gì về bộ lọc, trạng
 * thái hay lựa chọn. Những quy tắc "có được chọn hay không" thuộc về {@code VenueMapState}, nên
 * chạm vào chỗ ngoài bộ lọc vẫn nhận ra chỗ đó để báo lý do.
 * <p>
 * Java thuần, không cấp phát: gọi được trong lúc xử lý chạm mà không gây rác.
 */
public final class HitTester {
    private HitTester() {
    }

    /**
     * @param wx           điểm chạm, đã đổi sang tọa độ sơ đồ bằng {@link MapCamera#screenToWorldX}
     * @param tolerance    dung sai quanh mép chỗ, tính bằng đơn vị sơ đồ. Phải là {@code dp / scale}:
     *                     nếu quên chia cho tỉ lệ thì khi zoom lớn vùng nới rộng gấp nhiều lần và chạm
     *                     lệch vẫn "trúng"
     * @return chỗ trúng; nếu điểm chạm nằm trong vùng của nhiều chỗ (ở mức fit, vùng đã nới của các
     * chỗ kề nhau chồng lên nhau) thì chọn chỗ có tâm gần điểm chạm nhất; {@code null} nếu không trúng
     * chỗ nào, ví dụ chạm vào lối đi
     */
    public static Spot findSpotAt(List<Spot> spots, float wx, float wy, float tolerance) {
        Spot best = null;
        float bestDistanceSq = Float.MAX_VALUE;
        for (int i = 0; i < spots.size(); i++) {
            final Spot spot = spots.get(i);
            if (!spot.hitTest(wx, wy, tolerance)) {
                continue;
            }
            final float distanceSq = spot.distanceSqTo(wx, wy);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = spot;
            }
        }
        return best;
    }
}
