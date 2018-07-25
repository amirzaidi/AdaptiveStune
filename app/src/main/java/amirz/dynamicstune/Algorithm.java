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

    private static final int DATA_POINTS_PARABOLA = 3;
    private static final int DATA_POINTS_LINE = 2;

    private static final double SMOOTHNESS_TARGET = 0.1;

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
        Log.d(TAG, "Measurement count " + measurements.size());

        int[] boostCount = new int[BoostDB.MAX_BOOST + 1];
        for (Measurement m : measurements) {
            boostCount[m.boost]++;
        }

        SparseArray<Measurement> sums = new SparseArray<>();

        // Initialize all measurement sum objects.
        for (int i = 0; i < boostCount.length; i++) {
            if (boostCount[i] != 0) {
                sums.put(i, new Measurement(i));
            }
        }

        // Count sums
        for (Measurement m : measurements) {
            Measurement sum = sums.get(m.boost);
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
                Measurement sum = sums.get(i);
                sum.total /= count;
                sum.janky /= count;
                sum.perc90 /= count;
                sum.perc95 /= count;
                sum.perc99 /= count;
            }
        }

        List<Polynomial.Point> points = new ArrayList<>();
        for (int i = 0; i < sums.size(); i++) {
            double smoothnessFactor = sums.valueAt(i).getJankFactor();
            points.add(new Polynomial.Point(sums.keyAt(i), smoothnessFactor));
        }

        if (sums.size() > DATA_POINTS_PARABOLA) {
            Polynomial parabola = new Polynomial(points, 2);
            double[] intersect = Parabola.intersect(parabola.getCoefficient(0), parabola.getCoefficient(1), parabola.getCoefficient(2), SMOOTHNESS_TARGET);
            if (intersect.length == 1) {
                Log.w(TAG, "Parabola fitting: " + intersect[0]);
                return (int) Math.round(intersect[0]);
            } else {
                Log.w(TAG, "Parabola fitting: invalid results (" + intersect.length + ")");
            }
        }

        if (sums.size() > DATA_POINTS_LINE) {
            Polynomial line = new Polynomial(points, 1);
            double intersect = Line.intersect(line.getCoefficient(0), line.getCoefficient(1), SMOOTHNESS_TARGET);
            if (intersect != Double.NaN) {
                Log.w(TAG, "Line fitting: " + line);
                return (int) Math.round(intersect);
            } else {
                Log.w(TAG, "Line fitting: invalid results" );
            }
        }

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
}
