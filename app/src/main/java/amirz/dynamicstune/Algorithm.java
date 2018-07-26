package amirz.dynamicstune;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import amirz.dynamicstune.database.BoostDB;
import amirz.dynamicstune.math.Line;
import amirz.dynamicstune.math.Parabola;
import amirz.dynamicstune.math.Polynomial;

import static amirz.dynamicstune.math.MathUtils.between;

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
    private static final int TARGET_FRAME_TIME_MS = 16;

    // ToDo: Make these constants tunable with a settings activity.
    private static final double PERC_90_TARGET_WEIGHT = 0.150f;
    private static final double PERC_95_TARGET_WEIGHT = 0.010f;
    private static final double PERC_99_TARGET_WEIGHT = 0.005f;

    // ToDo: Make these constants tunable with a settings activity.
    private static final int MIN_DATA_POINTS_PARABOLA = 5;
    private static final int MIN_DATA_POINTS_LINE = 3;

    // ToDo: Make these constants tunable with a settings activity.
    private static final double DURATION_COEFFICIENT = 0.15;
    private static final double DURATION_POW = 0.4;

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
        Log.d(TAG, "Frame time measurement count: " + measurements.size());

        // Save the min and max for further value checking.
        int minMeasuredBoost = BoostDB.MAX_BOOST;
        int maxMeasuredBoost = BoostDB.IDLE_BOOST;

        Set<Integer> boosts = new HashSet<>();
        List<Polynomial.Point> points = new ArrayList<>();

        for (Measurement m : measurements) {
            int boost = BoostDB.getBoostInt(m.boost);

            minMeasuredBoost = Math.min(minMeasuredBoost, boost);
            maxMeasuredBoost = Math.max(maxMeasuredBoost, boost);

            // Transform the data set into points for the Polynomial class.
            boosts.add(boost);
            points.add(new Polynomial.Point(boost, getJankTargetOffset(m)));
        }

        if (boosts.size() >= MIN_DATA_POINTS_PARABOLA) {
            Polynomial parabola = new Polynomial(points, 2);
            double a = parabola.getCoefficient(2);
            double b = parabola.getCoefficient(1);
            double c = parabola.getCoefficient(0);
            double[] intersect = Parabola.intersect(a, b, c, 0);

            Log.w(TAG, "Parabola fitting: " + a + " " + b + " " + c);

            // Never extrapolate, only interpolate.
            if (intersect.length > 0 && between(intersect[0], minMeasuredBoost, maxMeasuredBoost)) {
                Log.d(TAG, "Parabola fitting result 1: boost = " + intersect[0]);
                return intersect[0];
            }

            if (intersect.length == 2 && between(intersect[1], minMeasuredBoost, maxMeasuredBoost)) {
                Log.d(TAG, "Parabola fitting result 2: boost = " + intersect[1]);
                return intersect[1];
            }
        }

        if (boosts.size() >= MIN_DATA_POINTS_LINE) {
            Polynomial line = new Polynomial(points, 1);

            double a = line.getCoefficient(1);
            double b = line.getCoefficient(0);

            Log.d(TAG, "Line fitting: " + a + " " + b);

            // Line data does not make sense if this is not positive.
            if (a > 0) {
                double intersect = Line.intersect(a, b, 0);
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
        return measurement.janky / measurement.total +
                (measurement.perc90 - TARGET_FRAME_TIME_MS) * PERC_90_TARGET_WEIGHT +
                (measurement.perc95 - TARGET_FRAME_TIME_MS) * PERC_95_TARGET_WEIGHT +
                (measurement.perc99 - TARGET_FRAME_TIME_MS) * PERC_99_TARGET_WEIGHT;
    }

    public static double getDurationFactor(Measurement measurement) {
        return DURATION_COEFFICIENT * Math.pow(measurement.total, DURATION_POW);
    }
}
