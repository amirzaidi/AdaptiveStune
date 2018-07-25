package amirz.dynamicstune;

import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import amirz.dynamicstune.database.BoostDB;
import amirz.dynamicstune.math.Line;
import amirz.dynamicstune.math.Parabola;
import amirz.dynamicstune.math.Polynomial;

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
        public final int boost;
        public double total;
        public double janky;
        public double perc90;
        public double perc95;
        public double perc99;

        public Measurement(int boost) {
            this.boost = boost;
        }
    }

    public static int getBoost(List<Measurement> measurements) {
        SparseArray<Measurement> averages = avg(measurements);
        Log.d(TAG, "Boost measurements " + averages.size() + " (" + measurements.size() + " total)");

        // Transform the data set into points for the Polynomial class.
        List<Polynomial.Point> points = new ArrayList<>();
        for (int i = 0; i < averages.size(); i++) {
            points.add(new Polynomial.Point(averages.keyAt(i), parseJankFactor(averages.valueAt(i))));
        }

        if (averages.size() >= MIN_DATA_POINTS_PARABOLA) {
            Polynomial parabola = new Polynomial(points, 2);
            double a = parabola.getCoefficient(2);
            double b = parabola.getCoefficient(1);
            double c = parabola.getCoefficient(0);
            double[] intersect = Parabola.intersect(a, b, c, 0);

            Log.w(TAG, "Parabola fitting: " + a + " " + b + " " + c);
            if (intersect.length != 0) {
                if (validBoost(intersect[0])) {
                    Log.w(TAG, "Parabola fitting result 1: boost = " + intersect[0]);
                    return (int) Math.round(intersect[0]);
                }

                if (intersect.length == 2 && validBoost(intersect[1])) {
                    Log.w(TAG, "Parabola fitting result 2: boost = " + intersect[1]);
                    return (int) Math.round(intersect[1]);
                }
            }
        }

        if (averages.size() >= MIN_DATA_POINTS_LINE) {
            Polynomial line = new Polynomial(points, 1);

            double a = line.getCoefficient(1);
            // Line data does not make sense if this is not positive.
            if (a > 0) {
                double b = line.getCoefficient(0);
                double intersect = Line.intersect(a, b, 0);

                Log.w(TAG, "Line fitting: " + a + " " + b);
                if (intersect != Double.NaN) {
                    Log.w(TAG, "Line fitting result: boost = " + intersect);
                    return (int) Math.round(intersect);
                }
            }
        }

        // Fallback implementation when not enough data has been collected.
        // Adjust based on the last data point.
        Measurement lastMeasure = measurements.get(measurements.size() - 1);
        double offset = parseJankFactor(lastMeasure) * parseDurationFactor(lastMeasure);
        return lastMeasure.boost + (int) Math.round(offset);
    }

    private static boolean validBoost(double boost) {
        return boost >= BoostDB.IDLE_BOOST && boost <= BoostDB.MAX_BOOST;
    }

    private static SparseArray<Measurement> avg(List<Measurement> measurements) {
        // Could be optimized by shifting down with BoostDB.IDLE_BOOST.
        int[] boostCount = new int[BoostDB.MAX_BOOST + 1];
        for (Measurement m : measurements) {
            boostCount[m.boost]++;
        }

        // Initialize all measurement sum objects.
        SparseArray<Measurement> measurementSums = new SparseArray<>();
        SparseArray<Double> totalDurationWeights = new SparseArray<>();
        for (int i = 0; i < boostCount.length; i++) {
            if (boostCount[i] != 0) {
                measurementSums.put(i, new Measurement(i));
                totalDurationWeights.put(i, 0d);
            }
        }

        // Count sums
        for (Measurement m : measurements) {
            double durationWeight = parseDurationFactor(m);

            Measurement sum = measurementSums.get(m.boost);
            sum.total += m.total * durationWeight;
            sum.janky += m.janky * durationWeight;
            sum.perc90 += m.perc90 * durationWeight;
            sum.perc95 += m.perc95 * durationWeight;
            sum.perc99 += m.perc99 * durationWeight;

            totalDurationWeights.put(m.boost, totalDurationWeights.get(m.boost) + durationWeight);
        }

        // Normalize to averages
        for (int i = 0; i < measurementSums.size(); i++) {
            double totalDurationWeight = totalDurationWeights.valueAt(i);

            Measurement sum = measurementSums.valueAt(i);
            sum.total /= totalDurationWeight;
            sum.janky /= totalDurationWeight;
            sum.perc90 /= totalDurationWeight;
            sum.perc95 /= totalDurationWeight;
            sum.perc99 /= totalDurationWeight;
        }

        return measurementSums;
    }

    public static double parseJankFactor(Measurement measurement) {
        return measurement.janky / measurement.total +
                (measurement.perc90 - TARGET_FRAME_TIME_MS) * PERC_90_TARGET_WEIGHT +
                (measurement.perc95 - TARGET_FRAME_TIME_MS) * PERC_95_TARGET_WEIGHT +
                (measurement.perc99 - TARGET_FRAME_TIME_MS) * PERC_99_TARGET_WEIGHT;
    }

    public static double parseDurationFactor(Measurement measurement) {
        return DURATION_COEFFICIENT * Math.pow(measurement.total, DURATION_POW);
    }
}
