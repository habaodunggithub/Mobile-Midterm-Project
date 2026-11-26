package com.example.midtermproject.decor;

/**
 * Hình học các dải màu lỏng trên nền poster (REQUIREMENTS Q42). Java thuần để kiểm bằng JUnit.
 * <p>
 * Mọi số đo được định nghĩa trên một "khổ thiết kế" rộng {@link #DESIGN_WIDTH} đơn vị, rồi nhân với
 * {@code unit = chiều rộng View / DESIGN_WIDTH}, nên dải trông giống nhau trên mọi màn hình và khi xoay ngang.
 * <p>
 * Dải thứ i là một vệt dọc. Ở độ cao y (đơn vị thiết kế) và thời điểm t (radian):
 * <pre>
 *   tâm      c(y) = x0 + A·sin(y/λ1 + φ + t)
 *   bề rộng  w(y) = w0·(0,45 + 0,55·|sin(y/λ2 + 1,7φ + 0,6t)|)
 *   mép trái = c − w/2,  mép phải = c + w/2
 * </pre>
 * Tâm uốn qua lại và bề rộng phình thắt, nên dải trông như chất lỏng đang chảy. Hàm tuần hoàn theo t
 * với chu kỳ 10π (bội chung của 2π và 2π/0,6), nên chuyển động lặp lại mượt không giật.
 */
public final class LiquidColumns {
    /** Chiều rộng khổ thiết kế, khớp với bản xem trước (máy dọc rộng 360dp). */
    public static final float DESIGN_WIDTH = 360f;
    /** Biên độ uốn của tâm dải. */
    static final float SWAY = 16f;
    /** Bước sóng của đường uốn và của độ phình (đơn vị thiết kế). */
    static final float WAVE_LENGTH = 70f;
    static final float SWELL_LENGTH = 95f;
    /** Bề rộng nhỏ nhất bằng 45% bề rộng gốc. */
    static final float MIN_WIDTH_RATIO = 0.45f;
    /** Thời gian (radian) để mọi dải trở về đúng hình ban đầu. */
    public static final double PERIOD = 10 * Math.PI;

    /** Ba màu dải: cam, xanh lá, tím (chỉ số vào bảng màu của View). */
    public static final int COLOR_ORANGE = 0;
    public static final int COLOR_GREEN = 1;
    public static final int COLOR_VIOLET = 2;

    private final float[] baseX;
    private final float[] baseWidth;
    private final float[] phase;
    private final int[] colorSlot;

    LiquidColumns(float[] baseX, float[] baseWidth, float[] phase, int[] colorSlot) {
        this.baseX = baseX;
        this.baseWidth = baseWidth;
        this.phase = phase;
        this.colorSlot = colorSlot;
    }

    /** Tám dải của hướng G, giống bản xem trước {@code directions-v3.html}. */
    public static LiquidColumns poster() {
        return new LiquidColumns(
                new float[]{18f, 70f, 112f, 160f, 222f, 262f, 314f, 350f},
                new float[]{34f, 22f, 14f, 40f, 18f, 26f, 30f, 16f},
                new float[]{0.2f, 1.4f, 2.6f, 3.3f, 0.8f, 2.1f, 1.1f, 2.9f},
                new int[]{COLOR_ORANGE, COLOR_GREEN, COLOR_VIOLET, COLOR_ORANGE,
                        COLOR_GREEN, COLOR_VIOLET, COLOR_ORANGE, COLOR_GREEN});
    }

    public int count() {
        return baseX.length;
    }

    public int colorSlot(int column) {
        return colorSlot[column];
    }

    /**
     * Số điểm lấy mẫu trên mỗi mép để phủ hết chiều cao {@code heightDesign}, kể cả một bước thừa ở
     * trên và dưới (để mép dải không lộ ra ở cạnh màn hình).
     */
    public static int samplesFor(float heightDesign, float step) {
        return (int) Math.ceil(heightDesign / step) + 3;
    }

    /**
     * Tính hai mép của một dải ở thời điểm {@code time}, theo đơn vị thiết kế. Điểm thứ k nằm ở độ cao
     * {@code y = (k − 1)·step}. Không cấp phát gì: ghi vào hai mảng có sẵn.
     *
     * @param left  mép trái, cần ít nhất {@code samples} phần tử
     * @param right mép phải, cần ít nhất {@code samples} phần tử
     */
    public void edges(int column, int samples, float step, double time, float[] left, float[] right) {
        final float x0 = baseX[column];
        final float w0 = baseWidth[column];
        final float phi = phase[column];
        for (int k = 0; k < samples; k++) {
            final float y = (k - 1) * step;
            final double center = x0 + SWAY * Math.sin(y / WAVE_LENGTH + phi + time);
            final double swell = Math.abs(Math.sin(y / SWELL_LENGTH + 1.7 * phi + 0.6 * time));
            final double halfWidth = w0 * (MIN_WIDTH_RATIO + (1 - MIN_WIDTH_RATIO) * swell) / 2;
            left[k] = (float) (center - halfWidth);
            right[k] = (float) (center + halfWidth);
        }
    }
}
