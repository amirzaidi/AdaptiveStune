package amirz.dynamicstune;

import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import amirz.dynamicstune.database.BoostDB;
import amirz.dynamicstune.math.Line;
import amirz.dynamicstune.math.Parabola;
import amirz.dynamicstune.math.Polynomial;

public class Algorithm {
    private static final String TAG = "Algorithm";

    private static final double STEADY_INCREASE = 0.20;
    private static final double STEADY_DECREASE = 0.05;

    private static final int DATA_POINTS_PARABOLA = 5;
    private static final int DATA_POINTS_LINE = 3;

    private static final double JANK_FACTOR_TARGET = 0.1;

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

        public double getJankFactor() {
            return janky / total;
        }
    }

    public static int getBoost(List<Measurement> measurements) {
        SparseArray<Measurement> averages = avg(measurements);
        Log.d(TAG, "Boost measurements " + averages.size() + " (" + measurements.size() + " total)");

        List<Polynomial.Point> points = new ArrayList<>();
        for (int i = 0; i < averages.size(); i++) {
            double smoothnessFactor = averages.valueAt(i).getJankFactor();
            points.add(new Polynomial.Point(averages.keyAt(i), smoothnessFactor));
        }

        if (averages.size() >= DATA_POINTS_PARABOLA) {
            Polynomial parabola = new Polynomial(points, 2);
            double a = parabola.getCoefficient(2);
            double b = parabola.getCoefficient(1);
            double c = parabola.getCoefficient(0);
            double[] intersect = Parabola.intersect(a, b, c, JANK_FACTOR_TARGET);

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

        if (averages.size() >= DATA_POINTS_LINE) {
            Polynomial line = new Polynomial(points, 1);

            double a = line.getCoefficient(1);
            // Line data does not make sense if this is not positive.
            if (a > 0) {
                double b = line.getCoefficient(0);
                double intersect = Line.intersect(a, b, JANK_FACTOR_TARGET);

                Log.w(TAG, "Line fitting: " + a + " " + b);
                if (intersect != Double.NaN) {
                    Log.w(TAG, "Line fitting result: boost = " + intersect);
                    return (int) Math.round(intersect);
                }
            }
        }

        // Fallback implementation when not enough data has been collected.
        // Adjust based on the last data point.
        Measurement lastMeasurement = measurements.get(measurements.size() - 1);

        float offset = 0;
        double jankFactor = lastMeasurement.getJankFactor();

        if (lastMeasurement.perc90 > 16) {
            // 90% needs to be at least 60FPS
            offset = 1f;
        } else if (jankFactor >= STEADY_INCREASE || lastMeasurement.perc95 > 16) {
            // Try to get 95% to 60FPS too
            offset = 0.5f;
        } else if (jankFactor <= STEADY_DECREASE || lastMeasurement.perc99 <= 33) {
            // Having only 1% at 30FPS is acceptable
            offset = -0.5f;
        }

        // This will vary between approximately 1 and 4
        offset *= Math.log10(lastMeasurement.total);

        return lastMeasurement.boost + Math.round(offset);
    }

    private static boolean validBoost(double boost) {
            return boost >= BoostDB.IDLE_BOOST && boost <= BoostDB.MAX_BOOST;
        }

    private static SparseArray<Measurement> avg(List<Measurement> measurements) {
        int[] boostCount = new int[BoostDB.MAX_BOOST + 1];
        for (Measurement m : measurements) {
            boostCount[m.boost]++;
        }

        SparseArray<Measurement> averages = new SparseArray<>();

        // Initialize all measurement sum objects.
        for (int i = 0; i < boostCount.length; i++) {
            if (boostCount[i] != 0) {
                averages.put(i, new Measurement(i));
            }
        }

        // Count sums
        for (Measurement m : measurements) {
            Measurement sum = averages.get(m.boost);
            sum.total += m.total;
            sum.janky += m.janky;
            sum.perc90 += m.perc90;
            sum.perc95 += m.perc95;
            sum.perc99 += m.perc99;
        }

        // Normalize to averages
        for (int i = 0; i < boostCount.length; i++) {
            int count = boostCount[i];
            if (count != 0) {
                Measurement sum = averages.get(i);
                sum.total /= count;
                sum.janky /= count;
                sum.perc90 /= count;
                sum.perc95 /= count;
                sum.perc99 /= count;
            }
        }

        return averages;
    }
}
