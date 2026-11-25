package com.example.midtermproject.map;

import com.example.midtermproject.model.DisplayState;
import com.example.midtermproject.model.Spot;

/**
 * Nguồn thông tin để vẽ: trạng thái hiển thị của từng chỗ, chỗ có bị làm mờ (ngoài bộ lọc) và chỗ
 * đang xem không. Lớp vẽ chỉ đọc qua interface này, không biết dữ liệu đến từ đâu.
 */
public interface SpotStateSource {
    DisplayState displayStateOf(Spot spot);

    boolean isDimmed(Spot spot);

    boolean isFocused(Spot spot);
}
