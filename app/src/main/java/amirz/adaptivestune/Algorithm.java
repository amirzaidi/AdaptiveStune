package amirz.adaptivestune;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import amirz.adaptivestune.database.Boost;
import amirz.adaptivestune.math.Line;
import amirz.adaptivestune.math.Parabola;
import amirz.adaptivestune.math.Polynomial;

import static amirz.adaptivestune.math.MathUtils.between;

/**
 * This class takes measurements into account and returns a new boost value to try.
 * From that selected boost value new measurements are done, and eventually all data is
 * inserted into a polynomial. The class then selects a boost from this polynomial.
 * The goal is to get the parseJankFactor method to return zero, meaning, there is no jank.
 * Note that this does not directly imply there are no frame drops, but that they are insignificant.
 */
public class Algorithm {
    private static final String TAG = "Algorithm";

    // Frame time target for all percentiles.
    private static final Tunable.IntegerRef TARGET_FRAME_TIME_MS =
            new Tunable.IntegerRef(R.string.pref_target_frame_time_ms,
                    R.integer.default_target_frame_time_ms);

    // Influence on the jank factor is stronger when these are higher.
    private static final Tunable.FloatRef PERC_90_TARGET_WEIGHT =
            new Tunable.FloatRef(R.string.pref_perc_90_target_weight,
                    R.integer.default_perc_90_target_weight);

    private static final Tunable.FloatRef PERC_95_TARGET_WEIGHT =
            new Tunable.FloatRef(R.string.pref_perc_95_target_weight,
                    R.integer.default_perc_95_target_weight);

    private static final Tunable.FloatRef PERC_99_TARGET_WEIGHT =
            new Tunable.FloatRef(R.string.pref_perc_99_target_weight,
                    R.integer.default_perc_99_target_weight);

    // When this many different data points exist, use the parabola fitting algorithm.
    private static final Tunable.IntegerRef MIN_DATA_POINTS_PARABOLA =
            new Tunable.IntegerRef(R.string.pref_min_data_points_parabola,
                    R.integer.default_min_data_points_parabola);

    // When this many different data points exist, use the line fitting algorithm.
    private static final Tunable.IntegerRef MIN_DATA_POINTS_LINE =
            new Tunable.IntegerRef(R.string.pref_min_data_points_line,
                    R.integer.default_min_data_points_line);

    // Used to determine how important the latest result is for fallback algorithm.
    private static final Tunable.FloatRef DURATION_COEFFICIENT =
            new Tunable.FloatRef(R.string.pref_duration_coefficient,
                    R.integer.default_duration_coefficient);

    private static final Tunable.FloatRef DURATION_POW =
            new Tunable.FloatRef(R.string.pref_duration_pow,
                    R.integer.default_duration_pow);

    public static void init() {}

    /**
     * Subclass that acts as a data wrapper for a measurement or an aggregate of measurements.
     */
    public static class Measurement {
        public final float boost;
        public double total;
        public double janky;
        public double perc90;
        public double perc95;
        public double perc99;

        public Measurement(float boost) {
            this.boost = boost;
        }
    }

    public static double getBoost(List<Measurement> measurements) {
        // Save the min and max for further value checking.
        int minMeasuredBoost = Boost.MAX_BOOST.get();
        int maxMeasuredBoost = Boost.IDLE_BOOST.get();

        Set<Integer> boosts = new HashSet<>();
        List<Polynomial.Point> points = new ArrayList<>();

        for (Measurement m : measurements) {
            int boost = Boost.round(m.boost);

            minMeasuredBoost = Math.min(minMeasuredBoost, boost);
            maxMeasuredBoost = Math.max(maxMeasuredBoost, boost);

            // Transform the data set into points for the Polynomial class.
            boosts.add(boost);
            points.add(new Polynomial.Point(boost, getJankTargetOffset(m)));
        }

        if (boosts.size() >= MIN_DATA_POINTS_PARABOLA.get()) {
            Polynomial parabola = new Polynomial(points, 2);
            double a = parabola.getCoefficient(2);
            double b = parabola.getCoefficient(1);
            double c = parabola.getCoefficient(0);
            double[] intersect = Parabola.root(a, b, c);

            Log.w(TAG, "Parabola fitting: " + a + " " + b + " " + c);
            if (intersect.length == 2) {
                // Never extrapolate, only interpolate.
                boolean firstBetween = between(intersect[0], minMeasuredBoost, maxMeasuredBoost);
                boolean secondBetween = between(intersect[1], minMeasuredBoost, maxMeasuredBoost);
                if (firstBetween && !secondBetween) {
                    Log.d(TAG, "Parabola fitting result 1: boost = " + intersect[0]);
                    return intersect[0];
                }
                if (!firstBetween && secondBetween) {
                    Log.d(TAG, "Parabola fitting result 2: boost = " + intersect[1]);
                    return intersect[1];
                }
            }
        }

        if (boosts.size() >= MIN_DATA_POINTS_LINE.get()) {
            Polynomial line = new Polynomial(points, 1);

            double a = line.getCoefficient(1);
            double b = line.getCoefficient(0);

            Log.d(TAG, "Line fitting: " + a + " " + b);

            // Line data does not make sense if this is not negative, because there should be a
            // downwards trend for jank calculations at higher boosts.
            if (a < 0) {
                double intersect = Line.root(a, b);
                if (between(intersect, minMeasuredBoost, maxMeasuredBoost)) {
                    Log.d(TAG, "Line fitting result: boost = " + intersect);
                    return intersect;
                }
            }
        }

        // Fallback implementation when not enough data has been collected.
        // Adjust based on the last data point.
        Measurement lastMeasure = measurements.get(measurements.size() - 1);
        double offset = getJankTargetOffset(lastMeasure) * getDurationFactor(lastMeasure);
        return lastMeasure.boost + offset;
    }

    /**
     * Converts a measurement into a single factor that judges the smoothness.
     * @param measurement The measurement from which to create a factor.
     * @return Zero when the frame rate is equal to the target.
     * A positive value when it is too janky, a negative value when it is too smooth.
     */
    public static double getJankTargetOffset(Measurement measurement) {
        int targetFrameTime = TARGET_FRAME_TIME_MS.get();
        return measurement.janky / measurement.total +
                (measurement.perc90 - targetFrameTime) * PERC_90_TARGET_WEIGHT.get() +
                (measurement.perc95 - targetFrameTime) * PERC_95_TARGET_WEIGHT.get() +
                (measurement.perc99 - targetFrameTime) * PERC_99_TARGET_WEIGHT.get();
    }

    public static double getDurationFactor(Measurement measurement) {
        return DURATION_COEFFICIENT.get() * Math.pow(measurement.total, DURATION_POW.get());
    }
}
