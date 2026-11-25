package com.example.midtermproject.data;

import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotKind;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.model.ZoneType;

import java.util.ArrayList;
import java.util.List;

/**
 * Sinh sơ đồ concert hình móng ngựa bằng dữ liệu giả lập, luôn giống nhau với cùng {@link #SEED}.
 * <p>
 * Tọa độ là "world units", trục y hướng xuống. Sân khấu nằm ở đầu trên; bãi Standing nằm ngay
 * trước sân khấu; các ghế VIP và Standard xếp theo vành cung quanh tâm (giữa mép dưới sân khấu).
 * <p>
 * Thứ tự dựng cũng là thứ tự bốc số ngẫu nhiên cho trạng thái, nên không được đảo thứ tự:
 * Standing A, Standing B, (các phân khu VIP, Standard được thêm ở các bước sau).
 */
public final class MockVenueFactory {
    public static final long SEED = 2026L;

    static final float WORLD_WIDTH = 1080f;
    static final float WORLD_HEIGHT = 630f;

    private static final float STAGE_LEFT = 390f;
    private static final float STAGE_TOP = 30f;
    private static final float STAGE_RIGHT = 690f;
    private static final float STAGE_BOTTOM = 90f;

    // Bãi Standing: hai khối 4 hàng x 6 ô đứng, đặt ngay trước sân khấu.
    private static final float STANDING_HALF = 10f;      // nửa cạnh ô đứng
    private static final float STANDING_STEP = 26f;      // khoảng cách giữa hai tâm kề nhau
    private static final float STANDING_GAP = 40f;       // lối đi: giữa hai khối và giữa sân khấu với khối
    private static final int STANDING_ROWS = 4;
    private static final int STANDING_COLS = 6;
    private static final long STANDING_BASE_PRICE = 1_200_000L;
    private static final long STANDING_PRICE_STEP = 100_000L;

    private MockVenueFactory() {
    }

    public static Venue create() {
        Builder builder = new Builder(new SpotStatusPolicy(SEED));
        addStandingBlocks(builder);
        return builder.build();
    }

    private static void addStandingBlocks(Builder builder) {
        float blockWidth = (STANDING_COLS - 1) * STANDING_STEP + 2 * STANDING_HALF;
        float centerX = (STAGE_LEFT + STAGE_RIGHT) / 2f;
        float leftA = centerX - blockWidth - STANDING_GAP / 2f;
        float leftB = centerX + STANDING_GAP / 2f;
        float top = STAGE_BOTTOM + STANDING_GAP;
        addGridSection(builder, "STA-A", "Standing A", leftA, top, blockWidth);
        addGridSection(builder, "STA-B", "Standing B", leftB, top, blockWidth);
    }

    /** Dựng một khối ô đứng dạng lưới, hàng A nằm sát sân khấu nhất. */
    private static void addGridSection(Builder builder, String sectionId, String name,
                                       float left, float top, float blockWidth) {
        // Tên phân khu nằm ở lối đi giữa sân khấu và khối.
        builder.addSection(new Section(sectionId, name, ZoneType.STANDING,
                left + blockWidth / 2f, STAGE_BOTTOM + STANDING_GAP / 2f));
        for (int row = 0; row < STANDING_ROWS; row++) {
            long price = STANDING_BASE_PRICE - row * STANDING_PRICE_STEP;
            for (int col = 0; col < STANDING_COLS; col++) {
                float cx = left + STANDING_HALF + col * STANDING_STEP;
                float cy = top + STANDING_HALF + row * STANDING_STEP;
                builder.addSpot(sectionId, ZoneType.STANDING, SpotKind.STANDING_CELL,
                        row, col + 1, price, cx, cy, STANDING_HALF);
            }
        }
    }

    /** Gom các phân khu và chỗ trong lúc dựng, rồi tạo {@link Venue} bất biến. */
    private static final class Builder {
        private final SpotStatusPolicy policy;
        private final List<Section> sections = new ArrayList<>();
        private final List<Spot> spots = new ArrayList<>();

        Builder(SpotStatusPolicy policy) {
            this.policy = policy;
        }

        void addSection(Section section) {
            sections.add(section);
        }

        /** Hàng được đánh số từ 0 và đổi thành chữ cái: 0 = "A", 1 = "B"... */
        void addSpot(String sectionId, ZoneType zone, SpotKind kind, int rowIndex, int number,
                     long price, float cx, float cy, float size) {
            String rowLabel = String.valueOf((char) ('A' + rowIndex));
            SpotStatus status = policy.statusFor(sectionId, rowLabel, number);
            spots.add(new Spot(sectionId, zone, kind, rowLabel, number, status, price, cx, cy, size));
        }

        Venue build() {
            return new Venue(WORLD_WIDTH, WORLD_HEIGHT,
                    STAGE_LEFT, STAGE_TOP, STAGE_RIGHT, STAGE_BOTTOM, sections, spots);
        }
    }
}
