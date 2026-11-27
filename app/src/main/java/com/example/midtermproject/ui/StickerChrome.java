package com.example.midtermproject.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.midtermproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.EdgeTreatment;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.ShapePath;
import com.google.android.material.textfield.TextInputLayout;

/** Nét cắt lượn sóng cho những thành phần dùng nền và ripple của Material. */
final class StickerChrome {
    private static final int[] CARDS = {
            R.id.filter_panel, R.id.card_price_filter, R.id.layout_filter_status,
            R.id.layout_no_result, R.id.card_legend
    };
    private static final int[] MAP_BUTTONS = {
            R.id.btn_legend, R.id.btn_reset_view, R.id.btn_compass, R.id.btn_more
    };
    private static final int[] FILTER_CHIPS = {
            R.id.chip_zone_all, R.id.chip_zone_standing, R.id.chip_zone_vip, R.id.chip_zone_standard
    };

    private StickerChrome() {}

    static void apply(View root) {
        final Context context = root.getContext();
        final ShapeAppearanceModel cardShape = shape(context, 18f);
        final ShapeAppearanceModel controlShape = shape(context, 12f);
        for (int id : CARDS) {
            final View card = root.findViewById(id);
            // Bộ lọc giá ở ngang nằm trực tiếp trong panel, nên không có nền riêng.
            if (card != null && card.getBackground() != null) {
                card.setBackground(surface(context, cardShape));
            }
        }

        final int ripple = MaterialColors.getColor(root, android.R.attr.colorControlHighlight, 0x22000000);
        for (int id : MAP_BUTTONS) {
            final MaterialShapeDrawable mask = new MaterialShapeDrawable(controlShape);
            mask.setFillColor(ColorStateList.valueOf(Color.WHITE));
            root.findViewById(id).setBackground(new RippleDrawable(ColorStateList.valueOf(ripple),
                    surface(context, controlShape), mask));
        }

        final TextInputLayout search = root.findViewById(R.id.til_search);
        final ShapeAppearanceModel searchShape = shape(context, 16f);
        search.setShapeAppearanceModel(searchShape);
        final View input = search.getEditText();
        if (input != null) {
            final int borderWidth = search.getBoxStrokeWidth();
            final ColorStateList border = ContextCompat.getColorStateList(context, R.color.field_stroke);
            input.setForeground(new RaisedStickerStroke(searchShape, borderWidth, border, input::getBackground));
            search.setBoxStrokeWidth(0);
            search.setBoxStrokeWidthFocused(0);
        }
        for (int id : FILTER_CHIPS) {
            final Chip chip = root.findViewById(id);
            chip.setShapeAppearanceModel(controlShape);
            chip.setForeground(new RaisedStickerStroke(controlShape, chip.getChipStrokeWidth(),
                    chip.getChipStrokeColor(), chip::getChipDrawable));
            chip.setChipStrokeWidth(0f);
        }
        final MaterialButton price = root.findViewById(R.id.btn_filter_advanced);
        if (price != null) {
            raiseButton(price, controlShape);
        }
        final MaterialButton book = root.findViewById(R.id.btn_book);
        raiseButton(book, controlShape);
    }

    static MaterialAlertDialogBuilder dialog(Context context) {
        return new MaterialAlertDialogBuilder(context).setBackground(surface(context, shape(context, 20f)));
    }

    private static Drawable surface(Context context, ShapeAppearanceModel shape) {
        final MaterialShapeDrawable drawable = new MaterialShapeDrawable(shape);
        drawable.setFillColor(ColorStateList.valueOf(context.getColor(R.color.surface)));
        final float width = 3f * context.getResources().getDisplayMetrics().density;
        final ColorStateList border = ColorStateList.valueOf(context.getColor(R.color.sticker_outline));
        return new LayerDrawable(new Drawable[] {
                drawable, new RaisedStickerStroke(shape, width, border, () -> drawable)
        });
    }

    private static void raiseButton(MaterialButton button, ShapeAppearanceModel shape) {
        button.setShapeAppearanceModel(shape);
        final Drawable surface = materialSurface(button.getBackground());
        if (surface != null) {
            button.setForeground(new RaisedStickerStroke(shape, button.getStrokeWidth(),
                    button.getStrokeColor(), () -> surface));
            button.setStrokeWidth(0);
        }
    }

    /** Nền nút Material nằm dưới ripple và các inset của vùng chạm. */
    private static Drawable materialSurface(Drawable drawable) {
        if (drawable instanceof MaterialShapeDrawable) {
            return drawable;
        }
        if (drawable instanceof InsetDrawable) {
            return materialSurface(((InsetDrawable) drawable).getDrawable());
        }
        if (drawable instanceof LayerDrawable) {
            final LayerDrawable layers = (LayerDrawable) drawable;
            for (int i = 0; i < layers.getNumberOfLayers(); i++) {
                final Drawable match = materialSurface(layers.getDrawable(i));
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static ShapeAppearanceModel shape(Context context, float cornerDp) {
        final float density = context.getResources().getDisplayMetrics().density;
        return ShapeAppearanceModel.builder()
                .setAllCornerSizes(cornerDp * density)
                .setAllEdges(new StickerEdge(2.2f * density))
                .build();
    }

    private static final class StickerEdge extends EdgeTreatment {
        private final float depth;

        StickerEdge(float depth) {
            this.depth = depth;
        }

        @Override
        public void getEdgePath(float length, float center, float interpolation, ShapePath path) {
            final float step = length / 3f;
            final float wave = Math.min(depth, step / 4f) * interpolation;
            // Ba nhịp không đều, nối vào góc với tiếp tuyến phẳng.
            path.cubicToPoint(step * 0.2f, 0f, step * 0.65f, wave * 2f, step, wave);
            path.cubicToPoint(step * 1.3f, 0f, step * 1.6f, 0f, step * 2f, wave * 0.8f);
            path.cubicToPoint(step * 2.45f, wave * 1.6f, step * 2.75f, 0f, length, 0f);
        }
    }
}
