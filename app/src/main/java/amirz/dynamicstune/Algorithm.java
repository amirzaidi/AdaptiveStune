package amirz.dynamicstune;

public class Algorithm {
    private static final int JANK_FACTOR_MIN_FRAMES = 90;

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
        if (info.total > JANK_FACTOR_MIN_FRAMES) {
            double jankFactor = info.getJankFactor();

            if (jankFactor >= Database.JANK_FACTOR_QUICK_BOOST) {
                offset = 5;
            } else if (jankFactor >= Database.JANK_FACTOR_STEADY_INCREASE) {
                offset = 1;
            } else if (jankFactor <= Database.JANK_FACTOR_STEADY_DECREASE) {
                offset = -1;
            }
        }

        return offset;
    }
}
