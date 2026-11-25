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
 * Standing A, Standing B, VIP Trái, VIP Giữa, VIP Phải, Standard 1 đến 4 (từ trái sang phải).
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

    // Ghế trên các vành cung: hình tròn, bán kính chung.
    private static final float SEAT_RADIUS = 10f;

    // VIP: 3 phân khu x 3 hàng x 8 ghế, vành cung gần sân khấu.
    private static final float[] VIP_ROW_RADII = {260f, 286f, 312f};
    private static final int[] VIP_SEATS_PER_ROW = {8, 8, 8};
    private static final long VIP_BASE_PRICE = 2_500_000L;
    private static final long VIP_PRICE_STEP = 300_000L;
    private static final float VIP_LABEL_RADIUS = 344f;     // lối đi giữa vành VIP và vành Standard

    // Standard: 4 phân khu x 4 hàng (6, 6, 7, 7 ghế), vành cung xa sân khấu.
    private static final float[] STANDARD_ROW_RADII = {376f, 402f, 428f, 454f};
    private static final int[] STANDARD_SEATS_PER_ROW = {6, 6, 7, 7};
    private static final long STANDARD_BASE_PRICE = 700_000L;
    private static final long STANDARD_PRICE_STEP = 100_000L;
    private static final float STANDARD_LABEL_RADIUS = 490f;   // ngoài hàng D, nằm dưới sơ đồ

    private MockVenueFactory() {
    }

    public static Venue create() {
        Builder builder = new Builder(new SpotStatusPolicy(SEED));
        addStandingBlocks(builder);
        addVipSections(builder);
        addStandardSections(builder);
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

    private static void addVipSections(Builder builder) {
        // Góc theta đo từ trục +x theo chiều kim đồng hồ trên màn hình (90 độ = thẳng xuống).
        // Dựng từ trái sang phải trên màn hình: Trái (góc lớn), Giữa, Phải (góc nhỏ).
        addArcSection(builder, "VIP-T", "VIP Trái", ZoneType.VIP, VIP_ROW_RADII, VIP_SEATS_PER_ROW,
                125f, 175f, VIP_BASE_PRICE, VIP_PRICE_STEP, VIP_LABEL_RADIUS);
        addArcSection(builder, "VIP-G", "VIP Giữa", ZoneType.VIP, VIP_ROW_RADII, VIP_SEATS_PER_ROW,
                65f, 115f, VIP_BASE_PRICE, VIP_PRICE_STEP, VIP_LABEL_RADIUS);
        addArcSection(builder, "VIP-P", "VIP Phải", ZoneType.VIP, VIP_ROW_RADII, VIP_SEATS_PER_ROW,
                5f, 55f, VIP_BASE_PRICE, VIP_PRICE_STEP, VIP_LABEL_RADIUS);
    }

    private static void addStandardSections(Builder builder) {
        // Dựng từ trái sang phải trên màn hình: Standard 1 (góc lớn nhất) đến Standard 4.
        addArcSection(builder, "STD-1", "Standard 1", ZoneType.STANDARD,
                STANDARD_ROW_RADII, STANDARD_SEATS_PER_ROW,
                139f, 175f, STANDARD_BASE_PRICE, STANDARD_PRICE_STEP, STANDARD_LABEL_RADIUS);
        addArcSection(builder, "STD-2", "Standard 2", ZoneType.STANDARD,
                STANDARD_ROW_RADII, STANDARD_SEATS_PER_ROW,
                95f, 131f, STANDARD_BASE_PRICE, STANDARD_PRICE_STEP, STANDARD_LABEL_RADIUS);
        addArcSection(builder, "STD-3", "Standard 3", ZoneType.STANDARD,
                STANDARD_ROW_RADII, STANDARD_SEATS_PER_ROW,
                49f, 85f, STANDARD_BASE_PRICE, STANDARD_PRICE_STEP, STANDARD_LABEL_RADIUS);
        addArcSection(builder, "STD-4", "Standard 4", ZoneType.STANDARD,
                STANDARD_ROW_RADII, STANDARD_SEATS_PER_ROW,
                5f, 41f, STANDARD_BASE_PRICE, STANDARD_PRICE_STEP, STANDARD_LABEL_RADIUS);
    }

    /**
     * Dựng một phân khu ghế trên vành cung quanh tâm (arcCenterX, arcCenterY).
     * Hàng A nằm gần sân khấu nhất. Trong mỗi hàng, ghế số 1 nằm bên trái trên màn hình:
     * <pre>
     *   theta(k) = endDeg - (k - 1) * (endDeg - startDeg) / (n - 1)
     *   cx = arcCenterX + r * cos(theta)      cy = arcCenterY + r * sin(theta)
     * </pre>
     */
    private static void addArcSection(Builder builder, String sectionId, String name, ZoneType zone,
                                      float[] rowRadii, int[] seatsPerRow,
                                      float startDeg, float endDeg,
                                      long basePrice, long priceStep, float labelRadius) {
        float centerX = (STAGE_LEFT + STAGE_RIGHT) / 2f;
        float centerY = STAGE_BOTTOM;

        double midRad = Math.toRadians((startDeg + endDeg) / 2.0);
        builder.addSection(new Section(sectionId, name, zone,
                (float) (centerX + labelRadius * Math.cos(midRad)),
                (float) (centerY + labelRadius * Math.sin(midRad))));

        for (int row = 0; row < rowRadii.length; row++) {
            int seats = seatsPerRow[row];
            long price = basePrice - row * priceStep;
            for (int k = 1; k <= seats; k++) {
                double step = seats > 1 ? (endDeg - startDeg) / (seats - 1) : 0.0;
                double theta = Math.toRadians(endDeg - (k - 1) * step);
                float cx = (float) (centerX + rowRadii[row] * Math.cos(theta));
                float cy = (float) (centerY + rowRadii[row] * Math.sin(theta));
                builder.addSpot(sectionId, zone, SpotKind.SEAT, row, k, price, cx, cy, SEAT_RADIUS);
            }
        }
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
