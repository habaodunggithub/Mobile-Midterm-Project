package com.example.midtermproject.ui;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.midtermproject.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Hộp thoại xác nhận làm mới dữ liệu (AC-8.1): nêu số chỗ đang chọn và số vé của bạn sẽ bị xóa.
 * Hai con số đi vào qua {@code arguments} để {@code FragmentManager} dựng lại đúng nội dung sau khi
 * xoay màn hình (AC-8.3). Bấm "Xác nhận" chỉ báo cho Activity qua {@link Callback}; "Hủy", Back hay
 * chạm ra ngoài chỉ đóng hộp thoại.
 */
public class ResetDataDialog extends DialogFragment {
    public static final String TAG = "reset_data";
    private static final String ARG_SELECTED = "selected_count";
    private static final String ARG_TICKETS = "ticket_count";

    /** Activity chứa hộp thoại phải cài đặt interface này. */
    public interface Callback {
        void onResetConfirmed();
    }

    public static ResetDataDialog newInstance(int selectedCount, int ticketCount) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SELECTED, selectedCount);
        args.putInt(ARG_TICKETS, ticketCount);
        final ResetDataDialog dialog = new ResetDataDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = requireArguments();
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_reset_title)
                .setMessage(getString(R.string.dialog_reset_message,
                        args.getInt(ARG_SELECTED), args.getInt(ARG_TICKETS)))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm,
                        (dialog, which) -> ((Callback) requireActivity()).onResetConfirmed())
                .create();
    }
}
