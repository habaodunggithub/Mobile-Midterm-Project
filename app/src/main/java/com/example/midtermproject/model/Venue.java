package com.example.midtermproject.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Toàn bộ sơ đồ: kích thước, sân khấu, các phân khu và các chỗ.
 * Bất biến: danh sách được sao chép và không sửa được từ bên ngoài.
 */
public final class Venue {
    public final float worldWidth, worldHeight;
    public final float stageLeft, stageTop, stageRight, stageBottom;
    /** Tâm các vành cung: giữa mép dưới sân khấu. */
    public final float arcCenterX, arcCenterY;

    private final List<Section> sections;
    private final List<Spot> spots;
    private final Map<String, Spot> spotsById;

    public Venue(float worldWidth, float worldHeight,
                 float stageLeft, float stageTop, float stageRight, float stageBottom,
                 List<Section> sections, List<Spot> spots) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.stageLeft = stageLeft;
        this.stageTop = stageTop;
        this.stageRight = stageRight;
        this.stageBottom = stageBottom;
        this.arcCenterX = (stageLeft + stageRight) / 2f;
        this.arcCenterY = stageBottom;
        this.sections = Collections.unmodifiableList(new ArrayList<>(sections));
        this.spots = Collections.unmodifiableList(new ArrayList<>(spots));

        Map<String, Spot> byId = new HashMap<>();
        for (Spot spot : this.spots) {
            if (byId.put(spot.id, spot) != null) {
                throw new IllegalArgumentException("Duplicate spot id: " + spot.id);
            }
        }
        this.spotsById = byId;
    }

    public List<Section> getSections() {
        return sections;
    }

    /** Các chỗ theo thứ tự dựng, cũng là thứ tự vẽ. */
    public List<Spot> getSpots() {
        return spots;
    }

    /** Trả về null nếu không có chỗ nào mang mã này. */
    public Spot findSpot(String id) {
        return id == null ? null : spotsById.get(id);
    }
}
