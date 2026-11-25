package com.example.midtermproject.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.midtermproject.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

/**
 * Hộp thoại xác nhận đặt vé (AC-6.1): số vé, danh sách mã chỗ và tổng tiền, kèm nút "Hủy" và "Xác nhận".
 * <p>
 * Dữ liệu đi vào qua {@code arguments} chứ không đọc từ Activity: khi xoay màn hình, Activity và hộp
 * thoại đều bị tạo lại, {@code FragmentManager} dựng lại hộp thoại từ {@code arguments} nên nội dung
 * không đổi (AC-6.4) mà không cần Activity nhớ gì thêm.
 * <p>
 * Hộp thoại không tự đặt vé. Bấm "Xác nhận" chỉ báo cho Activity qua {@link Callback}, Activity gọi
 * {@code VenueMapState.bookSelection()}; bấm "Hủy", Back hay chạm ra ngoài chỉ đóng hộp thoại (AC-6.3).
 */
public class BookingConfirmDialog extends DialogFragment {
    public static final String TAG = "booking_confirm";
    private static final String ARG_IDS = "ids";
    private static final String ARG_TOTAL = "total";

    /** Activity chứa hộp thoại phải cài đặt interface này. */
    public interface Callback {
        void onBookingConfirmed();
    }

    public static BookingConfirmDialog newInstance(ArrayList<String> ids, long total) {
        final Bundle args = new Bundle();
        args.putStringArrayList(ARG_IDS, ids);
        args.putLong(ARG_TOTAL, total);
        final BookingConfirmDialog dialog = new BookingConfirmDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = requireArguments();
        final ArrayList<String> ids = args.getStringArrayList(ARG_IDS);
        final ArrayList<String> spotIds = ids == null ? new ArrayList<String>() : ids;
        final String message = getString(R.string.dialog_book_message,
                TextUtils.join(", ", spotIds), PriceFormatter.format(args.getLong(ARG_TOTAL)));
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_book_title, spotIds.size()))
                .setMessage(message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm,
                        (dialog, which) -> ((Callback) requireActivity()).onBookingConfirmed())
                .create();
    }
}
