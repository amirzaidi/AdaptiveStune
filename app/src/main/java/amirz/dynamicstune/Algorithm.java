package amirz.dynamicstune;

public class Algorithm {
    private static final int MIN_FRAMES = 90;

    private static final double QUICK_BOOST = 0.35;
    private static final double STEADY_INCREASE = 0.20;
    private static final double STEADY_DECREASE = 0.05;

    public static class GfxInfo {
        public int total;
        public int janky;
        public int perc90;
        public int perc95;
        public int perc99;

        public double getJankFactor() {
            return (double) janky / total;
        }
    }

    public static float getBoostOffset(GfxInfo info) {
        float offset = 0;

        // Discard results if not enough information is collected.
        if (info.total > MIN_FRAMES) {
            double jankFactor = info.getJankFactor();

            if (jankFactor >= QUICK_BOOST) {
                offset = 5;
            } else if (jankFactor >= STEADY_INCREASE) {
                offset = 1;
            } else if (jankFactor <= STEADY_DECREASE) {
                offset = -1;
            }

            // This will vary between approximately 1 and 4
            offset *= Math.log10(info.total);
        }

        return offset;
    }
}
