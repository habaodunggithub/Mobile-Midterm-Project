package com.example.midtermproject.map;

import com.example.midtermproject.model.DisplayState;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.state.VenueMapState;

/**
 * Cầu nối giữa lớp vẽ và {@link VenueMapState}: lớp vẽ chỉ biết {@link SpotStateSource}, còn trạng
 * thái thật nằm ở một nơi duy nhất. Mọi hàm chỉ tra cứu, không tạo đối tượng, nên gọi được trong
 * lúc vẽ.
 */
public final class VenueMapStateSource implements SpotStateSource {
    private final VenueMapState state;

    public VenueMapStateSource(VenueMapState state) {
        this.state = state;
    }

    @Override
    public DisplayState displayStateOf(Spot spot) {
        return state.getDisplayState(spot);
    }

    /** Chỗ nằm ngoài bộ lọc thì vẽ mờ, vẫn giữ nguyên trạng thái của nó. */
    @Override
    public boolean isDimmed(Spot spot) {
        return !state.matchesFilter(spot);
    }

    @Override
    public boolean isFocused(Spot spot) {
        return spot.id.equals(state.getFocusedId());
    }
}
