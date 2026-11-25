# Concert Seat Map

Ứng dụng Android (Java + XML) hiển thị **sơ đồ chỗ ngồi concert hình móng ngựa** gồm 224 chỗ, vẽ trên **một Canvas duy nhất**. Đây là bài giữa kỳ môn Phát triển ứng dụng di động. Dữ liệu là dữ liệu giả lập, không có API hay cơ sở dữ liệu.

| | |
|---|---|
| Repository | https://github.com/habaodunggithub/Mobile-Midterm-Project |
| Video minh họa | _chưa cập nhật_ |
| Nền tảng | Android 7.0 (API 24) trở lên, `compileSdk`/`targetSdk` 37 |
| Ngôn ngữ | Java 11 + XML Views (không dùng Kotlin, Compose hay thư viện bản đồ/zoom bên thứ ba) |

## 1. Mô tả đề tài

Sơ đồ có sân khấu ở một đầu, hai bãi **Standing** (ô đứng vuông) ngay trước sân khấu, rồi vành cung **VIP** và vành cung **Standard** (ghế tròn) bao quanh. Tổng cộng **224 chỗ** chia thành 9 phân khu:

| Khu | Phân khu | Số chỗ | Hình |
|---|---|---|---|
| Standing | Standing A, Standing B | 48 (2 × 24 ô) | ô đứng vuông |
| VIP | VIP Trái, VIP Giữa, VIP Phải | 72 (3 × 24 ghế) | ghế tròn |
| Standard | Standard 1 đến 4 | 104 (4 × 26 ghế) | ghế tròn |

Mỗi chỗ có một trong năm trạng thái hiển thị: **Còn trống**, **Đang chọn**, **Đã đặt**, **Bị khóa** và **Vé của bạn**. Khoảng 1/3 số chỗ đã có người đặt khi mở app lần đầu, 8 ghế bị khóa ở các cụm cố định.

### Chức năng

| Nhóm | Chức năng |
|---|---|
| Xem sơ đồ | Sơ đồ móng ngựa có phân khu, lối đi và nhãn; bảng chú thích (nút ⓘ) giải thích màu và ký hiệu |
| Điều hướng | Kéo một ngón để **Pan** (có giới hạn biên và quán tính); **Pinch-to-Zoom** quanh tâm hai ngón với mức nhỏ nhất và lớn nhất; xoay sơ đồ bằng hai ngón (tự chỉnh thẳng khi nhấc tay, nút la bàn để về góc mặc định); **Reset View** |
| Chọn chỗ | Chạm để chọn hoặc bỏ chọn, tối đa 8 chỗ; chạm vào chỗ không chọn được thì hiện lý do bằng Snackbar |
| Lọc và tìm kiếm | Theo khu, theo mã chỗ (khớp một phần, không phân biệt hoa thường), "Chỉ chỗ trống", khoảng giá; các tiêu chí kết hợp bằng AND; chỗ ngoài bộ lọc bị làm mờ nhưng lựa chọn được giữ nguyên |
| Bottom Sheet | Thu gọn hiện chỗ đang xem và tóm tắt lựa chọn; mở rộng hiện danh sách "Đang chọn" và "Vé của bạn"; bấm dòng để định vị chỗ trên sơ đồ, bấm ✕ để bỏ chọn; đồng bộ hai chiều với sơ đồ |
| Đặt vé | Nút "Đặt N vé", hộp thoại xác nhận, chỗ đã đặt thành "Vé của bạn" |
| Lưu dữ liệu | Lựa chọn và vé của bạn được lưu bằng `SharedPreferences` và còn lại sau khi tắt hẳn app; nút ⋮ → "Làm mới dữ liệu" để xóa |
| Xoay màn hình | Giữ nguyên chỗ đang xem, bộ lọc, góc nhìn, bảng chú thích, hộp thoại; bố cục riêng cho máy ngang (panel lọc bên trái, bảng chi tiết 420dp) |

## 2. Hướng dẫn chạy

**Yêu cầu:** Android Studio bản mới (kèm JDK 17 và Android SDK 37), hoặc JDK 17 và Android SDK cài sẵn nếu chạy bằng dòng lệnh. Mã nguồn được biên dịch ở mức Java 11.

**Bằng Android Studio:** mở thư mục dự án (thư mục chứa `settings.gradle.kts`), đợi Gradle đồng bộ, chọn một thiết bị ảo hoặc máy thật (Android 7.0 trở lên), bấm **Run**.

**Bằng dòng lệnh** (trong thư mục dự án; cần `local.properties` trỏ tới Android SDK, Android Studio tự tạo file này):

```bash
./gradlew assembleDebug    # build, ra app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug     # cài lên thiết bị hoặc emulator đang kết nối
./gradlew test             # chạy unit test JUnit trên JVM
./gradlew lint             # kiểm tra lint
```

**Cách thao tác:**

- Kéo một ngón: di chuyển sơ đồ. Vuốt nhanh rồi thả: sơ đồ trượt tiếp.
- Hai ngón chụm hoặc nở: zoom. Xoay hai ngón: xoay sơ đồ. Nút ⟲: Reset View. Nút la bàn (hiện khi sơ đồ lệch góc): về góc mặc định.
- Chạm vào chỗ: chọn hoặc bỏ chọn. Kéo bảng chi tiết ở đáy lên hoặc bấm phần đầu để mở rộng.
- Nút ⓘ: bảng chú thích. Nút ⋮: "Làm mới dữ liệu".
